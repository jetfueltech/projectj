"""Generate the inspection report.

For now this returns a JSON summary plus a placeholder PDF URL. A full
implementation should:
  1. Render a multi-page PDF (ReportLab) with: cover page, property info,
     orthomosaic with annotated damage polygons, per-damage thumbnail grid,
     totals + repair-cost estimate.
  2. Upload the PDF to Vercel Blob via the upload endpoint and return its URL.
"""

from __future__ import annotations

from collections import Counter


def generate(job_id: str, damages: list[dict], orthomosaic_url: str | None) -> dict:
    by_type = Counter(d["type"] for d in damages)
    by_severity = Counter(d["severity"] for d in damages)
    avg_severity = (
        sum(d["severity"] for d in damages) / len(damages) if damages else 0
    )

    summary_lines = [f"Inspection {job_id} — automated summary."]
    if not damages:
        summary_lines.append("No damage detected from the captured imagery.")
    else:
        summary_lines.append(
            f"Detected {len(damages)} potential issues across the roof "
            f"(avg severity {avg_severity:.1f}/5):"
        )
        for kind, count in by_type.most_common():
            summary_lines.append(f"  • {kind.replace('_', ' ')}: {count}")

    return {
        "pdfUrl": None,  # TODO: render + upload PDF
        "summary": "\n".join(summary_lines),
        "totals": {
            "damages": len(damages),
            "byType": dict(by_type),
            "bySeverity": {str(k): v for k, v in by_severity.items()},
            "avgSeverity": avg_severity,
            "orthomosaicUrl": orthomosaic_url,
        },
    }
