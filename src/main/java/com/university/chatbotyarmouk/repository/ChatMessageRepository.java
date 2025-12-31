package com.university.chatbotyarmouk.repository;


import com.university.chatbotyarmouk.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}
