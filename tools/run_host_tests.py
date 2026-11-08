#!/usr/bin/env python3
"""Run existing pure Java suites using Android signatures only; no ADB or device fixtures."""
import argparse
import os
from pathlib import Path
import subprocess

NAMES = "AppDrawerSearch GpsSpeedState GpsTelemetry MediaActions MediaSelectionPolicy MediaSources SourceCycle SpanishDateFormatter VehicleLightingPolicy WeatherPresentation StartupRecoveryPolicy GpsUpdatePolicy VehiclePlateConfig ThemeConfig ThemeManager DisplayMode DashboardPalette DisplayPreferences".split()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sdk", type=Path, default=Path(os.environ.get("ANDROID_HOME", "")))
    args = parser.parse_args()
    jar = args.sdk / "platforms/android-35/android.jar"
    if not jar.is_file():
        parser.error("Indica --sdk o ANDROID_HOME con Platform 35.")
    root = Path(__file__).resolve().parents[1]
    output = root / "build/host-tests"
    output.mkdir(parents=True, exist_ok=True)
    tests = sorted((root / "tests/java/com/fytsmart/dashboard").glob("*Test.java"))
    sources = [root / ("app/src/main/java/com/fytsmart/dashboard/" + name + ".java") for name in NAMES]
    subprocess.run(["javac", "-encoding", "UTF-8", "-cp", str(jar), "-d", str(output)] + [str(p) for p in sources + tests], check=True)
    for test in tests:
        subprocess.run(["java", "-cp", str(output) + os.pathsep + str(jar), "com.fytsmart.dashboard." + test.stem], check=True)
    print(str(len(tests)) + " suites Java: PASS. No se ejecutó nada en Android.")


if __name__ == "__main__":
    main()
