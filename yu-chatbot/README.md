# YU ChatBot - Milestone 1: Data Collection

## 📋 Requirements Met
- ✅ Spidering & Scraping (2 marks)
- ✅ NoSQL Database - MongoDB (2 marks)
- ✅ Retrieval with Citation (2 marks)
- ✅ REST API - GET, POST, PUT, DELETE (2 marks)

## 🚀 Quick Start

### 1. Install MongoDB
```bash
# Windows: Download from https://www.mongodb.com/try/download/community
# Or use Docker:
docker run -d -p 27017:27017 --name mongodb mongo:latest
```

### 2. Run the Application
```bash
cd yu-chatbot
mvn spring-boot:run
```

### 3. Test with Postman

## 📬 API Endpoints

### Crawler APIs
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/crawler/start` | Start crawling |
| GET | `/api/crawler/status/{jobId}` | Check status |

### Document APIs (CRUD)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/documents` | Get all documents |
| GET | `/api/documents/{id}` | Get by ID |
| GET | `/api/documents/search?q=keyword` | Search (with citation) |
| GET | `/api/documents/url?url=...` | Get by URL |
| GET | `/api/documents/type/{type}` | Get by file type |
| GET | `/api/documents/stats` | Get statistics |
| POST | `/api/documents` | Create document |
| PUT | `/api/documents/{id}` | Update document |
| DELETE | `/api/documents/{id}` | Delete document |

## 🧪 Postman Test Examples

### 1. Start Crawling
```
POST http://localhost:8080/api/crawler/start
Content-Type: application/json

{
  "url": "https://yu.edu.jo",
  "maxDepth": 2
}
```

### 2. Search Documents
```
GET http://localhost:8080/api/documents/search?q=admission
```

### 3. Create Document
```
POST http://localhost:8080/api/documents
Content-Type: application/json

{
  "url": "https://yu.edu.jo/test",
  "title": "Test Page",
  "content": "Test content",
  "fileType": "HTML"
}
```

### 4. Update Document
```
PUT http://localhost:8080/api/documents/{id}
Content-Type: application/json

{
  "title": "Updated Title",
  "content": "Updated content"
}
```

### 5. Delete Document
```
DELETE http://localhost:8080/api/documents/{id}
```

## 📁 Project Structure
```
yu-chatbot/
├── src/main/java/com/yu/chatbot/
│   ├── YuChatbotApplication.java
│   ├── controller/
│   │   ├── CrawlerController.java
│   │   └── DocumentController.java
│   ├── service/
│   │   ├── CrawlerService.java
│   │   ├── DocumentService.java
│   │   └── extractor/
│   │       ├── HtmlExtractor.java
│   │       ├── PdfExtractor.java
│   │       └── WordExtractor.java
│   ├── model/
│   │   └── WebDocument.java
│   └── repository/
│       └── DocumentRepository.java
├── src/main/resources/
│   └── application.properties
└── pom.xml
```

## 🎬 Demo Script
1. POST `/api/crawler/start` with YU URL
2. GET `/api/crawler/status/{jobId}` - show progress
3. GET `/api/documents` - show stored data
4. GET `/api/documents/search?q=admission` - show retrieval with citation
5. POST/PUT/DELETE operations

Good luck Wadea! 🚀
