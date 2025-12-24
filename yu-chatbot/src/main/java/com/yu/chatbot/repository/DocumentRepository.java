package com.yu.chatbot.repository;

import com.yu.chatbot.model.WebDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends MongoRepository<WebDocument, String> {

    Optional<WebDocument> findByUrl(String url);

    boolean existsByUrl(String url);

    List<WebDocument> findByFileType(String fileType);

    List<WebDocument> findByStatus(String status);

    @Query("{ 'content': { $regex: ?0, $options: 'i' } }")
    List<WebDocument> findByContentContaining(String keyword);

    @Query("{ 'title': { $regex: ?0, $options: 'i' } }")
    List<WebDocument> findByTitleContaining(String keyword);

    @Query("{ $or: [ { 'content': { $regex: ?0, $options: 'i' } }, { 'title': { $regex: ?0, $options: 'i' } } ] }")
    List<WebDocument> searchByKeyword(String keyword);

    List<WebDocument> findByUrlContaining(String urlPattern);

    long countByFileType(String fileType);

    void deleteByStatus(String status);
}