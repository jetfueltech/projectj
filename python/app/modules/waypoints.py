"""Generate a roof-specific waypoint grid (lawnmower / boustrophedon pattern).

Strategy:
  * Build the union of all roof-plane polygons in a local equirectangular frame
    centered on the roof centroid (so we can reason in meters).
  * Compute the ground-sample-distance from altitude + assumed sensor params
    to get the photo footprint, then pick along-track + cross-track spacing
    from the requested overlaps.
  * Sweep parallel scan lines over the polygon along the roof's principal axis,
    flipping direction every other line. Drop a waypoint at each line endpoint
    (and roughly every photo step in-between when needed).
  * Avoid obstacles by clipping any segment that intersects an obstacle buffer.
"""

from __future__ import annotations

import math
from typing import Iterable

from shapely.affinity import rotate, translate
from shapely.geometry import LineString, MultiPolygon, Point, Polygon, shape
from shapely.ops import unary_union

# Mavic 3 Enterprise wide camera defaults; tweak per drone model.
SENSOR_WIDTH_MM = 17.3
FOCAL_LENGTH_MM = 12.29
IMAGE_WIDTH_PX = 5280


def _gsd_m(altitude_m: float) -> float:
    return (SENSOR_WIDTH_MM * altitude_m) / (FOCAL_LENGTH_MM * IMAGE_WIDTH_PX)


def _footprint_m(altitude_m: float) -> tuple[float, float]:
    gsd = _gsd_m(altitude_m)
    width = gsd * IMAGE_WIDTH_PX
    height = width * 3 / 4  # assume 4:3 sensor
    return width, height


def _to_local(poly: Polygon, lat0: float, lng0: float) -> Polygon:
    m_per_deg_lat = 111_320
    m_per_deg_lng = 111_320 * math.cos(math.radians(lat0))
    return Polygon(
        [
            ((x - lng0) * m_per_deg_lng, (y - lat0) * m_per_deg_lat)
            for x, y in poly.exterior.coords
        ]
    )


def _to_wgs84(x: float, y: float, lat0: float, lng0: float) -> tuple[float, float]:
    m_per_deg_lat = 111_320
    m_per_deg_lng = 111_320 * math.cos(math.radians(lat0))
    return (lng0 + x / m_per_deg_lng, lat0 + y / m_per_deg_lat)


def _principal_angle_deg(poly: Polygon) -> float:
    """Angle of the longest edge of the minimum rotated rectangle (degrees)."""
    rect = poly.minimum_rotated_rectangle
    coords = list(rect.exterior.coords)
    edges = [(coords[i], coords[i + 1]) for i in range(len(coords) - 1)]
    longest = max(edges, key=lambda e: math.dist(e[0], e[1]))
    dx = longest[1][0] - longest[0][0]
    dy = longest[1][1] - longest[0][1]
    return math.degrees(math.atan2(dy, dx))


def plan_lawnmower(
    *,
    planes: list[dict],
    obstacles: list[dict],
    altitude_m: float,
    forward_overlap: float,
    side_overlap: float,
    gimbal_pitch_deg: float,
) -> list[dict]:
    if not planes:
        return []

    polys = [shape(p["polygon"]) for p in planes]
    union = unary_union(polys)
    if isinstance(union, Polygon):
        union = MultiPolygon([union])

    centroid = union.centroid
    lat0, lng0 = centroid.y, centroid.x
    local_polys = [_to_local(p, lat0, lng0) for p in polys]
    local_union = unary_union(local_polys)
    if isinstance(local_union, Polygon):
        local_union = MultiPolygon([local_union])

    obstacle_geoms = [
        shape(o["geometry"]).buffer(2.0)
        for o in obstacles
        if o.get("geometry") is not None
    ]

    angle = _principal_angle_deg(local_union.geoms[0])
    rotated = rotate(local_union, -angle, origin=(0, 0), use_radians=False)
    minx, miny, maxx, maxy = rotated.bounds

    fp_w, fp_h = _footprint_m(altitude_m)
    side_step = fp_w * (1.0 - side_overlap)
    forward_step = fp_h * (1.0 - forward_overlap)

    waypoints: list[dict] = []
    ordering = 0
    y = miny
    forward = True
    while y <= maxy:
        line = LineString([(minx - 5, y), (maxx + 5, y)])
        clipped = line.intersection(rotated)
        if not clipped.is_empty:
            segments = (
                list(clipped.geoms) if hasattr(clipped, "geoms") else [clipped]
            )
            for seg in segments:
                if not isinstance(seg, LineString):
                    continue
                xs = [seg.coords[0][0], seg.coords[-1][0]]
                if not forward:
                    xs.reverse()
                # Drop a waypoint roughly every forward_step along the line.
                length = abs(xs[1] - xs[0])
                steps = max(1, int(length / max(forward_step, 1.0)))
                for s in range(steps + 1):
                    t = s / steps
                    x = xs[0] + (xs[1] - xs[0]) * t
                    pt = rotate(Point(x, y), angle, origin=(0, 0), use_radians=False)
                    if any(g.contains(pt) for g in obstacle_geoms):
                        continue
                    lng, lat = _to_wgs84(pt.x, pt.y, lat0, lng0)
                    waypoints.append(
                        {
                            "ordering": ordering,
                            "latitude": lat,
                            "longitude": lng,
                            "altitudeM": altitude_m,
                            "headingDeg": (angle + 90) % 360 if forward else (angle - 90) % 360,
                            "gimbalPitchDeg": gimbal_pitch_deg,
                            "speedMs": 4.0,
                            "action": "shoot_photo",
                        }
                    )
                    ordering += 1
        y += side_step
        forward = not forward

    return waypoints
