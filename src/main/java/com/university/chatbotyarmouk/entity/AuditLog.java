package com.university.chatbotyarmouk.entity;

import jakarta.persistence.*;

@Entity
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: add fields

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
