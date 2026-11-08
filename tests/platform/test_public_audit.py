import importlib.util
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("public_audit", Path(__file__).resolve().parents[2] / "tools/public_audit.py")
tool = importlib.util.module_from_spec(spec);spec.loader.exec_module(tool)


class PublicAuditTests(unittest.TestCase):
    def tree(self, folder):
        root = Path(folder).resolve();plate = root / "app/src/main/java/com/fytsmart/dashboard/VehiclePlateConfig.java"
        plate.parent.mkdir(parents=True);plate.write_text('static final String DEFAULT = "TU-PLACA";')
        return root

    def test_clean_export_and_absence_of_history(self):
        with tempfile.TemporaryDirectory() as folder:
            result = tool.audit(self.tree(folder), ["private-test-fixture"])
            self.assertEqual(result["status"], "PASS")
            self.assertEqual(result["history"], "not_present")

    def test_private_values_and_files_fail_without_echoing_values(self):
        with tempfile.TemporaryDirectory() as folder:
            root = self.tree(folder);(root / "example.md").write_text("private-test-fixture")
            (root / "local.properties").write_text("sdk.dir=not-for-distribution")
            result = tool.audit(root, ["private-test-fixture"])
            self.assertEqual(result["status"], "REVIEW_REQUIRED")
            self.assertNotIn("private-test-fixture", str(result))
            self.assertEqual({f["reason"] for f in result["findings"]}, {"private_value_found", "private_or_build_file"})

    def test_non_generic_default_fails(self):
        with tempfile.TemporaryDirectory() as folder:
            root = self.tree(folder)
            (root / "app/src/main/java/com/fytsmart/dashboard/VehiclePlateConfig.java").write_text('static final String DEFAULT = "CUSTOM";')
            self.assertEqual(tool.audit(root)["status"], "REVIEW_REQUIRED")

    def test_reachable_old_blob_is_checked_even_if_worktree_clean(self):
        with tempfile.TemporaryDirectory() as folder:
            root = self.tree(folder);original = Path.is_dir
            def is_dir(path):
                return True if path == root / ".git" else original(path)
            calls = [subprocess.CompletedProcess([], 0, "a" * 40 + " old.txt\n", ""),
                     subprocess.CompletedProcess([], 0, "a" * 40 + "\n", ""),
                     subprocess.CompletedProcess([], 0, "blob\n", ""),
                     subprocess.CompletedProcess([], 0, b"private-test-fixture", b"")]
            with patch.object(Path, "is_dir", is_dir), patch.object(tool.subprocess, "run", side_effect=calls):
                result = tool.audit(root, ["private-test-fixture"])
            self.assertEqual(result["history"], "scanned")
            self.assertTrue(any(f["path"].startswith("git-object:") for f in result["findings"]))


if __name__ == "__main__":
    unittest.main()
