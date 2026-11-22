package com.sms.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="course_assignments")
public class CourseAssignment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="course_id") private Course course;
    @ManyToOne(optional=false) @JoinColumn(name="created_by") private AppUser createdBy;
    @Column(nullable=false, length=180) private String title;
    @Column(length=4000) private String description;
    private LocalDate dueDate;
    public Long getId(){return id;} public Course getCourse(){return course;} public void setCourse(Course v){course=v;}
    public AppUser getCreatedBy(){return createdBy;} public void setCreatedBy(AppUser v){createdBy=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate v){dueDate=v;}
}
