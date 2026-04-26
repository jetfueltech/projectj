# Roof Recon — Python processing service

FastAPI worker that owns steps 5, 6, 9, 10, 11 of the inspection pipeline.

```
POST /process/recon    -> plane_detection -> waypoint planner -> POST recon-result
POST /process/mission  -> orthomosaic     -> damage detection -> report -> POST mission-result
```

## Run locally

```bash
cp .env.example .env
pip install -e .
uvicorn app.main:app --reload --port 8000
```

`WEB_API_URL` and `INTERNAL_SECRET` must match the values used by the Next.js
app. `ANTHROPIC_API_KEY` is required for damage detection.

## Stubs to replace before going live

| Module | What it does today | Real implementation |
| --- | --- | --- |
| `modules/plane_detection.py` | Returns a single bounding-box plane from recon GPS | OpenSfM/COLMAP -> RANSAC plane fitting |
| `modules/orthomosaic.py` | Returns first image URL | OpenDroneMap / NodeODM |
| `modules/damage_detection.py` | Calls Claude vision per image | Fine-tuned YOLO/Mask R-CNN on collected dataset |
| `modules/report.py` | Returns summary text only | ReportLab PDF + Vercel Blob upload |

The waypoint planner (`modules/waypoints.py`) is real — it generates a
boustrophedon grid clipped to the roof polygon with overlap-driven spacing,
obstacle avoidance, and Mavic 3E sensor defaults.
