package com.sms.controller;

import com.sms.model.*;
import com.sms.repository.*;
import com.sms.service.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class AcademicController {
    private final AttendanceRepository attendance;
    private final GradeRepository grades;
    private final AssignmentRepository assignments;
    private final NoticeRepository notices;
    private final CourseRepository courses;
    private final UserRepository users;
    private final EnrollmentRepository enrollments;
    private final CurrentUserService current;

    public AcademicController(AttendanceRepository attendance, GradeRepository grades,
            AssignmentRepository assignments, NoticeRepository notices, CourseRepository courses,
            UserRepository users, EnrollmentRepository enrollments, CurrentUserService current) {
        this.attendance=attendance; this.grades=grades; this.assignments=assignments;
        this.notices=notices; this.courses=courses; this.users=users;
        this.enrollments=enrollments; this.current=current;
    }

    public record AttendanceRequest(@NotNull Long studentId, @NotNull LocalDate date, boolean present, String note) {}
    public record GradeRequest(@NotNull Long studentId, @NotBlank String assessmentName,
            @NotNull @DecimalMin("0.0") BigDecimal score, @NotNull @DecimalMin("0.01") BigDecimal maxScore, String remarks) {}
    public record AssignmentRequest(@NotBlank String title, String description, LocalDate dueDate) {}
    public record NoticeRequest(@NotBlank String title, @NotBlank String message) {}
    public record AttendanceView(Long id, Long courseId, String courseTitle, Long studentId,
            String studentName, LocalDate date, boolean present, String note) {}
    public record GradeView(Long id, Long courseId, String courseTitle, Long studentId,
            String studentName, String assessmentName, BigDecimal score, BigDecimal maxScore, String remarks) {}
    public record AssignmentView(Long id, Long courseId, String courseTitle, String title,
            String description, LocalDate dueDate, String createdBy) {}
    public record NoticeView(Long id, Long courseId, String courseTitle, String title,
            String message, LocalDateTime createdAt, String createdBy) {}

    private Course requireTeacherCourse(Long courseId) {
        Course c = courses.findById(courseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Course not found"));
        AppUser me = current.requireCurrentUser();
        if (me.getRole()!=Role.ADMIN && (c.getTeacher()==null || !c.getTeacher().getId().equals(me.getId())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You are not assigned to this course");
        return c;
    }
    private AppUser requireStudent(Long id, Course course) {
        AppUser student=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Student not found"));
        if(student.getRole()!=Role.STUDENT) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selected account is not a student");
        var enrollment=enrollments.findByStudentIdAndCourseId(id,course.getId())
                .filter(e->e.getStatus()==EnrollmentStatus.ACTIVE)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Student is not actively enrolled in this course"));
        return student;
    }
    private void requireActiveEnrollment(AppUser student, Long courseId) {
        enrollments.findByStudentIdAndCourseId(student.getId(),courseId)
                .filter(e->e.getStatus()==EnrollmentStatus.ACTIVE)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"You are not enrolled in this course"));
    }
    private AttendanceView av(AttendanceRecord a) { return new AttendanceView(a.getId(),a.getCourse().getId(),a.getCourse().getTitle(),a.getStudent().getId(),a.getStudent().getFullName(),a.getAttendanceDate(),a.isPresent(),a.getNote()); }
    private GradeView gv(GradeRecord g) { return new GradeView(g.getId(),g.getCourse().getId(),g.getCourse().getTitle(),g.getStudent().getId(),g.getStudent().getFullName(),g.getAssessmentName(),g.getScore(),g.getMaxScore(),g.getRemarks()); }
    private AssignmentView assignView(CourseAssignment a) { return new AssignmentView(a.getId(),a.getCourse().getId(),a.getCourse().getTitle(),a.getTitle(),a.getDescription(),a.getDueDate(),a.getCreatedBy().getFullName()); }
    private NoticeView noticeView(CourseNotice n) { return new NoticeView(n.getId(),n.getCourse().getId(),n.getCourse().getTitle(),n.getTitle(),n.getMessage(),n.getCreatedAt(),n.getCreatedBy().getFullName()); }

    @GetMapping("/api/teacher/courses/{courseId}/students")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public List<Map<String,Object>> students(@PathVariable Long courseId) {
        requireTeacherCourse(courseId);
        return enrollments.findByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE)
          .stream()
          .map(e->Map.<String,Object>of("id",e.getStudent().getId(),"fullName",e.getStudent().getFullName(),"email",e.getStudent().getEmail(),"enrolledAt",e.getEnrolledAt())).toList();
    }

    @PostMapping("/api/teacher/courses/{courseId}/attendance")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public AttendanceView saveAttendance(@PathVariable Long courseId,@Valid @RequestBody AttendanceRequest req) {
        Course c=requireTeacherCourse(courseId); AppUser student=requireStudent(req.studentId(),c);
        AttendanceRecord a=attendance.findByCourseIdOrderByAttendanceDateDesc(courseId).stream()
          .filter(x->x.getStudent().getId().equals(student.getId()) && x.getAttendanceDate().equals(req.date())).findFirst().orElseGet(AttendanceRecord::new);
        a.setCourse(c); a.setStudent(student); a.setAttendanceDate(req.date()); a.setPresent(req.present()); a.setNote(req.note());
        return av(attendance.save(a));
    }
    @GetMapping("/api/teacher/courses/{courseId}/attendance")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public List<AttendanceView> courseAttendance(@PathVariable Long courseId) {
        requireTeacherCourse(courseId); return attendance.findByCourseIdOrderByAttendanceDateDesc(courseId).stream().map(this::av).toList();
    }
    @PostMapping("/api/teacher/courses/{courseId}/grades")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public GradeView saveGrade(@PathVariable Long courseId,@Valid @RequestBody GradeRequest req) {
        Course c=requireTeacherCourse(courseId); AppUser student=requireStudent(req.studentId(),c);
        if(req.score().compareTo(req.maxScore())>0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Score cannot exceed maximum score");
        GradeRecord g=grades.findByCourseId(courseId).stream().filter(x->x.getStudent().getId().equals(student.getId())&&x.getAssessmentName().equalsIgnoreCase(req.assessmentName().trim())).findFirst().orElseGet(GradeRecord::new);
        g.setCourse(c);g.setStudent(student);g.setAssessmentName(req.assessmentName().trim());g.setScore(req.score());g.setMaxScore(req.maxScore());g.setRemarks(req.remarks());
        return gv(grades.save(g));
    }
    @GetMapping("/api/teacher/courses/{courseId}/grades")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public List<GradeView> courseGrades(@PathVariable Long courseId) {
        requireTeacherCourse(courseId); return grades.findByCourseId(courseId).stream().map(this::gv).toList();
    }
    @PostMapping("/api/teacher/courses/{courseId}/assignments")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public AssignmentView createAssignment(@PathVariable Long courseId,@Valid @RequestBody AssignmentRequest req) {
        Course c=requireTeacherCourse(courseId); CourseAssignment a=new CourseAssignment();a.setCourse(c);a.setCreatedBy(current.requireCurrentUser());a.setTitle(req.title().trim());a.setDescription(req.description());a.setDueDate(req.dueDate());return assignView(assignments.save(a));
    }
    @PostMapping("/api/teacher/courses/{courseId}/notices")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public NoticeView createNotice(@PathVariable Long courseId,@Valid @RequestBody NoticeRequest req) {
        Course c=requireTeacherCourse(courseId);CourseNotice n=new CourseNotice();n.setCourse(c);n.setCreatedBy(current.requireCurrentUser());n.setTitle(req.title().trim());n.setMessage(req.message().trim());n.setCreatedAt(LocalDateTime.now());return noticeView(notices.save(n));
    }

    @GetMapping("/api/student/attendance")
    @PreAuthorize("hasRole('STUDENT')")
    public List<AttendanceView> myAttendance() { AppUser s=current.requireCurrentUser(); return attendance.findByStudentIdOrderByAttendanceDateDesc(s.getId()).stream().filter(a->isActive(s,a.getCourse().getId())).map(this::av).toList(); }
    @GetMapping("/api/student/results")
    @PreAuthorize("hasRole('STUDENT')")
    public List<GradeView> myResults() { AppUser s=current.requireCurrentUser(); return grades.findByStudentId(s.getId()).stream().filter(g->isActive(s,g.getCourse().getId())).map(this::gv).toList(); }
    @GetMapping("/api/student/assignments")
    @PreAuthorize("hasRole('STUDENT')")
    public List<AssignmentView> myAssignments() { AppUser s=current.requireCurrentUser(); return assignments.findAllByOrderByDueDateAsc().stream().filter(a->isActive(s,a.getCourse().getId())).map(this::assignView).toList(); }
    @GetMapping("/api/student/notices")
    @PreAuthorize("hasRole('STUDENT')")
    public List<NoticeView> myNotices() { AppUser s=current.requireCurrentUser(); return notices.findAllByOrderByCreatedAtDesc().stream().filter(n->isActive(s,n.getCourse().getId())).map(this::noticeView).toList(); }
    private boolean isActive(AppUser s,Long courseId) { return enrollments.findByStudentIdAndCourseId(s.getId(),courseId).map(e->e.getStatus()==EnrollmentStatus.ACTIVE).orElse(false); }
}
