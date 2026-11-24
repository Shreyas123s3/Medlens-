# MedLens backend deployment (Vercel)

This private API proxy keeps the Gemini key on the server. The Android app contains only the MedLens backend URL. Never paste the provider key into Android settings, source, or GitHub files.

## Deploy for a beginner

1. Create a Vercel account at [vercel.com](https://vercel.com/) and connect the GitHub account containing MedLens.
2. Select **Add New → Project** and import `Shreyas123s3/medlens`.
3. In project settings during import, set **Root Directory** to `backend` and leave the framework as Other. Vercel should use the serverless functions in `api/`.
4. Under **Environment Variables**, add `GEMINI_API_KEY` with a newly generated key from Google AI Studio. Optionally add `GEMINI_MODEL` (default `gemini-3.8-flash`). Do not commit a `.env` file.
5. Deploy. Copy the public deployment URL, then open `<deployment-url>/api/health`; it should return `{"ok":true,"service":"medlens-api"}`.
6. In the GitHub repository, add **Settings → Secrets and variables → Actions → Variables → New repository variable** named `MEDLENS_BACKEND_URL`, with the deployment URL (no `/api/chat` suffix). Run the **Build MedLens APK** workflow again and install that newly built APK.

For local backend work, copy `.env.example` to `.env` and set a key only in your local ignored environment. The Android client does not read that file.

## API

- `GET /api/health` checks service availability and does not expose configuration.
- `POST /api/chat` accepts the last conversation turns and medicine facts from the app's local Room database. It rejects browser-origin requests, validates size and structure, limits per-instance requests, times out, and does not log message text. The rate limiter is process-local; deployments with multiple instances may need a shared store for a strict global quota.

The system prompt restricts the model to supplied medicine facts and educational responses. Client-side emergency detection runs before this endpoint is called; the server repeats an emergency check as defense in depth. Offline mode remains available when consent is declined, the URL is unset, or a request fails.
