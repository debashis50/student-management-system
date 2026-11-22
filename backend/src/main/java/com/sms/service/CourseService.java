package com.sms.service;

import com.sms.dto.CourseRequest;
import com.sms.dto.CourseResponse;
import com.sms.model.*;
import com.sms.repository.CourseRepository;
import com.sms.repository.EnrollmentRepository;
import com.sms.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.sms.repository.GradeRepository;
import com.sms.repository.AttendanceRepository;
import com.sms.repository.AssignmentRepository;
import com.sms.repository.NoticeRepository;

@Service
public class CourseService {
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;

    private final GradeRepository gradeRecords;
    private final AttendanceRepository attendanceRecords;
    private final AssignmentRepository assignments;
    private final NoticeRepository notices;

    public CourseService(
            CourseRepository courses,
            EnrollmentRepository enrollments,
            UserRepository users,
            GradeRepository gradeRecords,
            AttendanceRepository attendanceRecords,
            AssignmentRepository assignments,
            NoticeRepository notices) {

        this.courses = courses;
        this.enrollments = enrollments;
        this.users = users;
        this.gradeRecords = gradeRecords;
        this.attendanceRecords = attendanceRecords;
        this.assignments = assignments;
        this.notices = notices;
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> list(boolean includeAll) {
        List<Course> list = includeAll ? courses.findAll() : courses.findByStatusOrderByTitleAsc(CourseStatus.PUBLISHED);
        return list.stream().map(c -> CourseResponse.from(c,
                enrollments.countByCourseIdAndStatus(c.getId(), EnrollmentStatus.ACTIVE))).toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse get(Long id) {
        Course c = courses.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found"));
        return CourseResponse.from(c, enrollments.countByCourseIdAndStatus(id, EnrollmentStatus.ACTIVE));
    }

    @Transactional
    public CourseResponse create(CourseRequest req) {
        if (courses.findAll().stream().anyMatch(c -> c.getCourseCode().equalsIgnoreCase(req.courseCode()))) {
            throw new IllegalArgumentException("Course code already exists");
        }
        Course c = new Course();
        apply(c, req);
        c.setStatus(CourseStatus.DRAFT);
        c.setEnrollmentOpen(false);
        return saveResponse(c);
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest req) {
        Course c = courses.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found"));
        if (courses.findAll().stream().anyMatch(other -> !other.getId().equals(id)
                && other.getCourseCode().equalsIgnoreCase(req.courseCode()))) {
            throw new IllegalArgumentException("Course code already exists");
        }
        apply(c, req);
        return saveResponse(c);
    }

    private void apply(Course c, CourseRequest req) {
        c.setCourseCode(req.courseCode().trim().toUpperCase());
        c.setTitle(req.title().trim());
        c.setCategory(req.category());
        c.setDescription(req.description());
        c.setSyllabus(req.syllabus());
        c.setFee(req.fee());
        c.setDurationWeeks(req.durationWeeks());
        c.setCreditHours(req.creditHours());
        c.setStartDate(req.startDate());
        c.setEndDate(req.endDate());
        c.setEnrollmentDeadline(req.enrollmentDeadline());
        c.setCapacity(req.capacity());
        if (req.teacherId() == null) {
            c.setTeacher(null);
        } else {
            AppUser teacher = users.findById(req.teacherId()).orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
            if (teacher.getRole() != Role.TEACHER) throw new IllegalArgumentException("Selected user is not a teacher");
            c.setTeacher(teacher);
        }
    }

    @Transactional
    public CourseResponse publish(Long id) {
        Course c = courses.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found"));
        if (c.getStatus() == CourseStatus.ARCHIVED) throw new IllegalArgumentException("Archived course cannot be published");
        c.setStatus(CourseStatus.PUBLISHED);
        return saveResponse(c);
    }

    @Transactional
    public CourseResponse archive(Long id) {
        Course c = courses.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found"));
        c.setStatus(CourseStatus.ARCHIVED);
        c.setEnrollmentOpen(false);
        return saveResponse(c);
    }


    @Transactional
    public CourseResponse relaunch(Long id) {
        Course c = courses.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Course not found"));

        if (c.getStatus() != CourseStatus.ARCHIVED) {
            throw new IllegalArgumentException(
                    "Only archived courses can be relaunched");
        }

        c.setStatus(CourseStatus.DRAFT);
        c.setEnrollmentOpen(false);

        return saveResponse(c);
    }


    @Transactional
    public void deletePermanent(Long id) {
        Course c = courses.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Course not found"));

        if (c.getStatus() != CourseStatus.ARCHIVED) {
            throw new IllegalArgumentException(
                    "Archive the course before permanently deleting it.");
        }

        long enrollmentCount = enrollments.countByCourseId(id);
        long gradeCount = gradeRecords.countByCourseId(id);
        long attendanceCount = attendanceRecords.countByCourseId(id);
        long assignmentCount = assignments.countByCourseId(id);
        long noticeCount = notices.countByCourseId(id);
        
        System.out.println("Deleting course ID: " + id);
        System.out.println("Course title: " + c.getTitle());
        System.out.println("Enrollment count: " + enrollmentCount);
        System.out.println("Assignment count: " + assignmentCount);

        if (enrollmentCount > 0 || gradeCount > 0 ||
                attendanceCount > 0 || assignmentCount > 0 ||
                noticeCount > 0) {

            throw new IllegalArgumentException(
                    "Cannot delete course. Counts: " +
                            "Enrollments=" + enrollmentCount +
                            ", Grades=" + gradeCount +
                            ", Attendance=" + attendanceCount +
                            ", Assignments=" + assignmentCount +
                            ", Notices=" + noticeCount
            );
        }

        courses.delete(c);
    }

    @Transactional
    public CourseResponse setEnrollment(Long id, boolean open) {
        Course c = courses.findById(id).orElseThrow(() -> new IllegalArgumentException("Course not found"));
        if (open && c.getStatus() != CourseStatus.PUBLISHED) throw new IllegalArgumentException("Publish the course before opening enrollment");
        c.setEnrollmentOpen(open);
        return saveResponse(c);
    }

    private CourseResponse saveResponse(Course c) {
        Course saved = courses.save(c);
        return CourseResponse.from(saved, enrollments.countByCourseIdAndStatus(saved.getId(), EnrollmentStatus.ACTIVE));
    }
}