package io.p6spy.request;

import io.p6spy.domain.School;
import io.p6spy.domain.Student;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatriculationRequest {
    
    private String schoolName;
    private String studentName;
    private int studentAge;
    
    public Student getStudent() {
        return Student.builder()
                      .name(this.studentName)
                      .age(this.studentAge)
                      .build();
    }
}
