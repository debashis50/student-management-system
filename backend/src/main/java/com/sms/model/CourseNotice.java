package com.sms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="course_notices")
public class CourseNotice {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="course_id") private Course course;
    @ManyToOne(optional=false) @JoinColumn(name="created_by") private AppUser createdBy;
    @Column(nullable=false, length=180) private String title;
    @Column(nullable=false, length=5000) private String message;
    @Column(nullable=false) private LocalDateTime createdAt = LocalDateTime.now();
    public Long getId(){return id;} public Course getCourse(){return course;} public void setCourse(Course v){course=v;}
    public AppUser getCreatedBy(){return createdBy;} public void setCreatedBy(AppUser v){createdBy=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getMessage(){return message;} public void setMessage(String v){message=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
}
