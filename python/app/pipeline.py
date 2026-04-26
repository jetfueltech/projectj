"""Top-level orchestration for the recon and mission processing stages."""

from __future__ import annotations

import logging

from app import web_client
from app.modules import damage_detection, orthomosaic, plane_detection, report, waypoints

log = logging.getLogger(__name__)


def process_recon(job_id: str) -> None:
    """Steps 5 + 6: detect roof planes / obstacles, then plan a waypoint grid."""
    try:
        log.info("recon: job=%s fetching images", job_id)
        images = web_client.fetch_images(job_id, kind="recon")
        if not images:
            raise RuntimeError("no recon images uploaded")

        planes, obstacles = plane_detection.detect(images)
        log.info("recon: job=%s planes=%d obstacles=%d", job_id, len(planes), len(obstacles))

        wps = waypoints.plan_lawnmower(
            planes=planes,
            obstacles=obstacles,
            altitude_m=25.0,
            forward_overlap=0.8,
            side_overlap=0.7,
            gimbal_pitch_deg=-90.0,
        )
        log.info("recon: job=%s waypoints=%d", job_id, len(wps))

        web_client.post_recon_result(
            job_id,
            planes=planes,
            obstacles=obstacles,
            waypoints=wps,
        )
    except Exception:
        log.exception("recon failed for job %s", job_id)
        raise


def process_mission(job_id: str) -> None:
    """Steps 9 + 10 + 11: stitch orthomosaic, detect damage, generate report."""
    try:
        log.info("mission: job=%s fetching images", job_id)
        images = web_client.fetch_images(job_id, kind="mission")
        if not images:
            raise RuntimeError("no mission images uploaded")

        ortho_url = orthomosaic.stitch(job_id, images)
        log.info("mission: job=%s orthomosaic=%s", job_id, ortho_url)

        damages = damage_detection.detect(images)
        log.info("mission: job=%s damages=%d", job_id, len(damages))

        report_payload = report.generate(job_id, damages, ortho_url)

        web_client.post_mission_result(
            job_id,
            orthomosaic_url=ortho_url,
            damages=damages,
            report=report_payload,
        )
    except Exception:
        log.exception("mission failed for job %s", job_id)
        raise
