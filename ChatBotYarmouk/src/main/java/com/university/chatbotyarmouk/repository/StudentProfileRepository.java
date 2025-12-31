package com.university.chatbotyarmouk.repository;


import com.university.chatbotyarmouk.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
}
