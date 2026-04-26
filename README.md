# Roof Recon

Drone-based roof inspection platform built around DJI's Mobile SDK.

The system covers the full roofing-contractor workflow:

| Step | Component | Notes |
| --- | --- | --- |
| 1. Roofer enters property info | `web/` (Next.js) | `/properties/new` |
| 2. App connects to drone | `mobile/` | DJI MSDK v5, Android |
| 3. Recon flight | `mobile/` | Manual or automated orbit |
| 4. Images upload to backend | `mobile/` -> `/api/mobile/jobs/:id/images` | Multipart with EXIF/telemetry |
| 5. Detect roof planes/obstacles | `python/` | `app/modules/plane_detection.py` |
| 6. Generate roof-specific waypoint grid | `python/` | `app/modules/waypoints.py` (real boustrophedon planner) |
| 7. App sends waypoint mission to DJI SDK | `mobile/` | WPML KMZ via `MissionKmzBuilder` |
| 8. Drone captures images | `mobile/` | Auto mission, photo at each waypoint |
| 9. Stitch orthomosaic | `python/` | `app/modules/orthomosaic.py` (stub -> swap for ODM) |
| 10. Detect damage | `python/` | `app/modules/damage_detection.py` (Claude vision today) |
| 11. Generate report | `python/` -> `web/` | `/jobs/:id/report` |

## Repo layout

```
.
├── app/                Next.js App Router (web dashboard + REST API)
├── components/         shadcn/ui + app-specific components
├── lib/db, lib/auth    Drizzle schema/queries, mobile JWT, internal-secret check
├── lib/processing      Trigger client for the Python service
├── python/             FastAPI worker (steps 5, 6, 9, 10, 11)
└── mobile/             Android Kotlin app (DJI MSDK v5)
```

## Running locally

```bash
# 1. Web
cp .env.example .env.local
pnpm install
pnpm db:generate && pnpm db:migrate
pnpm dev    # http://localhost:3000

# 2. Python
cd python
cp .env.example .env
pip install -e .
uvicorn app.main:app --reload --port 8000

# 3. Android
cd mobile
./gradlew assembleDebug \
    -PbackendUrl=http://10.0.2.2:3000 \
    -PdjiAppKey=YOUR_DJI_APP_KEY
```

`AUTH_SECRET`, `INTERNAL_SECRET`, and `POSTGRES_URL` need values before the
web app will boot. The mobile app additionally needs a DJI developer account
to mint an app key.

## Environment

Web (`.env.local`):
* `AUTH_SECRET` — signs both web sessions and mobile JWTs.
* `POSTGRES_URL`, `BLOB_READ_WRITE_TOKEN` — data + image storage.
* `PYTHON_SERVICE_URL` — base URL the web app POSTs to when triggering processing.
* `INTERNAL_SECRET` — shared with `python/.env`; used for the web ↔ python callbacks.

Python (`python/.env`):
* `WEB_API_URL` — pointed at the Next.js deployment.
* `INTERNAL_SECRET` — same value as the web side.
* `ANTHROPIC_API_KEY` — used by the damage-detection module.

## Stubs that need real implementations before flying

See per-component READMEs:
* `python/README.md` — plane detection, orthomosaic stitching, PDF report.
* `mobile/README.md` — DJI capture/mission execution wiring.
