package com.university.chatbotyarmouk.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.concurrent.TimeUnit;

/**
 * MongoConfig - MongoDB Configuration for YU ChatBot
 *
 * PURPOSE:
 * This class configures MongoDB connection and settings for:
 * - Storing crawled documents (CrawledDocument)
 * - Storing text chunks with embeddings (DocumentChunk)
 * - Vector similarity search for RAG (Retrieval-Augmented Generation)
 *
 * WHY MONGODB FOR RAG?
 * MongoDB is excellent for RAG systems because:
 * 1. Document-based: Natural fit for storing web pages/documents
 * 2. Flexible schema: Can store varying document structures
 * 3. Atlas Vector Search: Native vector similarity queries
 * 4. Scalability: Horizontal scaling for large document collections
 *
 * REAL-LIFE ANALOGY:
 * Think of MongoDB as a smart filing cabinet:
 * - PostgreSQL (relational) = Traditional cabinet with fixed folders
 * - MongoDB (document) = Flexible cabinet where each drawer can have different compartments
 *
 * For RAG:
 * - Documents = Filed papers
 * - Chunks = Highlighted paragraphs from papers
 * - Embeddings = Fingerprints that help find similar content
 * - Vector Search = Finding papers by "meaning" not just keywords
 *
 * @EnableMongoRepositories: Enables automatic repository creation
 * - basePackages: Where to scan for repository interfaces
 *
 * @author YU ChatBot Team
 * @version 1.0
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.university.chatbotyarmouk.repository.mongo")
public class MongoConfig extends AbstractMongoClientConfiguration {

    /*
     * ==================== CONFIGURATION PROPERTIES ====================
     * Loaded from application.properties file
     */

    /**
     * MongoDB connection URI
     *
     * Format: mongodb://[username:password@]host[:port]/database[?options]
     *
     * Examples:
     * - Local: mongodb://localhost:27017/yuchatbot
     * - Atlas: mongodb+srv://user:pass@cluster.mongodb.net/yuchatbot
     * - With auth: mongodb://admin:secret@localhost:27017/yuchatbot?authSource=admin
     */
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    /**
     * Database name
     *
     * This is the main database where all collections are stored.
     * Collections include: crawled_documents, document_chunks
     */
    @Value("${spring.data.mongodb.database:yuchatbot}")
    private String databaseName;

    /**
     * Connection timeout in milliseconds
     *
     * How long to wait when establishing connection.
     * Default: 10000ms (10 seconds)
     */
    @Value("${spring.data.mongodb.connection-timeout:10000}")
    private int connectionTimeout;

    /**
     * Socket timeout in milliseconds
     *
     * How long to wait for a response after sending a request.
     * Default: 60000ms (60 seconds) - longer for large documents
     */
    @Value("${spring.data.mongodb.socket-timeout:60000}")
    private int socketTimeout;

    /**
     * Maximum connection pool size
     *
     * Number of connections to maintain to MongoDB.
     * More connections = more concurrent operations
     * Default: 100 connections
     */
    @Value("${spring.data.mongodb.max-pool-size:100}")
    private int maxPoolSize;

    /**
     * Minimum connection pool size
     *
     * Minimum connections to keep open (even when idle).
     * Reduces connection establishment overhead.
     * Default: 10 connections
     */
    @Value("${spring.data.mongodb.min-pool-size:10}")
    private int minPoolSize;

    /**
     * REQUIRED: Specify the database name
     *
     * This method is required by AbstractMongoClientConfiguration.
     * It tells Spring Data MongoDB which database to use.
     *
     * @return Database name
     */
    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    /**
     * MONGODB CLIENT CONFIGURATION
     *
     * Creates and configures the MongoClient instance.
     * MongoClient is the main entry point for all MongoDB operations.
     *
     * Configuration includes:
     * - Connection settings (timeouts, pool sizes)
     * - Codec configuration (how to encode/decode documents)
     * - Retry policies
     *
     * Real-Life Analogy:
     * MongoClient is like a telephone operator:
     * - Maintains connections to MongoDB (phone lines)
     * - Routes requests to the right database (call routing)
     * - Handles connection failures (retry on busy signal)
     *
     * @return Configured MongoClient instance
     */
    @Override
    @Bean
    public MongoClient mongoClient() {

        // Parse the connection string from properties
        ConnectionString connectionString = new ConnectionString(mongoUri);

        // Build MongoClient settings with custom configuration
        MongoClientSettings settings = MongoClientSettings.builder()

                /*
                 * ==================== CONNECTION STRING ====================
                 * Apply settings from the URI (host, port, credentials)
                 */
                .applyConnectionString(connectionString)

                /*
                 * ==================== SOCKET SETTINGS ====================
                 * Configure network-level settings
                 *
                 * connectTimeout: Time to establish TCP connection
                 * readTimeout (socketTimeout): Time to wait for response
                 */
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                        .readTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                )

                /*
                 * ==================== CONNECTION POOL SETTINGS ====================
                 * Configure the connection pool
                 *
                 * Connection pooling:
                 * - Reuses connections instead of creating new ones
                 * - Reduces overhead of connection establishment
                 * - Limits concurrent connections to prevent overload
                 *
                 * Real-Life Analogy:
                 * Like a car rental service:
                 * - maxSize = Maximum cars available
                 * - minSize = Cars always kept ready
                 * - maxWaitTime = How long customers wait if no car available
                 */
                .applyToConnectionPoolSettings(builder -> builder
                        .maxSize(maxPoolSize)           // Maximum connections
                        .minSize(minPoolSize)           // Minimum connections to maintain
                        .maxWaitTime(30, TimeUnit.SECONDS)  // Max wait for connection
                        .maxConnectionIdleTime(60, TimeUnit.SECONDS)  // Close idle connections after 60s
                )

                /*
                 * ==================== SERVER SETTINGS ====================
                 * Configure server monitoring
                 *
                 * heartbeatFrequency: How often to check server health
                 * Used for replica set monitoring and failover detection
                 */
                .applyToServerSettings(builder -> builder
                        .heartbeatFrequency(10, TimeUnit.SECONDS)
                )

                /*
                 * ==================== RETRY SETTINGS ====================
                 * Enable automatic retry for transient failures
                 *
                 * retryWrites: Retry failed write operations
                 * retryReads: Retry failed read operations
                 *
                 * Handles temporary issues like:
                 * - Network glitches
                 * - Primary election (replica set)
                 * - Temporary overload
                 */
                .retryWrites(true)
                .retryReads(true)

                .build();

        // Create and return the MongoClient
        return MongoClients.create(settings);
    }

    /**
     * MONGO DATABASE FACTORY
     *
     * Factory for creating database connections.
     * Used internally by Spring Data MongoDB.
     *
     * @return MongoDatabaseFactory instance
     */
    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory() {
        return new SimpleMongoClientDatabaseFactory(mongoClient(), getDatabaseName());
    }

    /**
     * MONGO TEMPLATE
     *
     * MongoTemplate is the primary class for MongoDB operations.
     * It provides methods for:
     * - CRUD operations (insert, find, update, delete)
     * - Aggregation pipelines
     * - Index management
     * - Custom queries
     *
     * Usage Example:
     * ```java
     * // Insert document
     * mongoTemplate.insert(document);
     *
     * // Find by query
     * Query query = new Query(Criteria.where("url").is(url));
     * CrawledDocument doc = mongoTemplate.findOne(query, CrawledDocument.class);
     *
     * // Aggregation for vector search
     * AggregationResults<DocumentChunk> results = mongoTemplate.aggregate(
     *     aggregation, "document_chunks", DocumentChunk.class
     * );
     * ```
     *
     * Real-Life Analogy:
     * MongoTemplate is like a librarian:
     * - Knows how to find books (find operations)
     * - Knows how to add new books (insert operations)
     * - Can do complex searches (aggregation pipelines)
     *
     * @return Configured MongoTemplate
     */
    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoDatabaseFactory());
    }

    /**
     * AUTO-INDEX CREATION
     *
     * Enable/disable automatic index creation from @Indexed annotations.
     *
     * true = Spring creates indexes on startup based on entity annotations
     * false = Indexes must be created manually
     *
     * For production:
     * - Set to false and manage indexes via migration scripts
     * - Prevents unexpected index creation on startup
     *
     * For development:
     * - Set to true for convenience
     *
     * @return true to enable auto-index creation
     */
    @Override
    protected boolean autoIndexCreation() {
        return true;  // Enable for development; disable in production
    }
}

/*
 * ==================== VECTOR SEARCH SETUP NOTES ====================
 *
 * For MongoDB Atlas Vector Search, create the following index:
 *
 * Collection: document_chunks
 * Index Name: vector_index
 *
 * Index Definition (JSON):
 * {
 *   "fields": [
 *     {
 *       "type": "vector",
 *       "path": "embeddingVector",
 *       "numDimensions": 768,  // Depends on embedding model
 *       "similarity": "cosine"
 *     },
 *     {
 *       "type": "filter",
 *       "path": "url"
 *     }
 *   ]
 * }
 *
 * Query Example (Aggregation Pipeline):
 * [
 *   {
 *     "$vectorSearch": {
 *       "index": "vector_index",
 *       "path": "embeddingVector",
 *       "queryVector": [0.1, 0.2, ...],  // 768 dimensions
 *       "numCandidates": 100,
 *       "limit": 10
 *     }
 *   },
 *   {
 *     "$project": {
 *       "chunkText": 1,
 *       "url": 1,
 *       "score": { "$meta": "vectorSearchScore" }
 *     }
 *   }
 * ]
 *
 * If not using Atlas, implement cosine similarity in code:
 * See VectorSearchService for implementation.
 */