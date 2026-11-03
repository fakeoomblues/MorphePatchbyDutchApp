#!/usr/bin/env python3
"""Coordinate real FM/Maps app processes on the owned AVD; never emits warning packets."""
import argparse
import hashlib
import json
from pathlib import Path
import queue
import re
import shutil
import subprocess
import sys
import threading
import time


def verify_emulator_profile(serial,qemu,avd_name,sdk):
    """Old5580 is deliberately inadmissible after the external-thread handoff."""
    names=[line.strip() for line in avd_name.splitlines() if line.strip() and line.strip()!='OK']
    if serial!='emulator-5584' or qemu.strip()!='1' or names!=['native_warnings_car_api34'] or sdk.strip()!='34':
        raise ValueError('Only owned emulator-5584/native_warnings_car_api34/API34 is authorized for this cohort')
    return names[0]


def prepare_output(path):
    if path.exists() and any(path.iterdir()):
        raise ValueError("Use a fresh output directory; existing evidence must be retained")
    path.mkdir(parents=True,exist_ok=True)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--sdk',type=Path,required=True)
    parser.add_argument('--serial',choices=['emulator-5584'],default='emulator-5584')
    parser.add_argument('--source-apk',type=Path,required=True)
    parser.add_argument('--source-probe',type=Path,required=True)
    parser.add_argument('--receiver-apk',type=Path,required=True,help='Already test-signed current Maps APK')
    parser.add_argument('--receiver-probe',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--native-id',required=True)
    parser.add_argument('--mode',choices=['VOICE','TONE','SILENT'],required=True)
    parser.add_argument('--reviewed-synthetic-choices',action='store_true',required=True)
    parser.add_argument('--matrix-count',type=int,choices=range(1,101))
    parser.add_argument('--trigger-family',choices=['ordinary','p0'],default='ordinary')
    parser.add_argument('--trigger-pattern',choices=['single','update','clear','repeat'],default='single')
    parser.add_argument('--matrix-timeout-seconds',type=int,choices=range(60,901),default=900)
    parser.add_argument('--observe-pcm',action='store_true',help='Private observer for one integrated VOICE case only; never timing100')
    parser.add_argument('--diagnostic-hidden-api-policy',action='store_true',help='Owned5580 only: temporarily allow private listener identity observation and restore exact prestate')
    args=parser.parse_args();serial=args.serial;adb_path=args.sdk/'platform-tools/adb';prepare_output(args.output)
    if args.observe_pcm and (args.mode!='VOICE' or args.matrix_count!=1):parser.error('PCM observation is limited to one reviewed integrated VOICE case')
    if args.diagnostic_hidden_api_policy and not args.observe_pcm:parser.error('Diagnostic policy is allowed only for the private PCM observer')
    if args.diagnostic_hidden_api_policy:parser.error('Prior5580 diagnostic-policy admission does not authorize a new5584 override')
    def adb(*command,check=True):return subprocess.run([str(adb_path),'-s',serial,*map(str,command)],capture_output=True,text=True,check=check,timeout=60)
    def sha(path):
        with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()
    avd=verify_emulator_profile(serial,adb('shell','getprop','ro.kernel.qemu').stdout,adb('emu','avd','name').stdout,adb('shell','getprop','ro.build.version.sdk').stdout)
    if adb('shell','id','-u').stdout.strip()!='0':raise ValueError('Owned AVD adbd must already be root for the source UID network fence')
    inputs={};certs=[]
    for package,apk in [('nl.flitsmeister',args.source_apk),('com.google.android.apps.maps',args.receiver_apk),('nl.johninnl.warningprobe.integration.maps',args.receiver_probe)]:
        path=adb('shell','pm','path',package).stdout.strip().removeprefix('package:');actual=adb('shell','sha256sum',path).stdout.split()[0]
        if actual!=sha(apk):raise ValueError('Installed bytes differ: '+package)
        inputs[package]=actual
    for apk in [args.source_apk,args.source_probe,args.receiver_apk,args.receiver_probe]:
        text=subprocess.check_output([str(args.sdk/'build-tools/36.0.0/apksigner'),'verify','--print-certs',str(apk)],text=True)
        certs.append(re.search(r'Signer #1 certificate SHA-256 digest: ([0-9a-f]+)',text).group(1))
    if len(set(certs))!=1:raise ValueError('Source/receiver/helper signing mismatch')
    shutil.copyfile(args.receiver_probe,args.output/'receiver-helper.apk')
    def permissions():
        text=adb('shell','dumpsys','package','nl.flitsmeister').stdout;out={}
        for name in ['ACCESS_COARSE_LOCATION','ACCESS_FINE_LOCATION','ACCESS_BACKGROUND_LOCATION']:
            permission='android.permission.'+name
            found=re.search(re.escape(permission)+r': granted=(true|false), flags=\[([^\]]*)\]',text)
            if found:out[permission]={'granted':found.group(1)=='true','flags':found.group(2).strip()}
        return out
    before=permissions();changed=[];events=queue.Queue();processes={};threads=[];failure=None;receiver=None;source=None
    policy_before=adb('shell','settings','get','global','hidden_api_policy').stdout.strip() if args.diagnostic_hidden_api_policy else None
    record={'serial':serial,'qemu':'1','avd':avd,'android_sdk':34,'installed_sha256':inputs,'source_probe_sha256':sha(args.source_probe),'signer_sha256':certs[0],'permissions_before':before,'native_id':args.native_id,'mode':args.mode,'diagnostic_hidden_api_policy_before':policy_before,'diagnostic_policy_is_not_production_requirement':True}
    def launch(name,command):
        process=subprocess.Popen(list(map(str,command)),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,bufsize=1);processes[name]=process
        record[name+'_host_pid']=process.pid
        def drain():
            with (args.output/(name+'-output.txt')).open('w') as log:
                for line in process.stdout:log.write(line);log.flush();events.put((name,line));print(name+': '+line.rstrip(),flush=True)
            events.put((name,None))
        thread=threading.Thread(target=drain,daemon=True);thread.start();threads.append(thread);return process
    try:
        if args.diagnostic_hidden_api_policy:adb('shell','settings','put','global','hidden_api_policy','1')
        if not {'android.permission.ACCESS_FINE_LOCATION','android.permission.ACCESS_COARSE_LOCATION'}<=before.keys():raise ValueError('Source manifest/runtime location permission state is missing')
        for permission,value in before.items():
            if not value['granted']:
                if any(flag in value['flags'] for flag in ['POLICY_FIXED','SYSTEM_FIXED','USER_FIXED']):raise ValueError('Refuse to override a fixed source permission')
                adb('shell','pm','grant','nl.flitsmeister',permission);changed.append(permission)
        record['permissions_during']=permissions()
        adb('emu','geo','fix','5','52')
        source_command=[sys.executable,'-u',Path(__file__).with_name('run_fm_capture_probe.py'),'--adb',adb_path,'--serial',serial,'--target-apk',args.source_apk,'--probe-apk',args.source_probe,'--output',args.output/'source','--scenarios','integrated_matrix' if args.matrix_count else 'integrated_trigger','--receiver-package','com.google.android.apps.maps','--native-id',args.native_id,'--hold-seconds','20','--confirm-wait-seconds','120']
        if args.matrix_count:source_command+=['--trigger-family',args.trigger_family,'--trigger-pattern',args.trigger_pattern,'--event-count',str(args.matrix_count),'--matrix-timeout-seconds',str(args.matrix_timeout_seconds),'--native-spacing-ms','1000']
        source=launch('source',source_command)
        deadline=time.monotonic()+(args.matrix_timeout_seconds+220 if args.matrix_count else 190)
        while time.monotonic()<deadline:
            try:name,line=events.get(timeout=.2)
            except queue.Empty:
                if all(p.poll() is not None for p in processes.values()):break
                continue
            if name=='source' and line and 'INSTRUMENTATION_STATUS: fm_capture_stage=production_endpoint_ready' in line and receiver is None:
                record['receiver_launch_after_source_ready_monotonic_ns']=time.monotonic_ns()
                receiver_command=[adb_path,'-s',serial,'shell','am','instrument','-w','-r','-e','reviewed_synthetic_choices','true','-e','mode',args.mode,'-e','expected_native_type','INCIDENT' if args.trigger_family=='p0' else 'SPEED_CAM','-e','expected_native_subtype','AMBULANCE' if args.trigger_family=='p0' else 'SPEED_50']
                if args.matrix_count:receiver_command+=['-e','matrix_count',str(args.matrix_count),'-e','matrix_pattern',args.trigger_pattern,'-e','matrix_warning_base',('early/AMBULANCE:' if args.trigger_family=='p0' else 'SpeedCam:')+args.native_id,'-e','matrix_timeout_seconds',str(args.matrix_timeout_seconds)]
                else:receiver_command+=['-e','expected_warning_id','SpeedCam:'+args.native_id]
                if args.observe_pcm:receiver_command+=['-e','observe_pcm','true']
                receiver_command+=['nl.johninnl.warningprobe.integration.maps/nl.johninnl.warningprobe.integration.IntegratedMapsReceiverProbe'];receiver=launch('receiver',receiver_command)
            if line is None and name=='source' and receiver is None:raise RuntimeError('Source ended before production endpoint ready')
            if receiver is not None and source.poll() is not None and receiver.poll() is not None:break
        if any(p.poll() is None for p in processes.values()):raise TimeoutError('Integrated processes did not terminate within bounded run')
        if receiver is None:raise RuntimeError('Receiver was never launched')
        for thread in threads:thread.join(timeout=5)
        text=(args.output/'receiver-output.txt').read_text();match=re.search(r'^INSTRUMENTATION_RESULT: integrated_receiver_result=(.+)$',text,re.M)
        if match:result=json.loads(match.group(1))
        else:
            manifest=re.search(r'^INSTRUMENTATION_RESULT: integrated_receiver_manifest=(.+)$',text,re.M)
            if not manifest:raise RuntimeError('No complete actual receiver result')
            manifest=json.loads(manifest.group(1));remote=manifest['file']
            if remote!='/storage/emulated/0/Android/data/com.google.android.apps.maps/files/native-warning-integrated/matrix-result.json':raise RuntimeError('Unexpected private matrix report path')
            local=args.output/'receiver-full-report.json';adb('pull',remote,local)
            if sha(local)!=manifest['sha256']:raise RuntimeError('Private matrix report bytes differ')
            result=json.loads(local.read_text());record['receiver_manifest']=manifest
        (args.output/'receiver-result.json').write_text(json.dumps(result,indent=2)+'\n')
        if result.get('status')!='observed' or not result.get('preferences_restored_exactly') or not result.get('cleanup_output_quiescent'):raise RuntimeError('Receiver did not prove output and exact restoration')
        if args.observe_pcm:
            pcm=result.get('private_pcm_observer',{});ids={r['utterance_id'] for r in result.get('matrix_speaker_attempts',[])};selected=[r for r in pcm.get('utterances',[]) if r['utterance_id'] in ids]
            good=[r for r in selected if r['starts']==1 and r['done']==1 and r['errors']==0 and r['bytes']>1000 and r['nonzero_bytes']>100]
            if not good or pcm.get('overflow_chunks') or pcm.get('forward_failures') or not (pcm.get('original_listener_restored') or pcm.get('newer_listener_untouched')):raise RuntimeError('Actual measured warning PCM/lifecycle/listener restoration not proven')
            exports=[]
            for value in good:
                remote=value.get('wav','');expected='/storage/emulated/0/Android/data/com.google.android.apps.maps/files/native-warning-integrated/'+value['utterance_id']+'.wav'
                if remote!=expected:raise RuntimeError('Unexpected synthetic warning audio path')
                local=args.output/(value['utterance_id']+'.wav');adb('pull',remote,local);exports.append({'utterance_id':value['utterance_id'],'wav_sha256':sha(local),'path':str(local)})
            record['integrated_pcm_exports']=exports
        source_results=list((args.output/'source').glob('*/result.json'))
        if len(source_results)!=1:raise RuntimeError('Expected exactly one source result')
        source_result=json.loads(source_results[0].read_text())
        if not source_result.get('probe_completed') or not source_result.get('network_and_lease_restored'):raise RuntimeError('Source or its cleanup failed')
        record['source_result']=str(source_results[0]);record['receiver_result']=str(args.output/'receiver-result.json')
    except Exception as error:failure=repr(error)
    finally:
        # Leave source runner alive long enough to execute its verified force-stop/firewall/lease rollback.
        if any(p.poll() is None for p in processes.values()):
            adb('shell','am','force-stop','com.google.android.apps.maps',check=False)
            if receiver is not None:
                try:receiver.wait(timeout=10)
                except subprocess.TimeoutExpired:receiver.kill()
            if source is not None:
                try:source.wait(timeout=65)
                except subprocess.TimeoutExpired:failure=(failure or '')+'; source runner still live, manual verified rollback required'
        try:
            for permission in reversed(changed):adb('shell','pm','revoke','nl.flitsmeister',permission)
            record['permissions_after']=permissions();record['permissions_restored_exactly']=record['permissions_after']==before
            if not record['permissions_restored_exactly']:failure=(failure or '')+'; permission grants/flags differ from baseline'
        except Exception as error:failure=(failure or '')+'; permission restore: '+repr(error)
        finally:
            if args.diagnostic_hidden_api_policy:
                try:
                    if policy_before=='null':adb('shell','settings','delete','global','hidden_api_policy')
                    else:adb('shell','settings','put','global','hidden_api_policy',policy_before)
                    record['diagnostic_hidden_api_policy_after']=adb('shell','settings','get','global','hidden_api_policy').stdout.strip();record['diagnostic_hidden_api_policy_restored_exactly']=record['diagnostic_hidden_api_policy_after']==policy_before
                    if not record['diagnostic_hidden_api_policy_restored_exactly']:failure=(failure or '')+'; diagnostic hiddenAPIpolicy differs from baseline'
                except Exception as error:failure=(failure or '')+'; diagnostic policy restore: '+repr(error)
        record['failure']=failure;record['probe_completed']=failure is None;record['process_exit_codes']={name:p.poll() for name,p in processes.items()}
        (args.output/'result.json').write_text(json.dumps(record,indent=2)+'\n')
    print(json.dumps({'probe_completed':record['probe_completed'],'failure':failure,'result':str(args.output/'result.json')}),flush=True)
    if failure:raise SystemExit(1)


if __name__=='__main__':main()
