package com.university.chatbotyarmouk.mongo.repository;

import com.university.chatbotyarmouk.mongo.model.DocumentChunk;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DocumentChunkRepository extends MongoRepository<DocumentChunk, String> {
}
