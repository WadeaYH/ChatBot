package com.university.chatbotyarmouk.repository;


import com.university.chatbotyarmouk.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
