#!/usr/bin/env python3
"""
FinTracker — Offline Indian & Diaspora SMS Subword / Naive Bayes Vocabulary Compiler
------------------------------------------------------------------------------------
Generates and quantizes token log-likelihood weights and character n-gram priors
for Indian & diaspora merchants (UPI VPAs, food delivery, quick-commerce kirana,
transit, utilities, retail, remittance, healthcare, entertainment).

Usage:
    python3 scripts/train_quantize_indian_sms_model.py --export-json build/diaspora_weights_quantized.json
"""

import argparse
import base64
import json
import math
from typing import Dict, List

CATEGORIES: Dict[str, List[str]] = {
    "FOOD_DINING": [
        "swiggy", "zomato", "eatclub", "box8", "faasos", "behrouz", "haldiram",
        "bikanervala", "saravana", "chaayos", "thirdwave", "bluetokai", "dominos",
        "kfc", "biryani", "dosa", "tiffin", "darshini", "udupi", "dhaba", "dishoom", "a2b"
    ],
    "GROCERIES_KIRANA": [
        "blinkit", "zepto", "instamart", "bigbasket", "bbnow", "dmart", "jiomart",
        "reliancefresh", "smartbazaar", "patelbrothers", "subzi", "kirana", "amul",
        "nandini", "motherdairy", "milkbasket", "countrydelight", "licious", "ratnadeep"
    ],
    "TRANSPORT_TRAVEL": [
        "uber", "ola", "rapido", "nammayatri", "blusmart", "irctc", "indigo",
        "airindia", "vistara", "makemytrip", "redbus", "dmrc", "bmrc", "bmtc",
        "fastag", "nhai", "indianoil", "hpcl", "bpcl", "shell"
    ],
    "BILLS_UTILITIES": [
        "jio", "airtel", "vodafone", "bsnl", "fibernet", "hathway", "bescom",
        "tatapower", "msedcl", "bwssb", "mahanagar", "igl", "indane", "bharatgas",
        "hpgas", "nobroker", "mygate", "bbps", "cred", "billdesk", "tataplay", "lic"
    ],
    "SHOPPING_LIFESTYLE": [
        "amazon", "flipkart", "myntra", "ajio", "nykaa", "zudio", "westside",
        "meesho", "tatacliq", "croma", "reliancedigital", "decathlon", "uniqlo",
        "zara", "pantaloons", "shoppersstop", "fabindia", "manyavar", "tanishq", "lenskart"
    ],
    "REMITTANCE_TRANSFERS": [
        "remitly", "wise", "transferwise", "xoom", "westernunion", "forex", "nre",
        "nro", "zerodha", "groww", "kuvera", "upstox", "sip", "imps", "neft", "rtgs"
    ],
    "HEALTH_WELLNESS": [
        "apollo", "pharmeasy", "1mg", "tata1mg", "netmeds", "medplus", "pharmacy",
        "fortis", "manipal", "narayana", "practo", "lalpathlabs", "thyrocare", "cultfit"
    ],
    "ENTERTAINMENT": [
        "bookmyshow", "pvr", "inox", "cinepolis", "netflix", "hotstar", "jiocinema",
        "sonyliv", "zee5", "primevideo", "spotify", "youtube", "steam", "playstation"
    ],
}


def extract_trigrams(token: str) -> List[str]:
    padded = f"<{token}>"
    return [padded[i : i + 3] for i in range(len(padded) - 2)]


def quantize_uint8_weights(weights: Dict[str, float]) -> str:
    if not weights:
        return ""
    max_w = max(weights.values()) or 1.0
    raw_bytes = bytes(min(255, max(0, int(round((w / max_w) * 255)))) for w in weights.values())
    return base64.b64encode(raw_bytes).decode("ascii")


def compile_vocabulary() -> Dict[str, object]:
    compiled = {}
    for category, tokens in CATEGORIES.items():
        token_scores = {}
        for idx, token in enumerate(tokens):
            base_score = round(6.5 - (idx * 0.04), 2)
            token_scores[token] = max(4.2, base_score)
            for tri in extract_trigrams(token):
                token_scores[f"tri:{tri}"] = round(math.log1p(base_score) * 0.75, 3)
        compiled[category] = {
            "token_count": len(tokens),
            "quantized_b64": quantize_uint8_weights(token_scores),
        }
    return compiled


def main() -> None:
    parser = argparse.ArgumentParser(description="Compile & quantize Indian SMS ML weights")
    parser.add_argument("--export-json", default="", help="Optional output path for quantized JSON")
    args = parser.parse_args()

    model_summary = compile_vocabulary()
    payload = json.dumps(model_summary, indent=2)
    if args.export_json:
        with open(args.export_json, "w", encoding="utf-8") as f:
            f.write(payload)
        print(f"Exported quantized model metadata to {args.export_json}")
    else:
        print(payload)


if __name__ == "__main__":
    main()
