#!/usr/bin/env python3
"""Create a new allowlisted public working copy. Never edits app/, overwrites or publishes."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import zipfile
from public_audit import audit


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--name", default="public", help="Nueva carpeta dentro de distribution; no sobrescribe")
    args = parser.parse_args()
    if not re.fullmatch(r"[A-Za-z0-9_-]+", args.name):
        parser.error("Nombre simple requerido.")
    root = Path(__file__).resolve().parents[1]
    dest = root / "distribution" / args.name
    if dest.exists():
        parser.error("Ya existe esa copia: revisarla o elegir otro --name. No se elimina ni sobrescribe.")
    source_plate = root / "app/src/main/java/com/fytsmart/dashboard/VehiclePlateConfig.java"
    match = re.search(r'DEFAULT = "([^"]+)"', source_plate.read_text(encoding="utf-8"))
    if not match:
        parser.error("No se identificó la matrícula inicial; detener y revisar.")
    original_value = match[1]
    selected = [root / name for name in ["README.md", "AGENTS.md", "CHANGELOG.md", "LICENSE_PROPOSAL.md", "LICENSE", "NOTICE", "THIRD_PARTY_NOTICES.md", ".gitignore",
                 "build.gradle", "settings.gradle", "gradle.properties", "gradlew", "gradlew.bat", "app/build.gradle"]]
    allowed = {"app/src": {".java", ".xml", ".png", ".webp"},
               "app/src/main/assets/legal": {".txt", ".md"}, "gradle/wrapper": {".jar", ".properties"},
               "tools": {".py", ".sh", ".ps1"}, "tests": {".java", ".py"}, "docs/reusable": {".md", ".png"},
               "prompts": {".md"}, "schemas": {".json"}, "profiles/examples": {".json"}, "examples": {".json"}}
    for folder, suffixes in allowed.items():
        selected += [p for p in (root / folder).rglob("*") if p.is_file() and p.suffix in suffixes
                     and not any(part.startswith(".") or part == "__pycache__" for part in p.relative_to(root).parts)]
    selected = sorted(set(selected))
    if any(not p.is_file() or p.is_symlink() for p in selected):
        parser.error("Un archivo requerido falta o es un enlace simbólico; revisar antes de exportar.")
    dest.mkdir(parents=True)
    changed = []
    for source in selected:
        relative = source.relative_to(root)
        target = dest / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, target)
        if source.suffix in (".java", ".py", ".md", ".json", ".xml", ".gradle", ".properties"):
            text = target.read_text(encoding="utf-8")
            sanitized = text.replace(original_value, "TU-PLACA")
            if text != sanitized:
                target.write_text(sanitized, encoding="utf-8")
                changed.append(relative.as_posix())
    # Export only this reviewed public tree; the brand-neutral vehicle is drawn in Java, not imported from private assets.
    ignore = dest / ".gitignore"
    ignore.write_text(ignore.read_text().replace("assets/\n", ""), encoding="utf-8")
    licenses = dest / "licenses"
    licenses.mkdir()
    with zipfile.ZipFile(root / "gradle/wrapper/gradle-wrapper.jar") as jar:
        (licenses / "GRADLE_WRAPPER_LICENSE.txt").write_bytes(jar.read("META-INF/LICENSE"))
    private_values = [original_value] if original_value != "TU-PLACA" else []
    report = audit(dest, private_values)
    evidence = root / "artifacts/reusable-platform"
    evidence.mkdir(parents=True, exist_ok=True)
    (evidence / (args.name + "-audit.json")).write_text(json.dumps(report, indent=2) + "\n")
    if report["status"] != "PASS":
        print("Copia creada para revisión, NO lista para distribución. Ver reporte privado; no se eliminó el original.")
        return 1
    manifest = [{"path": p.relative_to(dest).as_posix(), "sha256": hashlib.sha256(p.read_bytes()).hexdigest()}
                for p in sorted(dest.rglob("*")) if p.is_file()]
    (evidence / (args.name + "-manifest.json")).write_text(json.dumps({"anonymized_files": changed, "files": manifest}, indent=2) + "\n")
    print("Copia limpia creada en distribution/" + args.name + "; app original intacta. No Git remoto, publicación, firma o instalación.")
    print("Avisos de licencia conservados. Firma pública, atribución del clima y pruebas de destino pendientes; no publicar automáticamente.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
