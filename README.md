# CourseHub — Online Course Management System

Full-stack learning management starter using React + TypeScript, Java 21, Spring Boot, Maven, and MySQL. No separate `database` folder is required: Hibernate creates/updates the tables from JPA entities (`spring.jpa.hibernate.ddl-auto=update`).

## Features implemented in this revision

### Authentication and roles
- Student self-registration; public registration always assigns `STUDENT`.
- JWT login and role-based authorization for Admin, Teacher, and Student.
- Login accepts username or email.
- BCrypt password hashing.
- Profile update and authenticated password change.
- Admin can create Student/Teacher accounts and activate/deactivate non-admin accounts.

### Admin
- Dashboard counts for students, active/inactive students, teachers, courses, course status, and enrollments.
- Course create/edit, draft/publish/archive, open/close enrollment, instructor assignment, fee, dates, duration, credits, capacity.
- User management for student and teacher accounts.

### Student
- Search/filter published course catalog and course fee/details.
- Enroll/drop courses with duplicate, deadline, enrollment-open and capacity checks.
- My Courses and active enrollment fee total. There is no payment gateway or payment processing.
- Profile editing and password change.
- Attendance, results/grades, assignments and notices for active enrollments.

### Teacher
- Assigned course view and enrolled student list.
- Record attendance and grades for active students.
- Create course assignments and notices.

## Requirements
- JDK 21
- MySQL 8 or compatible
- Node.js 20+
- IntelliJ IDEA (Community is fine)
- Maven (or use IntelliJ's Maven integration)

## Configure MySQL
Start MySQL, then create the database once:

```sql
CREATE DATABASE student_management;
```

Open `backend/src/main/resources/application.properties` and set credentials. For a passwordless local MySQL `root` account:

```properties
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
```

If your MySQL account has a password, set `DB_PASSWORD` or put it in the property default. Do not use an empty password on a shared/production system.

## Run backend
1. Open the project root in IntelliJ.
2. Set Project SDK to JDK 21.
3. Reload Maven from `backend/pom.xml`.
4. Run `com.sms.StudentManagementSystemApplication` or run from the backend directory:

```bash
mvn spring-boot:run
```

Backend: `http://localhost:8080`. Hibernate creates the tables automatically.

## Run frontend
In a terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Keep both backend and frontend running.

## Demo accounts
- Admin: `admin@example.com` / `Admin123!`
- Teacher: `teacher@example.com` / `Teacher123!`

The seeder initializes these demo accounts and refreshes their demo credentials on startup. This is intended only for local demonstration. Do not use demo passwords or the demo JWT secret in production.

New students register from the login page using name, email and password. Their role is assigned on the server.

## Academic feature workflow
1. Admin creates and publishes a course, assigns a teacher, and opens enrollment.
2. Students register, log in, and enroll.
3. Teacher opens My Teaching, selects an assigned course, sees active enrolled students and uses their Student ID to record attendance/grades or publish assignments/notices.
4. Students see these records under Attendance, Results, Assignments, and Notices while actively enrolled.

## Important behavior
- Fee is display-only. The current amount is the sum of fees for active enrollments; dropping a course removes it from the current total.
- Students cannot assign themselves a Teacher/Admin role.
- Teachers can only manage academic records for courses assigned to them.
- Course drop is blocked after the course's enrollment deadline when a deadline is configured.
- Profile update and password change require authentication.

## API overview
- `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`, `PUT /api/auth/password`
- `GET/PUT /api/profile/me`
- `GET /api/admin/dashboard`, `GET/POST /api/admin/users`, `PATCH /api/admin/users/{id}/active?value=true|false`
- `/api/courses` (catalog and admin course management)
- `/api/enrollments/my`, `/api/enrollments/my/summary`, `POST/DELETE /api/enrollments/{courseId}`
- `/api/teacher/courses/{courseId}/students`
- `/api/teacher/courses/{courseId}/attendance` (GET/POST)
- `/api/teacher/courses/{courseId}/grades` (GET/POST)
- `POST /api/teacher/courses/{courseId}/assignments`, `POST /api/teacher/courses/{courseId}/notices`
- `GET /api/student/attendance`, `/api/student/results`, `/api/student/assignments`, `/api/student/notices`

## Verification note
The frontend TypeScript project was checked with `tsc -b`. The backend was not built in this environment because Maven is not installed here; reload Maven and run the backend locally in IntelliJ. If compilation fails, share the first Maven compilation error so it can be corrected precisely.
