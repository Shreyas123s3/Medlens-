#!/usr/bin/env python3
"""Fetch verified OTC drug labels from OpenFDA and export MedLens assets.

Run from the repository root: python tools/import_medicines.py
The endpoint and source field names follow https://open.fda.gov/apis/drug/label/.
No brand names are exported: the source labels are US product labels and are
not evidence that a brand is sold in India.
"""

from __future__ import annotations

import argparse
import datetime as dt
import html
from html.parser import HTMLParser
import json
import os
from pathlib import Path
import re
import sys
import time
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
ENDPOINT = "https://api.fda.gov/drug/label.json"
MANIFEST = ROOT / "tools" / "medicine_manifest.json"
INTERNATIONAL_SYNONYMS = ROOT / "tools" / "international_synonyms.json"
ASSET_DIR = ROOT / "app" / "src" / "main" / "assets"
OUTPUT = ASSET_DIR / "medicines.json"
SYNONYM_OUTPUT = ASSET_DIR / "medicine_synonyms.json"
REPORT = ROOT / "MEDICINE_IMPORT_REPORT.md"
SOURCE_NAME = "U.S. FDA OpenFDA Drug Product Labeling"

FIELD_MAP = {
    "uses": ("indications_and_usage", "purpose"),
    "howSupplied": ("how_supplied",),
    "storage": ("storage_and_handling",),
    "warnings": ("boxed_warning", "warnings", "warnings_and_precautions"),
    "contraindications": ("contraindications",),
    "sideEffects": ("adverse_reactions",),
    "interactions": ("drug_interactions", "drug_and_or_laboratory_test_interactions"),
    "mechanism": ("mechanism_of_action",),
    "precautions": ("precautions", "general_precautions", "ask_doctor", "ask_doctor_or_pharmacist"),
    "whenToSeeDoctor": ("stop_use", "ask_doctor", "ask_doctor_or_pharmacist"),
}
UNAVAILABLE = tuple(FIELD_MAP)

DOSE_OR_DURATION = re.compile(
    r"(?:\b\d+(?:\.\d+)?\s*(?:mg|mcg|µg|μg|g|gram|grams|ml|mL|cc|unit|units|teaspoons?|tablespoons?|tsp|tbsp|%)\b"
    r"|\b\d+\s*(?:tablets?|caplets?|capsules?|sprays?|drops?|applications?)\b"
    r"|\b(?:once|twice|three times|four times)\s+(?:a|per)\s+day\b"
    r"|\b(?:once|twice|three times|four times)\s+daily\b"
    r"|\b\d+\s+times\s+(?:a|per)\s+day\b"
    r"|\bevery\s+\d+\s+(?:hours?|days?|weeks?|months?)\b"
    r"|\b(?:for|within|after|over|under|more than|longer than|at least|no more than|up to)\s+\d+\s+(?:hours?|days?|weeks?|months?)\b"
    r"|\b(?:lasts?|persists?|continues?|occurs?)\s+(?:for\s+)?(?:more than|longer than|at least|over)\s+\d+\s+(?:hours?|days?|weeks?|months?)\b"
    r"|\b(?:dosage|dose|directions for use|directions)\b"
    r"|\btake\s+(?:one|two|three|\d+)\b"
    r"|\b(?:daily|twice daily|once daily)\b)",
    re.IGNORECASE,
)
BLOCK_HEADINGS = re.compile(
    r"^(?:dosage and administration|dosage forms and strengths|directions|how to use|dosing information)\b",
    re.IGNORECASE,
)
SENTENCE_SPLIT = re.compile(r"(?<=[.!?])\s+(?=[A-Z0-9\[])" )


class _TextExtractor(HTMLParser):
    BLOCK_TAGS = {"p", "div", "li", "br", "tr", "h1", "h2", "h3", "h4", "table"}

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.parts: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if tag.lower() in self.BLOCK_TAGS:
            self.parts.append("\n")

    def handle_endtag(self, tag: str) -> None:
        if tag.lower() in self.BLOCK_TAGS:
            self.parts.append("\n")

    def handle_data(self, data: str) -> None:
        self.parts.append(data)


def clean_label_text(value: Any) -> list[str]:
    """Return source-verbatim bullets after removing dosage/duration material."""
    if value is None:
        return []
    values = value if isinstance(value, list) else [value]
    output: list[str] = []
    seen: set[str] = set()
    for raw in values:
        if not isinstance(raw, str):
            continue
        parser = _TextExtractor()
        parser.feed(html.unescape(raw))
        text = "".join(parser.parts).replace("\xa0", " ")
        for block in re.split(r"[\r\n]+", text):
            block = re.sub(r"\s+", " ", block).strip(" \t•*-\u2022")
            if not block or BLOCK_HEADINGS.search(block):
                continue
            fragments = [fragment for fragment in re.split(r"[•▪◦]\s*", block) if fragment.strip()]
            for fragment in fragments:
                for sentence in SENTENCE_SPLIT.split(fragment):
                    sentence = re.sub(r"\s+", " ", sentence).strip(" \t•*-\u2022")
                    if not sentence or DOSE_OR_DURATION.search(sentence):
                        continue
                    key = sentence.casefold()
                    if key not in seen:
                        seen.add(key)
                        output.append(sentence)
    return output


def _first_text(record: dict[str, Any], fields: tuple[str, ...]) -> list[str]:
    for field in fields:
        clean = clean_label_text(record.get(field))
        if clean:
            return clean
    return []


def parse_label(record: dict[str, Any], candidate: dict[str, Any], retrieved_at: str, query_url: str) -> dict[str, Any]:
    openfda = record.get("openfda") if isinstance(record.get("openfda"), dict) else {}
    product_types = [str(x).strip().upper() for x in openfda.get("product_type", [])]
    if "HUMAN OTC DRUG" not in product_types:
        raise ValueError("Only records explicitly marked HUMAN OTC DRUG can enter the offline OTC catalog")

    sections: dict[str, list[str]] = {key: _first_text(record, fields) for key, fields in FIELD_MAP.items()}
    unavailable = [key for key, lines in sections.items() if not lines]
    aliases = candidate.get("synonyms", [])
    alias_string = "|".join(dict.fromkeys(str(x).strip() for x in aliases if str(x).strip()))
    return {
        "genericName": candidate["genericName"],
        "category": candidate["category"],
        "brands": "",
        "brandAvailability": "Information unavailable",
        "synonyms": alias_string,
        "otcStatus": "OTC",
        **{key: "\n".join(lines) for key, lines in sections.items()},
        "source": SOURCE_NAME,
        "retrievedAt": retrieved_at,
        "sourceRecordId": str(record.get("set_id", "")),
        "sourceEffectiveDate": str(record.get("effective_time", "")),
        "sourceUrl": query_url,
        "productType": "HUMAN OTC DRUG",
        "unavailableFields": ",".join(unavailable),
    }


def _request_json(url: str, timeout: float = 20.0, attempts: int = 4) -> dict[str, Any]:
    headers = {"User-Agent": "MedLensOpenFDAImport/1.0 (educational; contact: project maintainer)"}
    api_key = os.getenv("OPENFDA_API_KEY")
    if api_key:
        # The key is sent only to openFDA and is never printed or stored in assets.
        params = url.split("?", 1)
        query = params[1] + "&" + urlencode({"api_key": api_key}) if len(params) > 1 else urlencode({"api_key": api_key})
        url = params[0] + "?" + query
    for attempt in range(attempts):
        try:
            request = Request(url, headers=headers)
            with urlopen(request, timeout=timeout) as response:
                return json.loads(response.read().decode("utf-8"))
        except HTTPError as exc:
            if exc.code == 404:
                return {"results": []}
            if exc.code < 500 and exc.code != 429:
                raise RuntimeError(f"OpenFDA returned HTTP {exc.code}") from None
            if attempt == attempts - 1:
                raise RuntimeError(f"OpenFDA returned HTTP {exc.code} after retries") from None
        except (URLError, TimeoutError, OSError) as exc:
            if attempt == attempts - 1:
                raise RuntimeError(f"OpenFDA request failed after retries: {type(exc).__name__}") from None
        time.sleep(0.5 * (2**attempt))
    return {"results": []}


def _label_score(record: dict[str, Any]) -> tuple[int, str]:
    section_names = set(record)
    score = sum(1 for field in ("indications_and_usage", "purpose", "warnings", "contraindications", "adverse_reactions", "drug_interactions", "mechanism_of_action", "storage_and_handling", "how_supplied") if field in section_names)
    return score, str(record.get("effective_time", ""))


def _route_matches(record: dict[str, Any], candidate: dict[str, Any]) -> bool:
    expected = candidate.get("expectedRoute")
    routes = record.get("openfda", {}).get("route", []) or []
    if not expected or not routes:
        return True
    return any(expected in str(route).upper() for route in routes)


def _single_ingredient_matches(record: dict[str, Any], candidate: dict[str, Any], all_candidates: list[dict[str, Any]] | None = None) -> bool:
    """Reject combination labels so a row never mixes two medicines' facts."""
    generics = record.get("openfda", {}).get("generic_name", []) or []
    if len(generics) != 1:
        return False
    generic = re.sub(r"[^a-z0-9]+", " ", str(generics[0]).casefold()).strip()
    alternatives = [candidate.get("searchTerm", candidate["genericName"]), candidate["genericName"], *candidate.get("synonyms", [])]
    ignored_formulations = {"topical", "nasal", "oral", "ophthalmic", "eye", "drops", "gel", "cream", "spray", "fumarate", "hydrochloride", "sodium", "citrate", "acid"}
    matched = False
    for alternative in alternatives:
        tokens = [token for token in re.sub(r"[^a-z0-9]+", " ", str(alternative).casefold()).split() if token not in ignored_formulations]
        if tokens and all(re.search(rf"\b{re.escape(token)}\b", generic) for token in tokens):
            matched = True
            break
    if not matched:
        return False

    # Some combination labels are indexed under only one generic name. Check
    # the active-ingredient field against other manifest entries as a second
    # guard (for example, ibuprofen plus diphenhydramine sleep-aid labels).
    active = " ".join(str(value) for value in (record.get("active_ingredient") or []))
    normalized_active = re.sub(r"[^a-z0-9]+", " ", active.casefold())
    for other in all_candidates or []:
        if other["genericName"].casefold() == candidate["genericName"].casefold():
            continue
        other_names = [other["genericName"], other.get("searchTerm", ""), *other.get("synonyms", [])]
        for name in other_names:
            tokens = [token for token in re.sub(r"[^a-z0-9]+", " ", str(name).casefold()).split() if token not in ignored_formulations]
            if tokens and all(re.search(rf"\b{re.escape(token)}\b", normalized_active) for token in tokens):
                return False
    return True


def fetch_candidate(candidate: dict[str, Any], all_candidates: list[dict[str, Any]] | None = None) -> tuple[dict[str, Any] | None, str]:
    search_term = candidate.get("searchTerm", candidate["genericName"])
    queries = [
        f'openfda.generic_name:"{search_term}" AND openfda.product_type:"HUMAN OTC DRUG"',
        f'active_ingredient:"{search_term}" AND openfda.product_type:"HUMAN OTC DRUG"',
    ]
    for query in queries:
        # Keep payloads small: the parser only needs a few label candidates to
        # select a well-populated, route-compatible OTC record.
        url = ENDPOINT + "?" + urlencode({"search": query, "limit": 10})
        response = _request_json(url)
        records = response.get("results", [])
        eligible = [r for r in records if "HUMAN OTC DRUG" in [str(x).upper() for x in (r.get("openfda", {}).get("product_type", []) or [])]]
        eligible = [r for r in eligible if _single_ingredient_matches(r, candidate, all_candidates)]
        route_filtered = [r for r in eligible if _route_matches(r, candidate)]
        if route_filtered:
            eligible = route_filtered
        if eligible:
            record = max(eligible, key=_label_score)
            # Delay requests to remain comfortably below public per-IP limits.
            time.sleep(0.12)
            return record, url
    return None, ENDPOINT + "?" + urlencode({"search": queries[-1], "limit": 10})


def build_synonym_map(candidates: list[dict[str, Any]], global_map: dict[str, str]) -> dict[str, str]:
    result = {str(key).casefold().strip(): str(value).casefold().strip() for key, value in global_map.items()}
    for item in candidates:
        canonical = str(item["genericName"]).casefold().strip()
        result[canonical] = canonical
        for alias in item.get("synonyms", []):
            result[str(alias).casefold().strip()] = canonical
    return result


def render_report(medicines: list[dict[str, Any]], missing: list[dict[str, str]]) -> str:
    lines = [
        "# OpenFDA medicine import report",
        "",
        f"Imported {len(medicines)} OTC records from the official OpenFDA drug-label API. Retrieval date is stored on each medicine record.",
        "",
        "Brand names were not populated: US label product names do not verify Indian market brand availability.",
        "",
        "| Medicine | Category | Flag | Source | Information unavailable |",
        "|---|---|---|---|---|",
    ]
    for medicine in medicines:
        unavailable = medicine["unavailableFields"]
        fields = ", ".join(unavailable.split(",")) if isinstance(unavailable, str) and unavailable else ", ".join(unavailable) if unavailable else "None"
        lines.append(f"| {medicine['genericName']} | {medicine['category']} | {medicine['otcStatus']} | {medicine['source']} ({medicine['sourceRecordId']}) | {fields} |")
    if missing:
        lines.extend(["", "## Candidates with no verified OpenFDA OTC label", "", "| Candidate | Category |", "|---|---|"])
        lines.extend(f"| {item['genericName']} | {item['category']} |" for item in missing)
    lines.append("")
    return "\n".join(lines)


def import_dataset(allow_partial: bool = False) -> tuple[list[dict[str, Any]], list[dict[str, str]]]:
    candidates: list[dict[str, Any]] = json.loads(MANIFEST.read_text(encoding="utf-8"))
    global_map: dict[str, str] = json.loads(INTERNATIONAL_SYNONYMS.read_text(encoding="utf-8"))
    retrieved_at = dt.datetime.now(dt.timezone.utc).date().isoformat()
    imported: list[dict[str, Any]] = []
    missing: list[dict[str, str]] = []
    for index, candidate in enumerate(candidates, start=1):
        try:
            record, query_url = fetch_candidate(candidate, candidates)
            if record is None:
                missing.append({"genericName": candidate["genericName"], "category": candidate["category"]})
                print(f"[{index}/{len(candidates)}] No verified OTC label: {candidate['genericName']}", file=sys.stderr)
                continue
            medicine = parse_label(record, candidate, retrieved_at, query_url)
            imported.append(medicine)
            print(f"[{index}/{len(candidates)}] Imported OTC label: {candidate['genericName']}", file=sys.stderr)
        except Exception as exc:
            missing.append({"genericName": candidate["genericName"], "category": candidate["category"]})
            print(f"[{index}/{len(candidates)}] Import failed for {candidate['genericName']}: {exc}", file=sys.stderr)

    if missing and not allow_partial:
        ASSET_DIR.mkdir(parents=True, exist_ok=True)
        REPORT.write_text(render_report(imported, missing), encoding="utf-8")
        raise RuntimeError(f"Only {len(imported)} of {len(candidates)} candidates had verified OTC labels. Review {REPORT} and rerun; partial data was not installed.")

    ASSET_DIR.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(imported, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    SYNONYM_OUTPUT.write_text(json.dumps(build_synonym_map(candidates, global_map), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    REPORT.write_text(render_report(imported, missing), encoding="utf-8")
    return imported, missing


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--allow-partial", action="store_true", help="write only medicines with verified OTC labels")
    args = parser.parse_args()
    try:
        medicines, missing = import_dataset(args.allow_partial)
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        return 1
    print(f"Wrote {len(medicines)} medicine rows to {OUTPUT}")
    print(f"Wrote source/unavailable-field report to {REPORT}")
    if missing:
        print(f"Not imported: {len(missing)} (see report)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
