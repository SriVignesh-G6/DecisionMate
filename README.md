# DecisionMate — Smart Decision Making

A student-friendly full-stack decision-support application built with React + Vite, Java Spring Boot, Spring Data JPA, and MySQL.

## Features
- Create and manage decisions.
- Add criteria and set their importance weights.
- Add alternatives to compare.
- Score every alternative against every criterion (1–10).
- Calculate weighted totals and rank alternatives automatically.
- Responsive dashboard and decision detail page.
- REST API with validation and clear project structure.

## Prerequisites
- Java 17+
- Maven 3.9+ (or use an IDE's Maven integration)
- Node.js 18+
- MySQL 8+

## 1. Create the database
Start MySQL, then run:

```sql
CREATE DATABASE decisionmate;
```

The app uses `spring.jpa.hibernate.ddl-auto=update` for a student-project setup, so tables are created automatically.

## 2. Configure the backend
Edit `backend/src/main/resources/application.properties` and set your MySQL username/password, or use environment variables:

- `DB_URL` (default `jdbc:mysql://localhost:3306/decisionmate`)
- `DB_USERNAME` (default `root`)
- `DB_PASSWORD` (default `root`)
- `SERVER_PORT` (default `8080`)

## 3. Run Spring Boot
From the project root:

```bash
cd backend
mvn spring-boot:run
```

API base URL: `http://localhost:8080/api`

Health check: `http://localhost:8080/api/health`

## 4. Run React
Open a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the URL printed by Vite (usually `http://localhost:5173`).

## Weighted scoring formula
For each criterion, the user enters an importance weight and a score from 1 to 10 for each alternative.

`Weighted total = Σ(score × weight) / Σ(weights)`

The result is a normalized score from 1 to 10. The highest score is ranked first. All criteria are treated as benefit criteria (higher is better) in this starter version.

## API overview
- `GET /api/health`
- `GET /api/decisions`
- `POST /api/decisions` — `{ "title": "...", "description": "..." }`
- `GET /api/decisions/{id}`
- `PUT /api/decisions/{id}`
- `DELETE /api/decisions/{id}`
- `POST /api/decisions/{id}/criteria` — `{ "name": "...", "weight": 3 }`
- `DELETE /api/criteria/{id}`
- `POST /api/decisions/{id}/alternatives` — `{ "name": "...", "description": "..." }`
- `DELETE /api/alternatives/{id}`
- `PUT /api/decisions/{decisionId}/scores` — `{ "alternativeId": 1, "criterionId": 1, "score": 8 }`
- `GET /api/decisions/{id}/results`

## Suggested demo
1. Create “Choose a Laptop”.
2. Add criteria: Price (weight 5), Performance (weight 4), Battery Life (weight 3).
3. Add alternatives: Laptop A, Laptop B, Laptop C.
4. Score each alternative for each criterion.
5. Open Results and explain the weighted ranking.

## Important notes
- This is an educational starter project, not a production deployment.
- Authentication, role-based access, audit history, and deployment automation are intentionally omitted to keep the project easy to run and explain.
- Before public deployment, add authentication/authorization, stricter ownership checks, migrations (Flyway), tests, and secrets management.
