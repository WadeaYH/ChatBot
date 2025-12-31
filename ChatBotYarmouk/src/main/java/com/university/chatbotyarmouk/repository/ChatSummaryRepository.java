package com.university.chatbotyarmouk.repository;


import com.university.chatbotyarmouk.entity.ChatSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSummaryRepository extends JpaRepository<ChatSummary, Long> {
}
