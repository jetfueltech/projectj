"""Stitch the mission images into a single orthomosaic.

A real implementation should hand the images + per-image GPS/IMU telemetry to
a photogrammetry pipeline like OpenDroneMap (`docker run opendronemap/odm ...`)
or NodeODM, then upload the resulting GeoTIFF to blob storage.

This stub returns the URL of the first mission image so downstream code paths
have something to render. Replace `stitch` when wiring up real ODM.
"""

from __future__ import annotations


def stitch(job_id: str, images: list[dict]) -> str | None:
    if not images:
        return None
    return images[0]["blobUrl"]
