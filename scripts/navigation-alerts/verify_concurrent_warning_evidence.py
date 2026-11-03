from pathlib import Path
import json,hashlib,struct,sys

if not __debug__:
    raise RuntimeError("Run validation without -O or PYTHONOPTIMIZE; assertion checks are required")


def event_key(event):
 raw=b''
 for value in [event['provider_session'],event['warning_id'],event['phase'].upper()]:
  data=value.encode('ascii');raw+=struct.pack('>H',len(data))+data
 return hashlib.sha256(raw+struct.pack('>q',event['announcement_index'])).hexdigest()

def audit(run):
 host=json.loads((run/'result.json').read_text());source=json.loads(Path(host['warning_suite_source_result']).read_text());case=source['scenarios']['integrated_warning_suite'];receiver=json.loads((run/'seed-receiver-result.json').read_text());assert host['probe_completed'] and source['probe_completed'];pairs=case['concurrent_warning_records'];assert len(pairs)==3
 starts=[row for row in receiver['diagnostics'] if row['stage']=='SCHEDULER_STARTED'];terminals=[row for row in receiver['diagnostics'] if row['stage'] in ['SCHEDULER_COMPLETED','SCHEDULER_INTERRUPTED','SCHEDULER_CANCELLED']];assert len(starts)==len(terminals)==7
 results=[];intervals=[]
 warm=case['warmup_authority'];warm_key=event_key(warm);warm_start=[row for row in starts if row['event_key']==warm_key];warm_end=[row for row in terminals if row['event_key']==warm_key];assert len(warm_start)==len(warm_end)==1 and warm_end[0]['stage']=='SCHEDULER_COMPLETED';assert warm_start[0]['elapsed_ms']<warm['announce_until_elapsed_ms'];assert [row['outcome'] for row in case['warmup_source_dispositions']]==['GRANTED','STARTED','COMPLETED'];intervals.append((warm_start[0]['elapsed_ms'],warm_end[0]['elapsed_ms'],warm_key))
 for pair in pairs:
  events=[pair['first_authority'],pair['second_authority']];assert 0<=events[1]['observed_elapsed_ms']-events[0]['observed_elapsed_ms']<8000;proof=[]
  for index,event in enumerate(events):
   key=event_key(event);begin=[row for row in starts if row['event_key']==key];end=[row for row in terminals if row['event_key']==key];assert len(begin)==len(end)==1 and begin[0]['elapsed_ms']<=end[0]['elapsed_ms'];assert begin[0]['elapsed_ms']<event['announce_until_elapsed_ms'];payloads=[row for row in receiver['actual_speaker_attempts'] if row['event_key']==key];assert len(payloads)==1 and payloads[0]['started'] and payloads[0]['payload']['message']==event['message'];intervals.append((begin[0]['elapsed_ms'],end[0]['elapsed_ms'],key));proof.append({'authority':event,'actual_speaker_attempt':payloads[0],'actual_start':begin[0],'actual_terminal':end[0]})
  if pair['pair']==1:
   assert events[0]['priority']==1 and events[1]['priority']==0 and events[0]['latitude']<events[1]['latitude'];assert proof[0]['actual_start']['elapsed_ms']<=events[1]['observed_elapsed_ms'];assert proof[0]['actual_terminal']['stage'] in ['SCHEDULER_CANCELLED','SCHEDULER_INTERRUPTED'];assert proof[1]['actual_terminal']['stage']=='SCHEDULER_COMPLETED';oldkey=event_key(events[0]);newkey=event_key(events[1]);oldterm=[r for r in pair['source_dispositions'] if r['event_key']==oldkey and r['outcome'] in ['CANCELLED','INTERRUPTED']];newgrant=[r for r in pair['source_dispositions'] if r['event_key']==newkey and r['outcome']=='GRANTED'];assert len(oldterm)==len(newgrant)==1 and oldterm[0]['started'] and oldterm[0]['observed_elapsed_ms']<=newgrant[0]['observed_elapsed_ms']
  else:
   assert all(row['actual_terminal']['stage']=='SCHEDULER_COMPLETED' for row in proof);assert proof[0]['actual_terminal']['elapsed_ms']<=proof[1]['actual_start']['elapsed_ms']
  results.append({'scenario':pair['scenario'],'events':proof,'source_dispositions':pair['source_dispositions'],'source_first_terminal':pair['first_terminal'],'source_second_terminal':pair['second_terminal']})
 intervals.sort();assert all(intervals[i][1]<=intervals[i+1][0] for i in range(len(intervals)-1));assert len({row['event_key'] for row in starts})==7;assert source['network_and_lease_restored'] and host['source_permissions_restored_exactly'] and host['window_settings_restored_exactly'] and all(row['restored_exactly'] for row in host['final_preferences_cleanup'].values());assert receiver['output_quiescent'] and receiver['preferences_restored_exactly']
 if host['receiver']=='vela':assert host['vela_permissions_restored_exactly']
 return {'apps':host['installed_sha256'],'helpers':host['helper_sha256'],'raw_result':str(run/'result.json'),'source_result':host['warning_suite_source_result'],'receiver_result':str(run/'seed-receiver-result.json'),'genuine_native_warmup':case['warmup_authority'],'measured_authorities':6,'actual_starts_including_warmup':7,'actual_terminal_count_including_warmup':7,'duplicate_starts':0,'no_native_api_playback_interval_overlap':True,'all_announcements_started_before_their_immutable_deadline':True,'provider_managed_mute':case['managed_mute_verified'],'exact_cleanup_passed':True,'pairs':results}
if __name__=='__main__':
 for value in sys.argv[1:]:
  result=audit(Path(value));Path(value,'independent-audit.json').write_text(json.dumps(result,indent=2)+'\n');print(value,'independent concurrent authority/payload/start/stop/ACK audit passed')
