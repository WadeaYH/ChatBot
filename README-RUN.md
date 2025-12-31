RUN BACKEND (NO DOCKER)

1) Install Java 21
2) Install PostgreSQL + create DB:
    - DB name: yu_chatbot
    - user: postgres
    - pass: postgres

3) Install MongoDB
    - default URI: mongodb://localhost:27017/yu_chatbot

4) Run Spring Boot:
    - from IntelliJ: Run ChatBotYarmoukApplication
    - or terminal: mvn spring-boot:run

DEFAULTS ARE ALREADY IN application.properties.
If you want production-like settings, set env vars:

- APP_JWT_SECRET=YOUR_LONG_SECRET_32+CHARS
- SPRING_DATASOURCE_URL=jdbc:postgresql://HOST:5432/yu_chatbot
- SPRING_DATASOURCE_USERNAME=...
- SPRING_DATASOURCE_PASSWORD=...
- SPRING_DATA_MONGODB_URI=mongodb://HOST:27017/yu_chatbot
- APP_SIS_MODE=mock or http
