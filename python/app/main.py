from fastapi import BackgroundTasks, FastAPI, Header, HTTPException

from app.config import settings
from app.pipeline import process_mission, process_recon

app = FastAPI(title="Roof Recon processor")


def _check_secret(x_internal_secret: str | None) -> None:
    if not x_internal_secret or x_internal_secret != settings.internal_secret:
        raise HTTPException(status_code=403, detail="forbidden")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/process/recon")
def trigger_recon(
    payload: dict,
    background: BackgroundTasks,
    x_internal_secret: str | None = Header(default=None),
) -> dict[str, str]:
    _check_secret(x_internal_secret)
    job_id = payload.get("jobId")
    if not job_id:
        raise HTTPException(status_code=400, detail="jobId required")
    background.add_task(process_recon, job_id)
    return {"status": "accepted", "jobId": job_id}


@app.post("/process/mission")
def trigger_mission(
    payload: dict,
    background: BackgroundTasks,
    x_internal_secret: str | None = Header(default=None),
) -> dict[str, str]:
    _check_secret(x_internal_secret)
    job_id = payload.get("jobId")
    if not job_id:
        raise HTTPException(status_code=400, detail="jobId required")
    background.add_task(process_mission, job_id)
    return {"status": "accepted", "jobId": job_id}
