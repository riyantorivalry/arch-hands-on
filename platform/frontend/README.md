# Frontend

This is the Phase 1 React frontend scaffold for the collaboration platform.

## Stack

- React
- Next.js App Router
- TypeScript

## Current scope

- tenant/workspace bootstrap flow
- login with backend session token
- workspace dashboard for channels, documents, and tasks
- create actions for first-phase collaboration flows

## Environment

Copy `.env.example` to `.env.local` and set:

- `NEXT_PUBLIC_API_BASE_URL`

Default local backend target:

```text
http://localhost:8080/api
```

## Notes

- the app stores the bearer token in browser local storage for now
- it targets the existing backend API directly
- there is no dependency installation in this repository yet; run `npm install` before starting the dev server
