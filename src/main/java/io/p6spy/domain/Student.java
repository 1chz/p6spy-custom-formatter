package io.p6spy.domain;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student implements Serializable {
    private static final long serialVersionUID = 23186552736747631L;
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    private School school;
    
    private String name;
    
    private int age;
    
    protected void setSchool(School school) {
        this.school = school;
    }
}
