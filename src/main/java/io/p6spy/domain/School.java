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
public class School implements Serializable {  
    private static final long serialVersionUID = 343332473817391823L;
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    public Student matriculation(Student student) {
        student.setSchool(this);
        return student;
    }
}
