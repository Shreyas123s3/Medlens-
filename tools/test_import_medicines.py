import json
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parent))
from import_medicines import _single_ingredient_matches, build_synonym_map, clean_label_text, parse_label


class ImporterTests(unittest.TestCase):
    def test_international_synonyms_map_to_canonical_names(self):
        with open("tools/medicine_manifest.json", encoding="utf-8") as source:
            manifest = json.load(source)
        with open("tools/international_synonyms.json", encoding="utf-8") as source:
            international = json.load(source)
        synonyms = build_synonym_map(manifest, international)
        self.assertEqual(synonyms["paracetamol"], "acetaminophen")
        self.assertEqual(synonyms["salbutamol"], "albuterol")
        self.assertEqual(synonyms["adrenaline"], "epinephrine")

    def test_label_parser_strips_dose_duration_and_keeps_source_verbatim_facts(self):
        candidate = {"genericName": "test medicine", "category": "Test", "synonyms": ["test"]}
        label = {
            "set_id": "source-id",
            "openfda": {"product_type": ["HUMAN OTC DRUG"]},
            "indications_and_usage": ["<p>For temporary relief of minor pain.</p><p>Take 2 tablets every 4 hours for 3 days.</p>"],
            "warnings": ["Stop use if an allergic reaction occurs."],
            "mechanism_of_action": [""],
        }
        parsed = parse_label(label, candidate, "2026-11-24", "https://api.fda.gov/drug/label.json")
        self.assertEqual(parsed["uses"], "For temporary relief of minor pain.")
        self.assertNotIn("2 tablets", parsed["uses"])
        self.assertEqual(parsed["warnings"], "Stop use if an allergic reaction occurs.")
        self.assertEqual(parsed["otcStatus"], "OTC")
        self.assertEqual(parsed["brands"], "")
        self.assertIn("mechanism", parsed["unavailableFields"])
        self.assertIsInstance(parsed["unavailableFields"], str)
        self.assertEqual(parsed["retrievedAt"], "2026-11-24")

    def test_text_cleaner_removes_duration_even_without_for(self):
        bullets = clean_label_text("Stop use if pain lasts more than 10 days. • Ask a doctor if rash occurs.")
        self.assertEqual(bullets, ["Ask a doctor if rash occurs."])

    def test_text_cleaner_removes_course_duration_and_package_dose_counts(self):
        bullets = clean_label_text("Do not use for more than 14 days. • Do not take more than 1 course every 4 months. • 120 sprays in a metered bottle.")
        self.assertEqual(bullets, [])

    def test_rx_label_is_not_misflagged_as_otc(self):
        with self.assertRaises(ValueError):
            parse_label({"openfda": {"product_type": ["HUMAN PRESCRIPTION DRUG"]}}, {"genericName": "x", "category": "x"}, "2026-11-24", "url")

    def test_combination_label_is_rejected_even_when_indexed_under_one_generic(self):
        candidates = [
            {"genericName": "ibuprofen", "searchTerm": "ibuprofen", "synonyms": []},
            {"genericName": "diphenhydramine", "searchTerm": "diphenhydramine", "synonyms": []},
        ]
        record = {"openfda": {"generic_name": ["Ibuprofen"], "product_type": ["HUMAN OTC DRUG"]},
                  "active_ingredient": ["Ibuprofen 200 mg; Diphenhydramine citrate 38 mg"]}
        self.assertFalse(_single_ingredient_matches(record, candidates[0], candidates))


if __name__ == "__main__":
    unittest.main()
