package com.university.chatbotyarmouk.mongo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "documentchunks")
public class DocumentChunk {

    @Id
    private String id;

    // TODO: add fields

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
