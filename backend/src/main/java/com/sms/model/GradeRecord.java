package com.sms.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name="grade_records", uniqueConstraints=@UniqueConstraint(columnNames={"course_id","student_id","assessment_name"}))
public class GradeRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) @JoinColumn(name="course_id") private Course course;
    @ManyToOne(optional=false) @JoinColumn(name="student_id") private AppUser student;
    @Column(name="assessment_name", nullable=false, length=160) private String assessmentName;
    @Column(nullable=false, precision=8, scale=2) private BigDecimal score;
    @Column(name="max_score", nullable=false, precision=8, scale=2) private BigDecimal maxScore;
    @Column(length=1000) private String remarks;
    public Long getId(){return id;} public Course getCourse(){return course;} public void setCourse(Course v){course=v;}
    public AppUser getStudent(){return student;} public void setStudent(AppUser v){student=v;}
    public String getAssessmentName(){return assessmentName;} public void setAssessmentName(String v){assessmentName=v;}
    public BigDecimal getScore(){return score;} public void setScore(BigDecimal v){score=v;}
    public BigDecimal getMaxScore(){return maxScore;} public void setMaxScore(BigDecimal v){maxScore=v;}
    public String getRemarks(){return remarks;} public void setRemarks(String v){remarks=v;}
}
