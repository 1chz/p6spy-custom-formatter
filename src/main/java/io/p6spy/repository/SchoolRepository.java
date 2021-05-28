package io.p6spy.repository;

import io.p6spy.domain.School;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, Long> {
    
    School findByName(String name);
}
