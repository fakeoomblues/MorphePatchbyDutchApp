import importlib.util
from pathlib import Path
import unittest

SPEC = importlib.util.spec_from_file_location('fm_cleanup', Path(__file__).resolve().parents[1] / 'scripts/navigation-alerts/run_fm_capture_probe.py')
MOD = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MOD)

class FirewallCleanupTest(unittest.TestCase):
    before = '*mangle\n:INPUT ACCEPT [1:10]\n-A routectrl_mangle_INPUT -i wlan0 -j MARK --set-xmark 0x30065/0xffefffff\nCOMMIT\n*filter\n:OUTPUT ACCEPT [1:10]\nCOMMIT\n'
    def test_counters_and_independent_netd_interface_marks_are_not_rolled_back(self):
        after = self.before.replace('[1:10]', '[9:99]').replace('0x30065', '0x30066')
        self.assertEqual(1, len(MOD.firewall_cleanup_changes(self.before, after, 'NWFM_test')))
    def test_remaining_owned_rule_fails(self):
        with self.assertRaisesRegex(RuntimeError, 'remains'):
            MOD.firewall_cleanup_changes(self.before, self.before.replace('COMMIT', '-A OUTPUT -j NWFM_test\nCOMMIT', 1), 'NWFM_test')
    def test_any_changed_filter_rule_fails(self):
        with self.assertRaisesRegex(RuntimeError, 'filter'):
            MOD.firewall_cleanup_changes(self.before, self.before.replace(':OUTPUT ACCEPT', ':OUTPUT DROP'), 'NWFM_test')
    def test_unrelated_mangle_rule_change_fails(self):
        with self.assertRaisesRegex(RuntimeError, 'Unexpected mangle'):
            MOD.firewall_cleanup_changes(self.before, self.before.replace(':INPUT ACCEPT', ':INPUT DROP'), 'NWFM_test')
