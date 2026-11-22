package com.sms.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="attendance_records", uniqueConstraints=@UniqueConstraint(columnNames={"course_id","student_id","attendance_date"}))
public class AttendanceRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="course_id") private Course course;
    @ManyToOne(optional=false) @JoinColumn(name="student_id") private AppUser student;
    @Column(name="attendance_date", nullable=false) private LocalDate attendanceDate;
    @Column(nullable=false) private boolean present;
    @Column(length=500) private String note;
    public Long getId(){return id;} public Course getCourse(){return course;} public void setCourse(Course v){course=v;}
    public AppUser getStudent(){return student;} public void setStudent(AppUser v){student=v;}
    public LocalDate getAttendanceDate(){return attendanceDate;} public void setAttendanceDate(LocalDate v){attendanceDate=v;}
    public boolean isPresent(){return present;} public void setPresent(boolean v){present=v;}
    public String getNote(){return note;} public void setNote(String v){note=v;}
}
