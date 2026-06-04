# Medicine Tracker

A cross-platform (Android + iOS) family medication tracker built with **React Native + Expo**.

Multiple family members on different devices share one household's medication schedule and
dose history, with reliable reminders, pill-bottle OCR scanning, and AI assistance.

## Tech stack
- **Framework:** React Native + Expo (TypeScript), expo-router
- **Backend:** Firebase — Firestore (offline-first sync), Auth, Cloud Messaging, Cloud Functions
- **Local DB:** expo-sqlite + Drizzle ORM (reactive cache + reminder schedule)
- **Reminders:** expo-notifications (exact alarms) + FCM push backup
- **OCR:** expo-camera + Google ML Kit text recognition
- **AI:** Google Gemini via a Cloud Function proxy

The full build plan (architecture, data model, security rules, milestones, and verification)
is the project's source of truth.

## Getting started
This app uses native modules, so it runs in an **Expo development build** (not Expo Go).

```bash
npm install
npm run android   # or: npm run ios
```

## Project structure
```
app/        expo-router routes (auth, tabs, modals, onboarding)
src/        db, firebase, sync, reminders, ocr, ai, features, ui, state
functions/  Firebase Cloud Functions (push scheduler, AI proxy, invites)
```

## Status
**M0 — project scaffold:** Expo TypeScript app with the three-tab shell (Home / Kids / Account).
Subsequent milestones (M1–M9) build the data layer, auth, sync, reminders, OCR, and AI.
