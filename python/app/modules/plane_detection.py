"""Detect roof planes and obstacles from a set of low-altitude recon images.

Real implementation strategy (for a future iteration):
  1. Run Structure-from-Motion on the recon set (e.g. OpenSfM or COLMAP) to recover
     a geo-referenced point cloud + per-image camera poses.
  2. Segment the point cloud above ground level.
  3. RANSAC plane fitting on the building cluster -> roof plane polygons in WGS84.
  4. Anything tall and not roof-shaped (trees, antennas, chimneys) becomes an obstacle.

This module currently returns a simple bounding-box-derived planar approximation
from the GPS bounds of the recon images so the rest of the pipeline can run
end-to-end. Replace `detect` with the real implementation.
"""

from __future__ import annotations

from shapely.geometry import Polygon, mapping


def detect(images: list[dict]) -> tuple[list[dict], list[dict]]:
    coords = [
        (img["longitude"], img["latitude"])
        for img in images
        if img.get("latitude") is not None and img.get("longitude") is not None
    ]
    if len(coords) < 4:
        return [], []

    lats = [lat for _, lat in coords]
    lngs = [lng for lng, _ in coords]
    # Shrink the bounding box by ~30% so we don't include the recon orbit area.
    lat_pad = (max(lats) - min(lats)) * 0.35
    lng_pad = (max(lngs) - min(lngs)) * 0.35

    poly = Polygon(
        [
            (min(lngs) + lng_pad, min(lats) + lat_pad),
            (max(lngs) - lng_pad, min(lats) + lat_pad),
            (max(lngs) - lng_pad, max(lats) - lat_pad),
            (min(lngs) + lng_pad, max(lats) - lat_pad),
        ]
    )

    plane = {
        "polygon": mapping(poly),
        "areaSqFt": _approx_area_sqft(poly),
        "pitchDegrees": 25.0,
        "azimuthDegrees": 180.0,
        "ridgeHeightM": 7.0,
        "eaveHeightM": 4.0,
    }
    return [plane], []


def _approx_area_sqft(poly: Polygon) -> float:
    # Equirectangular-ish approximation: fine for the small extents we work in.
    centroid_lat = poly.centroid.y
    import math

    m_per_deg_lat = 111_320
    m_per_deg_lng = 111_320 * math.cos(math.radians(centroid_lat))
    lng_extent = (poly.bounds[2] - poly.bounds[0]) * m_per_deg_lng
    lat_extent = (poly.bounds[3] - poly.bounds[1]) * m_per_deg_lat
    area_m2 = lng_extent * lat_extent
    return area_m2 * 10.7639
