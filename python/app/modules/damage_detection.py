"""Detect roof damage in mission images.

For the first iteration this calls Claude's vision model on each image and
parses a structured JSON response. Once enough labeled imagery has been
collected through the platform, swap this for a fine-tuned detection model
(e.g. YOLO or Mask R-CNN) trained on the captured set.
"""

from __future__ import annotations

import base64
import json
import logging

import httpx

from app.config import settings

log = logging.getLogger(__name__)

PROMPT = """You are a roofing inspector AI. Examine this aerial image of a residential roof
and identify any damage. Return ONLY a JSON array — no prose. Each element:
{
  "type": one of [missing_shingle, cracked_shingle, curling, hail_impact,
                  granule_loss, flashing_damage, debris, moss, punctured, other],
  "severity": integer 1-5,
  "confidence": float 0-1,
  "bbox": [x, y, w, h] in pixels (top-left origin), or null if unclear,
  "notes": short human-readable description
}
If no damage is visible, return []."""


def _call_claude(image_bytes: bytes) -> list[dict]:
    if not settings.anthropic_api_key:
        log.warning("ANTHROPIC_API_KEY missing; skipping damage detection")
        return []

    b64 = base64.standard_b64encode(image_bytes).decode("ascii")
    body = {
        "model": "claude-opus-4-7",
        "max_tokens": 1024,
        "messages": [
            {
                "role": "user",
                "content": [
                    {
                        "type": "image",
                        "source": {
                            "type": "base64",
                            "media_type": "image/jpeg",
                            "data": b64,
                        },
                    },
                    {"type": "text", "text": PROMPT},
                ],
            }
        ],
    }
    r = httpx.post(
        "https://api.anthropic.com/v1/messages",
        json=body,
        headers={
            "x-api-key": settings.anthropic_api_key,
            "anthropic-version": "2023-06-01",
            "content-type": "application/json",
        },
        timeout=120,
    )
    r.raise_for_status()
    text = r.json()["content"][0]["text"].strip()
    # The model occasionally wraps the JSON in code fences; strip them defensively.
    if text.startswith("```"):
        text = text.strip("`")
        if text.lower().startswith("json"):
            text = text[4:]
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        log.warning("damage detection: non-JSON response: %s", text[:200])
        return []


def detect(images: list[dict]) -> list[dict]:
    out: list[dict] = []
    for img in images:
        try:
            blob = httpx.get(img["blobUrl"], timeout=60).content
            findings = _call_claude(blob)
        except Exception:
            log.exception("damage detection failed on image %s", img.get("id"))
            findings = []
        for f in findings:
            out.append(
                {
                    "imageId": img["id"],
                    "planeId": None,
                    "type": f.get("type", "other"),
                    "severity": int(f.get("severity", 1)),
                    "confidence": float(f.get("confidence", 0)) or None,
                    "bbox": f.get("bbox"),
                    "geo": None,
                    "notes": f.get("notes"),
                }
            )
    return out
