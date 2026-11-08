#!/usr/bin/env python3
"""Build only. Uses the existing wrapper, never installs, signs or uploads an APK."""
import argparse
import os
from pathlib import Path
import re
import shutil
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=["debug", "release"])
    parser.add_argument("--sdk", type=Path, help="Android SDK local; no se guarda en archivos públicos")
    parser.add_argument("--offline", action="store_true", help="Solo si todas las dependencias ya están en caché")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    java = shutil.which("java")
    if not java:
        parser.error("Instala JDK 17 y configura JAVA_HOME/PATH.")
    version = subprocess.run([java, "-version"], capture_output=True, text=True)
    if not re.search(r'version "17(?:\.|\")', version.stdout + version.stderr):
        parser.error("La base de referencia se prueba con JDK 17; selecciona esa versión en PATH.")
    sdk = args.sdk or Path(os.environ.get("ANDROID_HOME", os.environ.get("ANDROID_SDK_ROOT", "")))
    if not (sdk / "platforms/android-35/android.jar").is_file():
        parser.error("Indica --sdk o ANDROID_HOME con Android SDK Platform 35 instalada.")
    env = os.environ.copy()
    env["ANDROID_HOME"] = str(sdk.resolve())
    env["ANDROID_SDK_ROOT"] = str(sdk.resolve())
    task = "assembleDebug" if args.mode == "debug" else "assembleRelease"
    # Calling the wrapper JAR on Windows avoids shell/batch quoting of paths containing metacharacters.
    command = ([java, "-cp", str(root / "gradle/wrapper/gradle-wrapper.jar"),
                "org.gradle.wrapper.GradleWrapperMain"] if os.name == "nt" else [str(root / "gradlew")])
    command += ["--no-daemon", "--console=plain"] + (["--offline"] if args.offline else []) + [task]
    result = subprocess.run(command, cwd=root, env=env)
    if result.returncode == 0:
        print("Compilación terminada. No se instaló nada. APK en app/build/outputs/apk/" + args.mode)
        if args.mode == "release":
            print("Release sin firma configurada: alinear, firmar y verificar según docs/reusable/BUILD_SIGNING.md.")
    return result.returncode


if __name__ == "__main__":
    raise SystemExit(main())
