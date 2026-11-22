package com.sms.config;

import com.sms.model.*;
import com.sms.repository.CourseRepository;
import com.sms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;

@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seedDemoData(UserRepository users, CourseRepository courses, PasswordEncoder encoder) {
        return args -> {
            AppUser admin = ensureUser(users, encoder, "admin", "admin@example.com", "Admin123!", "System Admin", Role.ADMIN);
            AppUser teacher = ensureUser(users, encoder, "teacher", "teacher@example.com", "Teacher123!", "Demo Teacher", Role.TEACHER);

            if (courses.count() == 0) {
                Course web = new Course();
                web.setCourseCode("WEB-101");
                web.setTitle("Web Development");
                web.setCategory("Development");
                web.setDescription("Learn the fundamentals of building modern websites.");
                web.setSyllabus("HTML and CSS\nJavaScript fundamentals\nResponsive layouts\nReact introduction");
                web.setFee(new BigDecimal("3000.00"));
                web.setDurationWeeks(8);
                web.setCreditHours(3);
                web.setCapacity(100);
                web.setStatus(CourseStatus.PUBLISHED);
                web.setEnrollmentOpen(true);
                web.setTeacher(teacher);
                courses.save(web);

                Course db = new Course();
                db.setCourseCode("DB-101");
                db.setTitle("Database Management");
                db.setCategory("Computer Science");
                db.setDescription("Learn relational database concepts and SQL.");
                db.setSyllabus("Relational model\nSQL\nJoins\nNormalization");
                db.setFee(new BigDecimal("2000.00"));
                db.setDurationWeeks(6);
                db.setCreditHours(3);
                db.setCapacity(80);
                db.setStatus(CourseStatus.PUBLISHED);
                db.setEnrollmentOpen(true);
                db.setTeacher(teacher);
                courses.save(db);

                Course python = new Course();
                python.setCourseCode("PY-101");
                python.setTitle("Python Programming");
                python.setCategory("Programming");
                python.setDescription("Start programming with Python.");
                python.setSyllabus("Syntax and data types\nConditions and loops\nFunctions\nOOP basics");
                python.setFee(new BigDecimal("1500.00"));
                python.setDurationWeeks(5);
                python.setCreditHours(2);
                python.setCapacity(100);
                python.setStatus(CourseStatus.PUBLISHED);
                python.setEnrollmentOpen(true);
                python.setTeacher(teacher);
                courses.save(python);
            }
        };
    }

    private AppUser ensureUser(
            UserRepository users,
            PasswordEncoder encoder,
            String username,
            String email,
            String password,
            String fullName,
            Role role) {

        AppUser user = users.findByUsername(username)
                .orElseGet(AppUser::new);

        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setFullName(fullName);
        user.setRole(role);
        user.setActive(true);

        return users.save(user);
    }
}
