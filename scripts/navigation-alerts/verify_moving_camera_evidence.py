#!/usr/bin/env python3
"""Join original camera decisions, real receiver GPS and playback acknowledgements."""
import argparse
import hashlib
import json
import math
from pathlib import Path
import struct

if not __debug__:
    raise RuntimeError("Run validation without -O or PYTHONOPTIMIZE; assertion checks are required")



def event_key(authority):
    # The private fixture IDs are ASCII, so Java writeUTF has ordinary UTF-8 bytes.
    values = [authority['provider_session'], authority['warning_id'], authority['phase'].upper()]
    data = b''
    for value in values:
        encoded = value.encode('ascii')
        data += struct.pack('>H', len(encoded)) + encoded
    return hashlib.sha256(data + struct.pack('>q', authority['announcement_index'])).hexdigest()


def verify(folder):
    record = json.loads((folder / 'result.json').read_text())
    assert record['probe_completed'] and record['failure'] is None
    assert record['serial'] == 'emulator-5590' and record['avd'] == 'native_warnings_ui_api34'
    assert record['warning_suite_cases'] == 'moving-camera'
    for key in ['route_permissions_restored_exactly', 'source_permissions_restored_exactly',
                'window_settings_restored_exactly']:
        assert record[key], key
    assert all(value['restored_exactly'] for value in record['final_preferences_cleanup'].values())
    source = json.loads(Path(record['warning_suite_source_result']).read_text())
    assert source['probe_completed'] and source['failure'] is None
    assert source['network_and_lease_restored'] and not source['cleanup_errors']
    case = source['scenarios']['integrated_warning_suite']
    assert case['production_endpoint_used'] and not case['test_sink_used']
    assert not case['raw_packet_emitted'] and not case['coverage_override_used']
    assert case['managed_mute_verified'] and case['device_stream_unchanged']
    assert case['provider_native_queue_empty'] and case['receiver_source_off_and_quiescent']
    receiver = json.loads((folder / 'seed-receiver-result.json').read_text())
    assert receiver['status'] == 'observed' and receiver['output_quiescent']
    assert receiver['preferences_restored_exactly'] and receiver['uid'] != source['uid']
    if record['receiver'] == 'vela':
        assert record['vela_permissions_restored_exactly']
    rows = case['moving_camera_records']
    assert [row['speed_kmh'] for row in rows] == [50, 100, 130]
    attempts = receiver['actual_speaker_attempts']
    assert len(attempts) == 3 and len({row['attempt_id'] for row in attempts}) == 3
    diagnostics = receiver['diagnostics']
    assert sum(row['stage'] == 'SCHEDULER_STARTED' for row in diagnostics) == 3
    assert sum(row['stage'] == 'SCHEDULER_COMPLETED' for row in diagnostics) == 3
    results = []
    for row in rows:
        authority = row['authority']
        assert authority['operation'] == 'ANNOUNCE' and authority['native_type'] == 'SPEED_CAM'
        assert authority['native_subtype'] == 'SPEED_' + str(row['speed_kmh'])
        assert authority['relevance'] == 'point_ahead' and authority['priority'] == 1
        key = event_key(authority)
        speaker, = [attempt for attempt in attempts if attempt['event_key'] == key]
        payload = speaker['payload']
        assert speaker['started']
        for field in ['provider_session', 'warning_id', 'announcement_index', 'phase',
                      'native_type', 'native_subtype', 'category', 'relevance', 'relationship',
                      'message', 'message_origin', 'language_tag', 'native_text_key',
                      'latitude', 'longitude', 'priority']:
            assert payload[field] == authority[field], field
        accepted, = [event for event in diagnostics if event['stage'] == 'RECEIVER_ACCEPTED'
                     and event['sequence'] == authority['sequence'] and event['event_key'] == key]
        started, = [event for event in diagnostics if event['stage'] == 'SCHEDULER_STARTED'
                    and event['event_key'] == key]
        completed, = [event for event in diagnostics if event['stage'] == 'SCHEDULER_COMPLETED'
                      and event['event_key'] == key]
        decision = row['native_decision_elapsed_ms']
        crossed = row['target_crossed_elapsed_ms']
        issued = authority['issued_elapsed_ms']
        assert decision <= issued <= accepted['elapsed_ms'] <= started['elapsed_ms'] < completed['elapsed_ms'] < crossed
        assert 0 <= started['elapsed_ms'] - issued < 2000, 'Extra receiving delay'
        assert crossed - started['elapsed_ms'] > 12000, 'Native lead collapsed to a twelve-second gate'
        speed = row['speed_kmh'] / 3.6
        assert 19 < row['native_decision_remaining_m'] / speed <= 20.2
        clear = row['native_clear']
        assert clear['operation'] == 'CLEAR' and clear['warning_id'] == authority['warning_id']
        assert clear['announcement_index'] == authority['announcement_index']
        assert clear['sequence'] > authority['sequence'] and clear['issued_elapsed_ms'] >= completed['elapsed_ms']
        assert row['source_terminal'] == 'COMPLETED'
        assert any(value['outcome'] == 'STARTED' and value['started'] for value in row['source_dispositions'])
        assert any(value['outcome'] == 'COMPLETED' and value['started'] for value in row['source_dispositions'])
        assert all(value['receiver_uid'] == receiver['uid'] and value['mode'] == 'VOICE'
                   for value in row['source_dispositions'])
        native_fixes = row['native_fixes']
        samples = [fix for fix in receiver['native_location_samples']
                   if native_fixes[0]['source_elapsed_ms'] <= fix['fix_elapsed_ms'] <= crossed + 2500
                   and abs(fix['speed_mps'] - speed) < .1]
        assert len(samples) >= 10, 'No moving native receiving location witness'
        assert any(fix['latitude'] < row['target_latitude'] for fix in samples)
        assert any(fix['latitude'] >= row['target_latitude'] for fix in samples)
        for fix in samples:
            assert fix['provider'] == 'gps' and 0 <= fix['observed_elapsed_ms'] - fix['fix_elapsed_ms'] < 2000
            assert math.isclose(fix['longitude'], row['target_longitude'], abs_tol=0.000003)
        results.append({'speed_kmh': row['speed_kmh'], 'native_decision_distance_m': row['native_decision_remaining_m'],
                        'decision_to_start_ms': started['elapsed_ms'] - decision,
                        'issued_to_start_ms': started['elapsed_ms'] - issued,
                        'receiver_accept_to_start_ms': started['elapsed_ms'] - accepted['elapsed_ms'],
                        'started_lead_ms': crossed - started['elapsed_ms'],
                        'completion_lead_ms': crossed - completed['elapsed_ms'],
                        'native_receiving_gps_samples': len(samples), 'native_clear_sequence': clear['sequence'],
                        'message': payload['message'], 'warning_id': authority['warning_id'],
                        'event_key': key, 'attempt_id': speaker['attempt_id']})
    evidence_files = [folder / 'result.json', folder / 'seed-receiver-result.json',
                      Path(record['warning_suite_source_result'])]
    return {'receiver': record['receiver'], 'folder': str(folder), 'installed_sha256': record['installed_sha256'],
            'raw_sha256': {str(path): hashlib.sha256(path.read_bytes()).hexdigest() for path in evidence_files},
            'helper_sha256': record['helper_sha256'], 'source_uid': source['uid'], 'receiver_uid': receiver['uid'],
            'cases': results, 'actual_starts': 3, 'actual_completions': 3,
            'extra_start_from_native_update_or_clear': False, 'exact_cleanup_verified': True,
            'scope': 'Original SpeedCamTask.a/d decisions with private candidate/location fixtures and real emulator GPS in the receiver. No public repository mutation or direct warning-manager trigger; not natural detector, physical driving or car-output acceptance.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('folders', nargs='+', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    results = [verify(folder) for folder in args.folders]
    args.output.write_text(json.dumps({'format': 1, 'runs': results}, indent=2) + '\n')
    print(json.dumps({'verified': len(results), 'native_camera_cases': sum(len(row['cases']) for row in results),
                      'output': str(args.output)}))


if __name__ == '__main__':
    main()
