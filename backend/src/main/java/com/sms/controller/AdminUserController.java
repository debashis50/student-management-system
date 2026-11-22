package com.sms.controller;

import com.sms.model.AppUser;
import com.sms.model.Role;
import com.sms.repository.EnrollmentRepository;
import com.sms.repository.GradeRepository;
import com.sms.repository.AttendanceRepository;
import com.sms.repository.CourseRepository;
import com.sms.repository.AssignmentRepository;
import com.sms.repository.NoticeRepository;
import com.sms.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final EnrollmentRepository enrollments;
    private final GradeRepository grades;
    private final AttendanceRepository attendance;
    private final CourseRepository courses;
    private final AssignmentRepository assignments;
    private final NoticeRepository notices;

    public AdminUserController(
            UserRepository users,
            PasswordEncoder encoder,
            EnrollmentRepository enrollments,
            GradeRepository grades,
            AttendanceRepository attendance,
            CourseRepository courses,
            AssignmentRepository assignments,
            NoticeRepository notices) {

        this.users = users;
        this.encoder = encoder;
        this.enrollments = enrollments;
        this.grades = grades;
        this.attendance = attendance;
        this.courses = courses;
        this.assignments = assignments;
        this.notices = notices;
    }
    public record CreateUserRequest(@NotBlank @Size(max=120) String fullName,@NotBlank @Email String email,@NotBlank @Size(min=8) String password, Role role) {}
    public record UserItem(Long id,String username,String email,String fullName,String role,boolean active) {}
    @GetMapping public List<UserItem> list(@RequestParam(required=false) Role role) {
        return (role==null?users.findAll():users.findByRoleOrderByFullNameAsc(role)).stream()
          .map(u->new UserItem(u.getId(),u.getUsername(),u.getEmail(),u.getFullName(),u.getRole().name(),u.isActive())).toList();
    }
    @PostMapping public UserItem create(@Valid @RequestBody CreateUserRequest req) {
        if(req.role()!=Role.TEACHER && req.role()!=Role.STUDENT) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Admin can create Teacher or Student accounts only");
        String email=req.email().trim().toLowerCase(java.util.Locale.ROOT);
        if(users.existsByEmail(email)||users.existsByUsername(email)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already exists");
        AppUser u=new AppUser();u.setUsername(email);u.setEmail(email);u.setFullName(req.fullName().trim());u.setPasswordHash(encoder.encode(req.password()));u.setRole(req.role());u.setActive(true);u=users.save(u);
        return new UserItem(u.getId(),u.getUsername(),u.getEmail(),u.getFullName(),u.getRole().name(),u.isActive());
    }
    @PatchMapping("/{id}/active") public UserItem active(@PathVariable Long id,@RequestParam boolean value) {
        AppUser u=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found"));
        if(u.getRole()==Role.ADMIN) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Admin accounts cannot be disabled here");
        u.setActive(value);u=users.save(u);return new UserItem(u.getId(),u.getUsername(),u.getEmail(),u.getFullName(),u.getRole().name(),u.isActive());
    }
    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePermanent(@PathVariable Long id) {

        AppUser u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (u.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Admin accounts cannot be permanently deleted here");
        }

        if (u.getRole() == Role.STUDENT) {
            long enrollmentCount = enrollments.countByStudentId(id);
            long gradeCount = grades.countByStudentId(id);
            long attendanceCount = attendance.countByStudentId(id);

            if (enrollmentCount > 0 || gradeCount > 0 || attendanceCount > 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot delete this student because academic records exist. " +
                                "Enrollments: " + enrollmentCount +
                                ", grades: " + gradeCount +
                                ", attendance: " + attendanceCount +
                                ". Deactivate the account instead.");
            }

        } else if (u.getRole() == Role.TEACHER) {
            long courseCount = courses.countByTeacherId(id);
            long assignmentCount = assignments.countByCreatedById(id);
            long noticeCount = notices.countByCreatedById(id);

            if (courseCount > 0 || assignmentCount > 0 || noticeCount > 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot delete this teacher because teaching records exist. " +
                                "Assigned courses: " + courseCount +
                                ", assignments: " + assignmentCount +
                                ", notices: " + noticeCount +
                                ". Reassign or preserve these records first.");
            }

        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported user role");
        }

        users.delete(u);
    }
}
