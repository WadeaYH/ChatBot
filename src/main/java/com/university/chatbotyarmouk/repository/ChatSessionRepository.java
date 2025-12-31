package com.university.chatbotyarmouk.repository;


import com.university.chatbotyarmouk.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
}
