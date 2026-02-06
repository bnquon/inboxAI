# InboxAI Frontend

Barebones React dashboard for the email draft assistant. Built with Vite, React, and Tailwind CSS.

## Run locally

1. Start the backend (from repo root: `cd backend && mvn spring-boot:run`).
2. Start Kafka + Redis (e.g. `docker compose -f docker/docker-compose.yml up -d`).
3. From this folder: `npm run dev`. App runs at http://localhost:5173.

The dev server proxies `/api` to `http://localhost:8080` so the frontend can call the backend without CORS issues.

## What’s included

- **Sidebar:** InboxAI logo, Dashboard, Drafts (with count badge), Preferences.
- **Dashboard:** Placeholder metric cards (no backend data yet).
- **Drafts:** List from `GET /api/drafts`; click a draft to load `GET /api/drafts/:emailId` and show original email + generated draft side-by-side.

Edit / Approve / Reject and real metrics are not implemented yet.
