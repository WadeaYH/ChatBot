package com.university.chatbotyarmouk.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * SchedulingConfig - Task Scheduling Configuration for YU ChatBot
 *
 * PURPOSE:
 * This class configures scheduled and asynchronous task execution:
 * - Automated web crawling (daily at 04:00 server time)
 * - Async processing for non-blocking operations
 * - Thread pool management for concurrent tasks
 *
 * KEY FEATURES:
 * 1. @EnableScheduling: Enables @Scheduled annotation support
 * 2. @EnableAsync: Enables @Async annotation support
 * 3. Custom thread pools for different task types
 *
 * SCHEDULING EXPLAINED (Real-Life Analogy):
 *
 * Think of scheduling like a building's maintenance system:
 * - Scheduled Tasks = Automatic routines (daily cleaning at 4 AM)
 * - Async Tasks = Background workers (cleaning while office is open)
 * - Thread Pool = Team of janitors (limited workers, queue for tasks)
 *
 * WHY SCHEDULED CRAWLING?
 * - Website content changes over time
 * - Daily updates ensure fresh RAG data
 * - 04:00 = Low traffic time (less server load)
 * - Automated = No manual intervention needed
 *
 * THREAD POOL CONCEPTS:
 *
 * Core Pool Size: Minimum threads always kept alive
 * - Like having permanent employees
 * - Always ready to handle tasks
 *
 * Max Pool Size: Maximum threads allowed
 * - Like hiring temporary workers during peak
 * - Created when queue is full
 *
 * Queue Capacity: Tasks waiting when all threads busy
 * - Like a waiting line
 * - Tasks wait here until a thread is free
 *
 * Keep Alive: How long idle threads survive
 * - Temporary workers go home if no work
 * - Saves resources
 *
 * @author YU ChatBot Team
 * @version 1.0
 */
@Slf4j
@Configuration
@EnableScheduling   // Enables @Scheduled annotations
@EnableAsync        // Enables @Async annotations
public class SchedulingConfig {

    /*
     * ==================== CONFIGURATION PROPERTIES ====================
     * Loaded from application.properties
     */

    /**
     * Core thread pool size for scheduled tasks
     *
     * Number of threads dedicated to scheduled tasks.
     * Default: 4 threads
     *
     * Considerations:
     * - More threads = More concurrent scheduled tasks
     * - Too many = Wasted resources if not used
     * - 4 is good for most applications
     */
    @Value("${scheduling.pool.core-size:4}")
    private int schedulerPoolSize;

    /**
     * Thread name prefix for scheduler
     *
     * Helps identify scheduler threads in logs and debugging.
     * Example thread name: "scheduler-1", "scheduler-2"
     */
    @Value("${scheduling.pool.thread-prefix:scheduler-}")
    private String schedulerThreadPrefix;

    /**
     * Core pool size for async tasks
     *
     * Minimum threads for async operations.
     * Default: 8 threads
     */
    @Value("${async.pool.core-size:8}")
    private int asyncCorePoolSize;

    /**
     * Maximum pool size for async tasks
     *
     * Maximum threads that can be created for async operations.
     * Default: 32 threads
     */
    @Value("${async.pool.max-size:32}")
    private int asyncMaxPoolSize;

    /**
     * Queue capacity for async tasks
     *
     * How many tasks can wait when all threads are busy.
     * Default: 100 tasks
     *
     * When queue is full and max threads reached:
     * - CallerRunsPolicy: Caller thread executes the task
     * - AbortPolicy: Throws RejectedExecutionException
     */
    @Value("${async.pool.queue-capacity:100}")
    private int asyncQueueCapacity;

    /**
     * Keep alive time for idle threads (seconds)
     *
     * How long excess threads (above core size) survive when idle.
     * Default: 60 seconds
     */
    @Value("${async.pool.keep-alive:60}")
    private int asyncKeepAlive;

    /**
     * Thread name prefix for async tasks
     *
     * Example thread name: "async-task-1", "async-task-2"
     */
    @Value("${async.pool.thread-prefix:async-task-}")
    private String asyncThreadPrefix;

    /**
     * Whether to wait for tasks to complete on shutdown
     *
     * true = Wait for running tasks to finish before shutdown
     * false = Interrupt tasks immediately
     */
    @Value("${async.pool.wait-for-completion:true}")
    private boolean waitForTasksToComplete;

    /**
     * Maximum wait time for shutdown (seconds)
     *
     * How long to wait for tasks to complete during shutdown.
     * After this time, tasks are forcefully interrupted.
     */
    @Value("${async.pool.await-termination:30}")
    private int awaitTerminationSeconds;

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {

        log.info("Configuring Task Scheduler with pool size: {}", schedulerPoolSize);

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        /*
         * ==================== POOL SIZE ====================
         * Number of threads for scheduled tasks
         *
         * Each @Scheduled method can run on its own thread.
         * If you have more scheduled methods than threads,
         * some will wait in queue.
         */
        scheduler.setPoolSize(schedulerPoolSize);

        /*
         * ==================== THREAD PREFIX ====================
         * Naming threads helps with debugging
         *
         * In logs you'll see:
         * [scheduler-1] INFO CrawlScheduler - Starting daily crawl
         */
        scheduler.setThreadNamePrefix(schedulerThreadPrefix);

        /*
         * ==================== ERROR HANDLER ====================
         * What happens when a scheduled task throws an exception?
         *
         * Default: Exception is logged and task continues next schedule
         * Custom: Can add retry logic, alerting, etc.
         */
        scheduler.setErrorHandler(throwable -> {
            log.error("Error in scheduled task: {}", throwable.getMessage(), throwable);
            // Could add alerting here (email, Slack, etc.)
        });

        /*
         * ==================== REMOVAL POLICY ====================
         * Remove cancelled tasks from queue immediately
         *
         * true = Clean up cancelled tasks right away
         * false = Wait until execution time to discover cancellation
         */
        scheduler.setRemoveOnCancelPolicy(true);

        /*
         * ==================== SHUTDOWN BEHAVIOR ====================
         * How to handle shutdown
         *
         * WaitForTasksToCompleteOnShutdown:
         * - true = Wait for running tasks to finish
         * - false = Interrupt immediately
         *
         * AwaitTerminationSeconds:
         * - Maximum wait time during shutdown
         */
        scheduler.setWaitForTasksToCompleteOnShutdown(waitForTasksToComplete);
        scheduler.setAwaitTerminationSeconds(awaitTerminationSeconds);

        /*
         * ==================== INITIALIZE ====================
         * Must call initialize() or afterPropertiesSet()
         * Spring usually does this, but explicit is safer
         */
        scheduler.initialize();

        return scheduler;
    }

    /**
     * ASYNC TASK EXECUTOR BEAN
     *
     * ThreadPoolTaskExecutor is used for @Async methods.
     *
     * Features:
     * - Non-blocking async execution
     * - Configurable thread pool
     * - Task queue for overflow
     * - Graceful shutdown
     *
     * Usage Example (in any service):
     * ```java
     * @Async("asyncTaskExecutor")
     * public CompletableFuture<Void> processDocumentAsync(String documentId) {
     *     // Long-running operation
     *     return CompletableFuture.completedFuture(null);
     * }
     * ```
     *
     * When to use @Async:
     * - Long-running operations (don't block HTTP thread)
     * - Fire-and-forget tasks (logging, notifications)
     * - Parallel processing (process multiple items concurrently)
     *
     * Real-Life Analogy:
     * Like a restaurant:
     * - Waiter takes order (HTTP thread)
     * - Chef cooks food (Async thread)
     * - Waiter serves other customers while cooking (Non-blocking)
     *
     * @return Configured ThreadPoolTaskExecutor
     */
    @Bean(name = "asyncTaskExecutor")
    public Executor asyncTaskExecutor() {

        log.info("Configuring Async Task Executor - Core: {}, Max: {}, Queue: {}",
                asyncCorePoolSize, asyncMaxPoolSize, asyncQueueCapacity);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        /*
         * ==================== CORE POOL SIZE ====================
         * Minimum threads always kept alive
         *
         * These threads are created on startup and never die.
         * Good for handling baseline load.
         */
        executor.setCorePoolSize(asyncCorePoolSize);

        /*
         * ==================== MAX POOL SIZE ====================
         * Maximum threads that can exist
         *
         * Extra threads created when:
         * - All core threads are busy
         * - Queue is full
         *
         * These threads die after keepAlive time if idle.
         */
        executor.setMaxPoolSize(asyncMaxPoolSize);

        /*
         * ==================== QUEUE CAPACITY ====================
         * Tasks waiting when all core threads busy
         *
         * Flow:
         * 1. Task arrives → Try core thread
         * 2. Core busy → Add to queue
         * 3. Queue full → Create new thread (up to max)
         * 4. Max reached + queue full → Rejection policy kicks in
         */
        executor.setQueueCapacity(asyncQueueCapacity);

        /*
         * ==================== KEEP ALIVE ====================
         * How long idle threads (above core) survive
         *
         * Threads above corePoolSize will be terminated
         * if they're idle for this duration.
         */
        executor.setKeepAliveSeconds(asyncKeepAlive);

        /*
         * ==================== THREAD PREFIX ====================
         * Naming for debugging
         *
         * In logs: [async-task-1] INFO SomeService - Processing...
         */
        executor.setThreadNamePrefix(asyncThreadPrefix);

        /*
         * ==================== REJECTION POLICY ====================
         * What happens when pool and queue are both full?
         *
         * Options:
         * 1. AbortPolicy (default): Throw RejectedExecutionException
         * 2. CallerRunsPolicy: Caller thread runs the task
         * 3. DiscardPolicy: Silently discard the task
         * 4. DiscardOldestPolicy: Discard oldest queued task
         *
         * CallerRunsPolicy is safest:
         * - Task still gets executed
         * - Provides natural backpressure (caller slows down)
         */
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        /*
         * ==================== SHUTDOWN BEHAVIOR ====================
         * Graceful shutdown handling
         */
        executor.setWaitForTasksToCompleteOnShutdown(waitForTasksToComplete);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);

        /*
         * ==================== INITIALIZE ====================
         * Initialize the executor
         */
        executor.initialize();

        return executor;
    }

    /**
     * CRAWL TASK EXECUTOR BEAN
     *
     * Dedicated executor for web crawling tasks.
     *
     * Separate pool because:
     * - Crawling is I/O intensive (network operations)
     * - May need many concurrent connections
     * - Shouldn't compete with other async tasks
     *
     * Usage:
     * ```java
     * @Async("crawlTaskExecutor")
     * public CompletableFuture<CrawlResult> crawlUrl(String url) {
     *     // Fetch and process URL
     * }
     * ```
     *
     * @return Configured ThreadPoolTaskExecutor for crawling
     */
    @Bean(name = "crawlTaskExecutor")
    public Executor crawlTaskExecutor() {

        log.info("Configuring Crawl Task Executor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        /*
         * Crawl-specific configuration:
         * - Higher concurrency (I/O bound, not CPU bound)
         * - Larger queue (many URLs to process)
         * - Longer timeouts (network latency)
         */
        executor.setCorePoolSize(16);    // 16 concurrent crawl threads
        executor.setMaxPoolSize(64);     // Up to 64 during peak
        executor.setQueueCapacity(500);  // Queue up to 500 URLs
        executor.setKeepAliveSeconds(30); // Quick cleanup
        executor.setThreadNamePrefix("crawl-task-");

        // CallerRunsPolicy: If overwhelmed, crawl sequentially
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);  // Give more time for crawl shutdown

        executor.initialize();

        return executor;
    }

    /**
     * EMBEDDING TASK EXECUTOR BEAN
     *
     * Dedicated executor for embedding generation.
     *
     * Separate pool because:
     * - Embedding API calls have rate limits
     * - Want to control concurrent API calls
     * - Expensive operation (API costs)
     *
     * @return Configured ThreadPoolTaskExecutor for embeddings
     */
    @Bean(name = "embeddingTaskExecutor")
    public Executor embeddingTaskExecutor() {

        log.info("Configuring Embedding Task Executor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        /*
         * Embedding-specific configuration:
         * - Lower concurrency (API rate limits)
         * - Large queue (batch processing)
         */
        executor.setCorePoolSize(4);     // 4 concurrent API calls
        executor.setMaxPoolSize(8);      // Up to 8 during peak
        executor.setQueueCapacity(1000); // Large queue for batch embedding
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("embedding-task-");

        // CallerRunsPolicy provides backpressure
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);  // Long wait for embeddings

        executor.initialize();

        return executor;
    }
}