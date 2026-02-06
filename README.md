# Email Draft Assistant (InboxAI)

Backend (Spring Boot, Kafka, Redis, Gemini) and frontend (React, Tailwind) for an AI-powered email draft assistant. Connects to Gmail, categorizes incoming mail, generates draft replies with AI, and lets you review or skip before sending.

## Demo Video
<p align="center" width="100%">
<video src="https://github.com/user-attachments/assets/f9da702d-9f2b-41f2-b11b-eaf50f905615" width="80%" controls muted></video>
</p>

## Layout

- **`backend/`** – Spring Boot app (Gmail → Kafka → categorize → draft → Redis). Run from here.
- **`frontend/`** – Vite + React + Tailwind dashboard. Run from here.
- **`docker/`** – Kafka, Redis, Kafka UI, RedisInsight (Compose).
- **`test/`** – Demo email seed script for local/demo use.

## Prerequisites

- Java 21, Maven
- Node.js 18+ (for frontend)
- Docker (for Kafka + Redis)
- Google Cloud project with Gmail API and OAuth credentials
- Gemini API key (for categorization and draft generation)

## Quick start

1. Start infrastructure:
   ```bash
   docker compose -f docker/docker-compose.yml up -d
   ```
2. Configure backend: copy `backend/src/main/resources/application-local.yml.example` to `application-local.yml` and set OAuth client ID/secret, Gemini API key, etc.
3. Run backend:
   ```bash
   cd backend && mvn spring-boot:run
   ```
   App listens on port 8080.
4. Run frontend:
   ```bash
   cd frontend && npm install && npm run dev
   ```
   Open http://localhost:5173. The dev server proxies `/api` to the backend.

5. Log in via the app (OAuth with Gmail), then use **Drafts** or seed demo data (see below).

## Demo data (no Gmail needed)

To trigger the pipeline with fake emails for a demo:

```bash
./test/seed-demo-emails.sh
```

Uses `test/demo-emails.json` and `POST /api/gmail/seed`. Backend and Kafka must be running.

## License

- Code: see [LICENSE](LICENSE).
- Docs: [LICENSE-DOCS.md](LICENSE-DOCS.md) (CC-BY 4.0).
