#!/usr/bin/env python3
"""Audit a clean public source export, not a complete machine or private working tree."""
import argparse
import ipaddress
import json
from pathlib import Path
import re
import subprocess

SKIP_DIRS = {".git", ".gradle", "build", "__pycache__", "artifacts"}
FORBIDDEN_SUFFIXES = {".jks", ".keystore", ".p12", ".pem", ".key", ".apk", ".aab", ".zip"}
PRIVATE_NAMES = {"local.properties", "keystore.properties", "signing.properties", "device-profile.json", "device-report.md", ".DS_Store"}
IPV4 = re.compile(rb"(?<![\d.])(?:\d{1,3}\.){3}\d{1,3}(?![\d.])")


def inspect_data(data, private_values):
    reasons = []
    for value in private_values:
        if value and value.encode("utf-8").lower() in data.lower():
            reasons.append("private_value_found")
            break
    if re.search(rb"-----BEGIN [A-Z ]*PRIVATE KEY-----", data):
        reasons.append("private_key_material")
    if re.search(rb"/(?:Users|home)/[A-Za-z0-9_.-]+/|[A-Za-z]:\\Users\\[^\\]+\\", data):
        reasons.append("personal_absolute_path")
    for value in IPV4.findall(data):
        try:
            addr = ipaddress.ip_address(value.decode())
            if addr.is_private and not addr.is_loopback:
                reasons.append("private_network_address")
                break
        except ValueError:
            pass
    return reasons


def audit(root, private_values=()):
    root = Path(root).resolve()
    findings = []
    files = 0
    for path in root.rglob("*"):
        rel = path.relative_to(root)
        if any(part in SKIP_DIRS for part in rel.parts):
            continue
        if path.is_symlink():
            findings.append({"path": str(rel), "reason": "symlink_review_required"})
            continue
        if not path.is_file():
            continue
        files += 1
        if path.suffix.lower() in FORBIDDEN_SUFFIXES or path.name in PRIVATE_NAMES or "private" in rel.parts:
            findings.append({"path": str(rel), "reason": "private_or_build_file"})
        for reason in inspect_data(path.read_bytes(), private_values):
            findings.append({"path": str(rel), "reason": reason})
    plate = root / "app/src/main/java/com/fytsmart/dashboard/VehiclePlateConfig.java"
    if not plate.exists() or 'DEFAULT = "TU-PLACA"' not in plate.read_text(encoding="utf-8"):
        findings.append({"path": "VehiclePlateConfig.java", "reason": "public_generic_default_required"})
    history = "not_present"
    if (root / ".git").is_dir():
        result = subprocess.run(["git", "-C", str(root), "rev-list", "--objects", "--all"], capture_output=True, text=True, timeout=30)
        if result.returncode:
            findings.append({"path": ".git", "reason": "history_scan_failed"})
        else:
            refs = subprocess.run(["git", "-C", str(root), "for-each-ref", "--format=%(objectname)"], capture_output=True, text=True, timeout=30)
            if refs.returncode:
                findings.append({"path": ".git", "reason": "reference_scan_failed"})
            objects = {line.split(" ", 1)[0] for line in (result.stdout + "\n" + refs.stdout).splitlines() if line.strip()}
            history = "empty" if not objects else "scanned"
            for oid in sorted(objects):
                if not re.fullmatch(r"[0-9a-f]{40,64}", oid):
                    continue
                kind = subprocess.run(["git", "-C", str(root), "cat-file", "-t", oid], capture_output=True, text=True, check=True).stdout.strip()
                if kind not in ("blob", "commit", "tag"):
                    continue
                data = subprocess.run(["git", "-C", str(root), "cat-file", kind, oid], capture_output=True, check=True).stdout
                for reason in inspect_data(data, private_values):
                    findings.append({"path": "git-object:" + oid, "reason": reason})
    return {"status": "PASS" if not findings else "REVIEW_REQUIRED", "source_files_scanned": files,
            "history": history, "findings": findings, "excluded_generated_directories": sorted(SKIP_DIRS),
            "scope": "Source export and reachable Git objects; review pictures visually, commit identities and license before publication."}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--private-values-file", type=Path, help="Lista JSON LOCAL de valores a excluir; nunca copiar al repositorio público")
    args = parser.parse_args()
    if not args.root.is_dir():
        parser.error("La raíz debe existir.")
    values = json.loads(args.private_values_file.read_text(encoding="utf-8")) if args.private_values_file else []
    if not isinstance(values, list) or not all(isinstance(value, str) for value in values):
        parser.error("La lista privada debe contener cadenas.")
    report = audit(args.root, values)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if report["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
