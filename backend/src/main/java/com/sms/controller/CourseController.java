package com.sms.controller;

import com.sms.dto.CourseRequest;
import com.sms.dto.CourseResponse;
import com.sms.dto.EnrollmentOpenRequest;
import com.sms.model.Role;
import com.sms.repository.UserRepository;
import com.sms.service.CourseService;
import com.sms.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService service;
    private final CurrentUserService currentUser;

    public CourseController(CourseService service, CurrentUserService currentUser) {
        this.service = service; this.currentUser = currentUser;
    }

    @GetMapping
    public List<CourseResponse> list() {
        boolean all = currentUserOptionalRoleIsAdminOrTeacher();
        return service.list(all);
    }

    private boolean currentUserOptionalRoleIsAdminOrTeacher() {
        try {
            Role role = currentUser.requireCurrentUser().getRole();
            return role == Role.ADMIN || role == Role.TEACHER;
        } catch (Exception ex) {
            return false;
        }
    }

    @GetMapping("/{id}")
    public CourseResponse get(@PathVariable Long id) {
        CourseResponse course = service.get(id);
        if (!currentUserOptionalRoleIsAdminOrTeacher() && !"PUBLISHED".equals(course.status())) {
            throw new IllegalArgumentException("Course not found");
        }
        return course;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse create(@Valid @RequestBody CourseRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse publish(@PathVariable Long id) { return service.publish(id); }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse archive(@PathVariable Long id) { return service.archive(id); }

    @PatchMapping("/{id}/enrollment")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse setEnrollment(@PathVariable Long id, @RequestBody EnrollmentOpenRequest request) {
        return service.setEnrollment(id, request.open());
    }


    @PatchMapping("/{id}/relaunch")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse relaunch(@PathVariable Long id) {
        return service.relaunch(id);
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePermanent(@PathVariable Long id) {
        service.deletePermanent(id);
    }

}