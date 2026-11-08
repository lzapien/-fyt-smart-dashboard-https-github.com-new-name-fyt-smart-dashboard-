import argparse
import errno
import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("dashboard", ROOT / "tools/dashboard.py")
tool = importlib.util.module_from_spec(spec)
spec.loader.exec_module(tool)


class FakeAdb:
    device = "emulator-5554"
    def __init__(self):
        self.calls = []
        self.values = {"prop_manufacturer": "Reference", "prop_model": "Sample Unit",
                       "prop_android": "10", "prop_sdk": "29", "prop_abis": "arm64-v8a,armeabi-v7a",
                       "prop_fyt_platform": "6315", "prop_adb_tcp_port": "12345",
                       "screen_size": "Physical size: 1280x720\nOverride size: 1024x600",
                       "screen_density": "Physical density: 160\nOverride density: 200",
                       "memory": "MemTotal: 4000000 kB\nMemAvailable: 2000000 kB\nPRIVATE_SENTINEL: unused",
                       "features": "feature:android.hardware.location.gps\nfeature:android.hardware.sensor.compass",
                       "packages": "package:com.navimods.radio versionCode:100\npackage:com.waze versionCode:200\npackage:com.private.unrelated versionCode:1",
                       "binder_service_names": "Found 3 services:\n0 media_session: [android.media.session.ISessionManager]\n1 com.syu.ms: [com.syu.IMs]\n2 unrelated: [private.Interface]",
                       "dump_service_names": "Currently running services:\n  media.audio_flinger\n  location",
                       "home": "priority=0\ncom.example.launcher/.MainActivity", "location_mode": "3"}
    def require_device(self):
        self.calls.append("require_device")
    def read(self, name):
        self.calls.append(name)
        value = self.values.get(name, "")
        if isinstance(value, Exception):
            raise value
        return value


class ProfileTests(unittest.TestCase):
    def test_profile_is_metadata_only_and_limits_verification(self):
        fake = FakeAdb();profile = tool.collect(fake);text = json.dumps(profile)
        self.assertNotIn("PRIVATE_SENTINEL", text)
        self.assertNotIn("com.private.unrelated", text)
        self.assertNotIn("emulator-5554", text)
        self.assertEqual(profile["hardware"]["sdk"], 29)
        self.assertEqual(profile["hardware"]["abis"], ["arm64-v8a", "armeabi-v7a"])
        self.assertEqual(profile["display"]["size"]["value"]["effective"], {"width": 1024, "height": 600})
        self.assertEqual(profile["display"]["density"]["value"]["effective"], 200)
        self.assertEqual(profile["memory_kib"]["MemAvailable"], 2000000)
        self.assertEqual(profile["capabilities"]["gps"]["status"], "detected")
        self.assertEqual(profile["capabilities"]["outside_temperature"]["status"], "unknown")
        self.assertIsNone(profile["capabilities"]["outside_temperature"]["value"])
        self.assertEqual(profile["capabilities"]["vehicle_information"]["status"], "unknown")
        self.assertEqual(profile["capabilities"]["wireless_pairing"]["status"], "unknown")
        self.assertEqual(profile["adb"]["connected"]["status"], "verified")
        self.assertEqual(profile["adb"]["reported_ports"], [12345])
        self.assertEqual(profile["vendor_services"], ["com.syu.ms"])
        self.assertEqual(fake.calls[0], "require_device")

    def test_denials_do_not_invent_absence_and_keep_working(self):
        fake = FakeAdb()
        for key in ("features", "screen_size", "location_mode", "binder_service_names", "dump_service_names"):
            fake.values[key] = tool.DiagnosticError("permission_denied")
        profile = tool.collect(fake)
        self.assertEqual(profile["capabilities"]["gps"]["status"], "unknown")
        self.assertIsNone(profile["capabilities"]["gps"]["value"]["declared"])
        self.assertIsNone(profile["display"]["size"]["value"])
        self.assertEqual(profile["capabilities"]["media_sessions"]["status"], "unknown")
        self.assertEqual(len(profile["query_summary"]["errors"]), 5)

    def test_old_package_command_fallback(self):
        fake = FakeAdb();fake.values["packages"] = tool.DiagnosticError("unsupported")
        fake.values["packages_fallback"] = "package:com.navimods.radio"
        profile = tool.collect(fake)
        self.assertIn("packages_fallback", fake.calls)
        self.assertEqual(profile["relevant_packages"], [{"package": "com.navimods.radio", "version_code": None}])

    def test_write_never_overwrites_previous_diagnostic(self):
        with tempfile.TemporaryDirectory() as folder:
            profile = tool.collect(FakeAdb());tool.write_profile(profile, folder)
            first = (Path(folder) / "device-profile.json").read_bytes()
            self.assertEqual(json.loads(first)["hardware"]["model"], "Sample Unit")
            self.assertIn("outside_temperature", (Path(folder) / "device-report.md").read_text())
            with self.assertRaises(tool.DiagnosticError):tool.write_profile(profile, folder)
            self.assertEqual((Path(folder) / "device-profile.json").read_bytes(), first)

    def test_markdown_is_not_injected_from_model_text(self):
        fake = FakeAdb();fake.values["prop_model"] = "![x](https://invalid.example/)`<script>"
        report = tool.markdown(tool.collect(fake))
        self.assertIn("\\u0060", report)
        self.assertNotIn("<script>", report)

    def test_no_sensitive_or_write_queries(self):
        joined = " ".join(" ".join(q) for q in tool.QUERIES.values())
        for forbidden in ("ro.serialno", "android_id", "latitude", "dumpsys location", "dumpsys media_session", "logcat", " set ", "service call", "tcpip", "reboot", "install", "su "):
            self.assertNotIn(forbidden, joined)
        self.assertTrue(all(q[0] == "shell" for q in tool.QUERIES.values()))


class TransportTests(unittest.TestCase):
    def test_tcp_route_failure_does_not_modify_network(self):
        with patch.object(tool.shutil,"which",return_value=None), patch.object(tool.socket,"create_connection",side_effect=OSError(errno.EHOSTUNREACH,"unreachable")):
            result=tool.network("stereo.example",[12345])
        self.assertIsNone(result["ping"])
        self.assertEqual(result["tcp"],[{"port":12345,"reachable":False,"reason":"no_route_to_host"}])

    def test_connect_failure_even_if_adb_returns_zero(self):
        with patch.object(tool.shutil,"which",return_value="fake-adb"), patch.object(tool.Adb,"run",return_value="failed to connect: connection refused"):
            self.assertEqual(tool.main(["connect","--host","stereo.example","--port","12345"]),2)

    def test_inputs_reject_shell_fragments(self):
        for value in ("-bad", "host;reboot", "host$(id)", "host:12", "", "host/name"):
            with self.assertRaises(argparse.ArgumentTypeError):tool.validate_host(value)
        for value in ("device shell reboot", "-s", "$(id)"):
            with self.assertRaises(argparse.ArgumentTypeError):tool.validate_device(value)
        for value in (0, 65536, "bad"):
            with self.assertRaises(argparse.ArgumentTypeError):tool.port(value)
        self.assertEqual(tool.endpoint(tool.validate_host("[2001:db8::1]"), 12345), "[2001:db8::1]:12345")

    def test_remote_reads_always_use_selected_transport(self):
        with patch.object(tool.shutil, "which", return_value="fake-adb"):
            adb = tool.Adb(device="emulator-5554")
        with patch.object(adb, "run", return_value="29") as run:
            self.assertEqual(adb.read("prop_sdk"), "29")
            run.assert_called_once_with("-s", "emulator-5554", "shell", "getprop", "ro.build.version.sdk")
        with self.assertRaises(tool.DiagnosticError):adb.read("reboot")

    def test_unauthorized_and_offline_stop_before_shell(self):
        for state in ("unauthorized", "offline"):
            with patch.object(tool.shutil, "which", return_value="fake-adb"):
                adb = tool.Adb(device="emulator-5554")
            with patch.object(adb, "run", return_value="List of devices attached\nemulator-5554\t" + state) as run:
                with self.assertRaisesRegex(tool.DiagnosticError, state):adb.require_device()
                run.assert_called_once_with("devices")

    def test_parser_does_not_require_adb_for_help_or_network(self):
        with self.assertRaises(SystemExit) as context:tool.main(["--help"])
        self.assertEqual(context.exception.code, 0)
        with patch.object(tool, "network", return_value={"ping": False, "tcp": []}):
            self.assertEqual(tool.main(["network", "--host", "stereo.example", "--port", "12345"]), 0)

    def test_no_implicit_install_or_tcpip_cli(self):
        for command in ("install", "reboot", "tcpip", "home"):
            with self.assertRaises(SystemExit):tool.main([command])
        self.assertEqual(tool.failure("No route to host"), "no_route_to_host")
        self.assertEqual(tool.failure("device unauthorized"), "unauthorized")
        self.assertEqual(tool.failure("device offline"), "offline")


if __name__ == "__main__":
    unittest.main()
