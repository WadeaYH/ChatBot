package com.university.chatbotyarmouk.mongo.repository;


import com.university.chatbotyarmouk.mongo.model.CrawledDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CrawledDocumentRepository extends MongoRepository<CrawledDocument, String> {
}
