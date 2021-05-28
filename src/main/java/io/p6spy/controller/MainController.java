package io.p6spy.controller;

import io.p6spy.domain.School;
import io.p6spy.domain.Student;
import io.p6spy.repository.SchoolRepository;
import io.p6spy.repository.StudentRepository;
import io.p6spy.request.MatriculationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class MainController {
    
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    
    @GetMapping("/")
    public String index() {
        return "index";
    }
    
    @ResponseBody
    @PostMapping("/matriculation")
    public void run(MatriculationRequest matriculationRequest) {
        Student student = matriculationRequest.getStudent();
        School school = schoolRepository.findByName(matriculationRequest.getSchoolName());
        if(school == null) {
            school = schoolRepository.save(createSchool());
        }
        school.matriculation(student);
        call_1(student);
    }
    
    private School createSchool() {
        return School.builder()
                     .name("용산초")
                     .build();
    }
    
    private void call_1(Student student) {
        call_2(student);
    }
    
    private void call_2(Student student) {
        call_3(student);
    }
    
    private void call_3(Student student) {
        studentRepository.save(student);
    }
    
}
