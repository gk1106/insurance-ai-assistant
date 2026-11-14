"""Exercise update control flow with fake Docker/flock; never contact a server or Docker daemon."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

HERE = Path(__file__).resolve().parent
BASH = os.environ.get('BASH_BIN') or shutil.which('bash')
if os.name == 'nt' and Path('C:/Program Files/Git/bin/bash.exe').exists():
    BASH = 'C:/Program Files/Git/bin/bash.exe'

@unittest.skipUnless(BASH, 'Bash is required')
class UpdateTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.bin = self.root / 'bin'
        self.bin.mkdir()
        shutil.copyfile(HERE / 'update.sh', self.root / 'update.sh')
        (self.root / '.env').write_text('OPENAI_API_KEY=fixture-only\n', encoding='utf-8')
        (self.root / 'docker-compose.prod.yml').write_text('services: {}\n', encoding='utf-8')
        self.sha = 'a' * 40
        self.log = self.root / 'calls.log'
        (self.bin / 'docker').write_text('#!/usr/bin/env bash\nprintf \'%s|%s\\n\' "$IMAGE_TAG" "$*" >> "$TEST_DOCKER_LOG"\nif [[ "$*" == *\' pull \'* && "${TEST_FAIL_PULL:-0}" == 1 ]]; then exit 42; fi\nif [[ "$*" == *\' up \'* && "${TEST_FAIL_UP:-0}" == 1 ]]; then exit 43; fi\nexit 0\n', encoding='utf-8')
        (self.bin / 'flock').write_text('#!/usr/bin/env bash\nexit "${TEST_FAIL_LOCK:-0}"\n', encoding='utf-8')
        for tool in self.bin.iterdir(): tool.chmod(0o700)

    def tearDown(self): self.temp.cleanup()

    def invoke(self, tag=None, **flags):
        env = os.environ.copy()
        env['TEST_DOCKER_LOG'] = self.log.as_posix()
        env.update(flags)
        path_setup = 'export PATH="$(cygpath -u "$1"):$PATH"; shift; exec bash "$@"' if os.name == 'nt' else 'export PATH="$1:$PATH"; shift; exec bash "$@"'
        command = [BASH, '-c', path_setup, '_', self.bin.as_posix(), (self.root / 'update.sh').as_posix()]
        if tag is not None: command.append(tag)
        return subprocess.run(command, env=env, text=True, capture_output=True)

    def calls(self):
        return self.log.read_text(encoding='utf-8').splitlines() if self.log.exists() else []

    def test_success_checks_pulls_then_starts_exact_commit_and_preserves_env(self):
        before = (self.root / '.env').read_bytes()
        result = self.invoke(self.sha)
        self.assertEqual(result.returncode, 0, result.stderr)
        calls = self.calls()
        self.assertEqual(len(calls), 4)
        self.assertTrue(all(line.startswith(self.sha + '|') for line in calls))
        self.assertIn('config --quiet', calls[0])
        self.assertIn('pull backend frontend', calls[1])
        self.assertIn('up -d --no-build --wait --wait-timeout 240', calls[2])
        self.assertNotIn('down', '\n'.join(calls))
        self.assertEqual((self.root / '.last-deployed-tag').read_text().strip(), self.sha)
        self.assertEqual((self.root / '.env').read_bytes(), before)
        self.assertTrue((self.root / 'documents').is_dir())

    def test_pull_failure_never_recreates_containers(self):
        result = self.invoke(self.sha, TEST_FAIL_PULL='1')
        self.assertEqual(result.returncode, 42)
        self.assertEqual(len(self.calls()), 2)
        self.assertFalse((self.root / '.last-deployed-tag').exists())

    def test_health_failure_does_not_record_a_successful_release(self):
        (self.root / '.last-deployed-tag').write_text('b' * 40)
        result = self.invoke(self.sha, TEST_FAIL_UP='1')
        self.assertEqual(result.returncode, 43)
        self.assertEqual((self.root / '.last-deployed-tag').read_text(), 'b' * 40)

    def test_invalid_tag_rejected_before_docker(self):
        for tag in ['main', '../bad', 'a; echo injected', '123', '$(echo bad)']:
            self.assertEqual(self.invoke(tag).returncode, 2)
        self.assertEqual(self.calls(), [])

    def test_release_file_and_latest_are_supported(self):
        (self.root / 'release-tag.txt').write_bytes((self.sha + '\r\n').encode())
        self.assertEqual(self.invoke().returncode, 0)
        self.assertEqual(self.invoke('latest').returncode, 0)

    def test_missing_environment_is_rejected(self):
        (self.root / '.env').unlink()
        self.assertEqual(self.invoke(self.sha).returncode, 2)
        self.assertEqual(self.calls(), [])

    def test_lock_conflict_does_not_call_docker(self):
        self.assertEqual(self.invoke(self.sha, TEST_FAIL_LOCK='1').returncode, 1)
        self.assertEqual(self.calls(), [])

if __name__ == '__main__': unittest.main()
