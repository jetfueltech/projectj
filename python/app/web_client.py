"""HTTP client for the Next.js web backend's internal API."""

from __future__ import annotations

import httpx

from app.config import settings


def _headers() -> dict[str, str]:
    return {"x-internal-secret": settings.internal_secret}


def fetch_images(job_id: str, kind: str) -> list[dict]:
    r = httpx.get(
        f"{settings.web_api_url}/api/internal/jobs/{job_id}/images",
        params={"kind": kind},
        headers=_headers(),
        timeout=30,
    )
    r.raise_for_status()
    return r.json().get("images", [])


def post_recon_result(
    job_id: str,
    *,
    planes: list[dict],
    obstacles: list[dict],
    waypoints: list[dict],
) -> None:
    r = httpx.post(
        f"{settings.web_api_url}/api/internal/jobs/{job_id}/recon-result",
        json={"planes": planes, "obstacles": obstacles, "waypoints": waypoints},
        headers=_headers(),
        timeout=60,
    )
    r.raise_for_status()


def post_mission_result(
    job_id: str,
    *,
    orthomosaic_url: str | None,
    damages: list[dict],
    report: dict,
) -> None:
    r = httpx.post(
        f"{settings.web_api_url}/api/internal/jobs/{job_id}/mission-result",
        json={
            "orthomosaicUrl": orthomosaic_url,
            "damages": damages,
            "report": report,
        },
        headers=_headers(),
        timeout=120,
    )
    r.raise_for_status()


def download_image(url: str) -> bytes:
    r = httpx.get(url, timeout=60)
    r.raise_for_status()
    return r.content
