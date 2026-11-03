package nl.johninnl.navigation.alerts;

import static org.junit.Assert.*;
import android.os.Bundle;
import java.util.*;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32,manifest=Config.NONE)
public class WarningSchedulerTest {
    final String session=UUID.randomUUID().toString();
    final Time time=new Time();final Policy policy=new Policy();final Speaker speaker=new Speaker();
    final List<PlaybackGrant.Outcome> observed=new ArrayList<>();
    Source source;WarningScheduler scheduler;long sequence;
    @Before public void setup(){source=new Source();scheduler=new WarningScheduler(time,policy,speaker,new WarningScheduler.Observer(){public void changed(String status){}public void outcome(WarningEvent event,PlaybackGrant.Outcome value){observed.add(value);}});scheduler.connected(source,snapshot(Collections.emptyList()));}
    @Test public void nativeEmergencyWithoutGeometryStartsImmediatelyAndEnqueueIsNotCompletion(){
        WarningEvent e=event("ambulance",0,1,WarningEvent.Operation.ANNOUNCE);source.add(e);scheduler.events(source,Arrays.asList(e));
        assertEquals(1,speaker.calls);assertNotNull(source.pending);assertFalse(observed.contains(PlaybackGrant.Outcome.COMPLETED));
        assertEquals(PlaybackGrant.Outcome.GRANTED,source.pending.outcome);
        speaker.callback.started(speaker.grant);assertEquals(PlaybackGrant.Outcome.STARTED,source.pending.outcome);
        speaker.callback.finished(speaker.grant,PlaybackGrant.Outcome.COMPLETED,true);assertNull(source.pending);assertTrue(observed.contains(PlaybackGrant.Outcome.COMPLETED));
        scheduler.events(source,Arrays.asList(e));assertEquals(1,speaker.calls);
    }
    @Test public void p0WaitsForPositiveCancellationAndItsAckBeforeReplacingLowerPriorityAudio(){
        WarningEvent ordinary=event("camera",1,1,WarningEvent.Operation.ANNOUNCE);source.add(ordinary);scheduler.events(source,Arrays.asList(ordinary));speaker.callback.started(speaker.grant);
        WarningScheduler.Callback old=speaker.callback;PlaybackGrant oldGrant=speaker.grant;
        WarningEvent urgent=event("person",0,1,WarningEvent.Operation.ANNOUNCE);source.add(urgent);scheduler.events(source,Arrays.asList(urgent));
        assertEquals(1,speaker.cancels);assertEquals(1,speaker.calls);
        old.finished(oldGrant,PlaybackGrant.Outcome.UNCERTAIN,false);assertEquals(1,speaker.calls);assertNotNull(source.pending);
        old.finished(oldGrant,PlaybackGrant.Outcome.INTERRUPTED,true);assertEquals(2,speaker.calls);assertEquals("person",speaker.event.warningId);
    }
    @Test public void upsertAndSnapshotStateDoNotInventAudioAndRevisedPayloadRetainsOriginalDeadline(){
        WarningEvent a=event("road",1,1,WarningEvent.Operation.ANNOUNCE);source.add(a);speaker.ready=false;scheduler.events(source,Arrays.asList(a));
        time.now+=100;WarningEvent update=event("road",1,2,WarningEvent.Operation.UPSERT);source.add(update);scheduler.events(source,Arrays.asList(update));
        speaker.ready=true;scheduler.refresh();assertEquals(1,speaker.calls);assertEquals(2,speaker.event.revision);assertEquals(a.announceUntil,speaker.deadline);
        speaker.callback.finished(speaker.grant,PlaybackGrant.Outcome.COMPLETED,true);
        WarningEvent state=event("state-only",0,1,WarningEvent.Operation.UPSERT);source.add(state);scheduler.events(source,Arrays.asList(state));assertEquals(1,speaker.calls);
    }
    @Test public void repeatedStateUpdatesDoNotCancelIdenticalAudioAlreadyAcceptedByTheSpeaker(){
        WarningEvent authority=event("road",1,1,WarningEvent.Operation.ANNOUNCE);source.add(authority);scheduler.events(source,Collections.singletonList(authority));
        PlaybackGrant granted=speaker.grant;
        for(int revision=2;revision<=4;revision++){
            time.now+=25;Bundle fields=WarningCodec.encode(event("road",1,revision,WarningEvent.Operation.UPSERT));
            fields.putString("message",authority.message);fields.putDouble("latitude",52+revision*.00001);fields.putDouble("longitude",5);
            fields.putLong("position_observed_elapsed_ms",time.now);fields.putString("position_clock_origin","native_decision");fields.putBoolean("position_estimated",false);
            WarningEvent state=WarningCodec.decode(fields,time.now);source.add(state);scheduler.events(source,Collections.singletonList(state));
        }
        assertEquals("Unchanged speech must not consume cancellation/retry attempts",0,speaker.cancels);
        assertEquals(1,speaker.calls);assertEquals(granted.attemptId,speaker.grant.attemptId);
        assertEquals("The submitted immutable payload still matches its durable grant",granted.revision,speaker.event.revision);
        assertEquals(authority.announceUntil,speaker.deadline);
        speaker.callback.started(granted);speaker.callback.finished(granted,PlaybackGrant.Outcome.COMPLETED,true);
        assertNull(source.pending);assertTrue(observed.contains(PlaybackGrant.Outcome.COMPLETED));
    }
    @Test public void startArrivingAfterCancellationIsStillPersistedAndNeverReplayed(){
        WarningEvent authority=event("road",1,1,WarningEvent.Operation.ANNOUNCE);source.add(authority);scheduler.events(source,Collections.singletonList(authority));
        PlaybackGrant original=speaker.grant;WarningScheduler.Callback originalCallback=speaker.callback;
        time.now+=25;WarningEvent changed=event("road",1,2,WarningEvent.Operation.UPSERT);source.add(changed);scheduler.events(source,Collections.singletonList(changed));
        assertEquals(1,speaker.cancels);
        originalCallback.started(original);
        assertTrue("An actual start remains durable even when cancellation was already requested",source.pending.started);
        originalCallback.finished(original,PlaybackGrant.Outcome.INTERRUPTED,true);time.now+=250;scheduler.refresh();
        assertEquals("An interrupted started warning must not obtain replacement audio",1,speaker.calls);
        assertNull(source.pending);
    }
    @Test public void changedUnsaidMessageStillCancelsBeforeReplacementAndRetainsOriginalDeadline(){
        WarningEvent authority=event("road",1,1,WarningEvent.Operation.ANNOUNCE);source.add(authority);scheduler.events(source,Collections.singletonList(authority));
        PlaybackGrant original=speaker.grant;WarningScheduler.Callback originalCallback=speaker.callback;
        time.now+=25;WarningEvent changed=event("road",1,2,WarningEvent.Operation.UPSERT);source.add(changed);scheduler.events(source,Collections.singletonList(changed));
        assertEquals(1,speaker.cancels);assertEquals(1,speaker.calls);
        originalCallback.finished(original,PlaybackGrant.Outcome.NOT_STARTED,true);time.now+=250;scheduler.refresh();
        assertEquals(2,speaker.calls);assertEquals(changed.message,speaker.event.message);assertEquals(changed.revision,speaker.event.revision);
        assertNotEquals(original.attemptId,speaker.grant.attemptId);assertEquals(authority.announceUntil,speaker.deadline);
    }
    @Test public void clearAndSourceOffFenceLateGrantBeforeAnySpeakerCall(){
        source.delayBegin=true;WarningEvent e=event("road",0,1,WarningEvent.Operation.ANNOUNCE);source.add(e);scheduler.events(source,Arrays.asList(e));
        assertEquals(0,speaker.calls);scheduler.disconnected();source.beginReply.complete(source.delayedReply);
        assertEquals(0,speaker.calls);assertNull(source.pending);assertTrue(observed.contains(PlaybackGrant.Outcome.NOT_STARTED));
    }
    @Test public void silentOwnerAndFilteredCategoriesNeverInvokeSpeaker(){
        policy.mode=WarningAudioOwner.Mode.SILENT;policy.enabled=false;
        WarningEvent e=event("filtered",0,1,WarningEvent.Operation.ANNOUNCE);source.add(e);scheduler.events(source,Arrays.asList(e));assertEquals(0,speaker.calls);assertTrue(observed.contains(PlaybackGrant.Outcome.USER_FILTERED));
        policy.enabled=true;WarningEvent second=event("muted",0,1,WarningEvent.Operation.ANNOUNCE);source.add(second);scheduler.events(source,Arrays.asList(second));assertEquals(0,speaker.calls);assertTrue(observed.contains(PlaybackGrant.Outcome.USER_MUTED));
    }
    @Test public void expiredReplayAndUnsupportedSourceCoverageNeverReachSpeaker(){
        WarningEvent e=event("late",0,1,WarningEvent.Operation.ANNOUNCE);source.add(e);time.now=e.announceUntil+1;scheduler.events(source,Arrays.asList(e));assertEquals(0,speaker.calls);assertTrue(observed.contains(PlaybackGrant.Outcome.EXPIRED));
        scheduler.connected(source,new WarningConnection.Snapshot(session,sequence,false,false,"UNOWNED",new ArrayList<>(),new ArrayList<>(),new ArrayList<>(),new ArrayList<>()));
        WarningEvent valid=event("not-covered",0,1,WarningEvent.Operation.ANNOUNCE);source.add(valid);scheduler.events(source,Arrays.asList(valid));assertEquals(0,speaker.calls);assertEquals("COVERAGE_INCOMPLETE",scheduler.status());
    }
    @Test public void urgentSecondWarningDisplacesAdviceAtTheSixteenEntryBound(){
        speaker.ready=false;
        for(int i=0;i<18;i++){WarningEvent e=event("advice-"+i,2,1,WarningEvent.Operation.ANNOUNCE);source.add(e);scheduler.events(source,Arrays.asList(e));}
        WarningEvent urgent=event("urgent-behind",0,1,WarningEvent.Operation.ANNOUNCE);source.add(urgent);scheduler.events(source,Arrays.asList(urgent));
        assertTrue(observed.contains(PlaybackGrant.Outcome.REJECTED));speaker.ready=true;scheduler.refresh();
        assertEquals(1,speaker.calls);assertEquals("urgent-behind",speaker.event.warningId);
    }
    @Test public void distinctNativeWarningsHaveNoGlobalCooldownAndARealRepeatGetsNewOutput(){
        WarningEvent first=event("first",0,1,WarningEvent.Operation.ANNOUNCE);source.add(first);scheduler.events(source,Arrays.asList(first));speaker.callback.started(speaker.grant);speaker.callback.finished(speaker.grant,PlaybackGrant.Outcome.COMPLETED,true);
        WarningEvent second=event("second",0,1,WarningEvent.Operation.ANNOUNCE);source.add(second);scheduler.events(source,Arrays.asList(second));assertEquals(2,speaker.calls);speaker.callback.started(speaker.grant);speaker.callback.finished(speaker.grant,PlaybackGrant.Outcome.COMPLETED,true);
        Bundle repeat=WarningCodec.encode(event("first",0,1,WarningEvent.Operation.ANNOUNCE));repeat.putLong("announcement_index",2);WarningEvent nativeRepeat=WarningCodec.decode(repeat,time.now);source.add(nativeRepeat);scheduler.events(source,Arrays.asList(nativeRepeat));assertEquals(3,speaker.calls);
    }
    @Test public void expiredOwnerCancelsOutputAndNeverGrantsAnotherBeforeActualStop(){
        WarningEvent e=event("road",0,1,WarningEvent.Operation.ANNOUNCE);source.add(e);scheduler.events(source,Arrays.asList(e));
        time.now+=6001;scheduler.refresh();assertEquals(1,speaker.cancels);assertNotNull(source.pending);assertEquals(1,speaker.calls);
        speaker.callback.finished(speaker.grant,PlaybackGrant.Outcome.INTERRUPTED,true);assertNull(source.pending);
    }
    @Test public void explicitPreviewUsesTheSourceGrantAndIsNotAnActualWarning(){
        policy.enabled=false;CompletableFuture<String> result=scheduler.preview("en");
        assertEquals(1,speaker.calls);assertTrue(Boolean.TRUE.equals(speaker.event.fields().get("is_preview")));assertNotNull(source.pending);assertFalse(result.isDone());
        scheduler.refresh();assertEquals(0,speaker.cancels);
        speaker.callback.started(speaker.grant);speaker.callback.finished(speaker.grant,PlaybackGrant.Outcome.COMPLETED,true);
        assertEquals("COMPLETED",result.join());assertNull(source.pending);
        WarningView view=new WarningView();view.events(Arrays.asList(speaker.event));assertEquals("",view.messages(time.now,e->true));
    }
    @Test public void mutedPreviewDoesNotCreateAnAuthorityOrInitializeOutput(){
        policy.mode=WarningAudioOwner.Mode.SILENT;assertEquals("USER_MUTED",scheduler.preview("nl").join());assertEquals(0,speaker.calls);assertEquals(0,sequence);
    }
    WarningConnection.Snapshot snapshot(List<WarningConnection.Candidate> candidates){return new WarningConnection.Snapshot(session,sequence,false,true,"UNOWNED",new ArrayList<>(),new ArrayList<>(),candidates,new ArrayList<>());}
    WarningEvent event(String id,int priority,long revision,WarningEvent.Operation operation){
        Bundle b=new Bundle();b.putInt("protocol_version",1);b.putString("provider_session",session);b.putLong("sequence",++sequence);b.putString("operation",operation.name());b.putString("warning_id",id);b.putLong("revision",revision);b.putLong("announcement_index",1);b.putString("phase","notice");b.putString("native_type","Incident");b.putString("category","road_danger");b.putInt("priority",priority);b.putString("message","Native "+id+" revision "+revision);b.putString("language_tag","en");b.putString("message_origin","native_text");b.putString("relevance","notice");b.putString("relationship","unknown");b.putLong("observed_elapsed_ms",time.now);b.putLong("issued_elapsed_ms",time.now);b.putLong("state_until_elapsed_ms",time.now+30000);if(operation==WarningEvent.Operation.ANNOUNCE)b.putLong("announce_until_elapsed_ms",time.now+(priority==0?5000:10000));return WarningCodec.decode(b,time.now);
    }
    static class Time implements WarningScheduler.Time {long now=1000;public long now(){return now;}public void execute(Runnable task){task.run();}public void after(Runnable task,long ms){} }
    static class Policy implements WarningScheduler.Policy {boolean enabled=true;WarningAudioOwner.Mode mode=WarningAudioOwner.Mode.VOICE;public boolean active(){return true;}public WarningAudioOwner.Mode mode(){return mode;}public boolean enabled(WarningEvent event){return enabled;}public boolean passed(WarningEvent event,long now){return false;} }
    static class Speaker implements WarningScheduler.Speaker {boolean ready=true;int calls,cancels;WarningScheduler.Callback callback;PlaybackGrant grant;WarningEvent event;long deadline;public void prepare(String language){}public boolean ready(String language,WarningAudioOwner.Mode mode){return ready||mode==WarningAudioOwner.Mode.SILENT;}public WarningAudioOwner.Mode effective(WarningAudioOwner.Mode mode){return mode;}public boolean speak(WarningEvent e,PlaybackGrant g,long until,WarningScheduler.Callback cb){calls++;event=e;grant=g;callback=cb;deadline=until;return true;}public void cancel(PlaybackGrant g){cancels++;} }
    class Source implements WarningScheduler.Transport {
        PlaybackGrant pending;boolean delayBegin;CompletableFuture<WarningConnection.Attempt> beginReply;WarningConnection.Attempt delayedReply;
        final WarningJournal journal=new WarningJournal(session);final WarningCapabilities profile=new WarningCapabilities(repeat('a',64),Collections.singleton("native_text"),true);
        final WarningAudioOwner owner=new WarningAudioOwner("boot-count:7",session,journal,new PlaybackStore(){public PlaybackGrant load(){return pending;}public void save(PlaybackGrant g){pending=g;}},profile);
        final WarningAudioOwner.Client client=new WarningAudioOwner.Client(10001,"app.client",UUID.randomUUID().toString(),UUID.randomUUID().toString(),profile.required,EnumSet.allOf(WarningAudioOwner.Mode.class),profile.digest);
        void add(WarningEvent e){assertEquals(WarningJournal.Append.ACCEPTED,journal.append(e,time.now));}
        public String identity(){return "app.provider";}
        public CompletableFuture<WarningConnection.Claim> claim(WarningAudioOwner.Mode mode,boolean ready){return CompletableFuture.completedFuture(claimValue(owner.claim(10001,client,mode,ready,time.now)));}
        public CompletableFuture<WarningConnection.Claim> renew(WarningConnection.Lease lease,WarningAudioOwner.Mode mode,boolean ready){return CompletableFuture.completedFuture(claimValue(owner.renew(10001,client,lease.token,lease.epoch,mode,ready,time.now)));}
        WarningConnection.Claim claimValue(WarningAudioOwner.Claim result){Bundle b=WarningWire.status(result.code.name());if(result.lease!=null){Bundle l=new Bundle();l.putString("owner_token",result.lease.token);l.putLong("owner_epoch",result.lease.epoch);l.putLong("until_elapsed_ms",result.lease.until);l.putString("mode",result.lease.mode.name());l.putBoolean("ready",result.lease.ready);b.putBundle("lease",l);}return new WarningConnection.Claim(b);}
        public CompletableFuture<WarningConnection.Attempt> begin(WarningConnection.Lease lease,WarningEvent e,String nonce){WarningConnection.Attempt reply=attempt(owner.begin(10001,client,lease.token,lease.epoch,e.warningId,e.phase,e.announcementIndex,e.revision,nonce,time.now));if(!delayBegin)return CompletableFuture.completedFuture(reply);delayedReply=reply;beginReply=new CompletableFuture<>();return beginReply;}
        public CompletableFuture<WarningConnection.Attempt> decide(WarningConnection.Lease lease,WarningEvent e,String nonce,PlaybackGrant.Outcome decision){return CompletableFuture.completedFuture(attempt(owner.decide(10001,client,lease.token,lease.epoch,e.warningId,e.phase,e.announcementIndex,nonce,decision,time.now)));}
        WarningConnection.Attempt attempt(WarningAudioOwner.GrantResult r){Bundle b=WarningWire.status(r.code.name());if(r.grant!=null)b.putBundle("grant",WarningWire.grant(r.grant));if(r.announcement!=null){b.putBundle("authority",WarningCodec.encode(r.announcement.authority));b.putBundle("payload",WarningCodec.encode(r.announcement.payload));}return new WarningConnection.Attempt(b,time.now);}
        public CompletableFuture<String> outcome(PlaybackGrant g,PlaybackGrant.Outcome o,boolean quiet){return CompletableFuture.completedFuture(owner.outcome(10001,client.consumerNonce,g,o,quiet,time.now).name());}
        public CompletableFuture<String> release(WarningConnection.Lease lease){return CompletableFuture.completedFuture(lease==null?"OK":owner.release(10001,client,lease.token,lease.epoch,time.now).name());}
        public CompletableFuture<WarningConnection.Candidate> preview(WarningConnection.Lease lease,String language,String nonce){
            Bundle b=WarningCodec.encode(event("preview",2,1,WarningEvent.Operation.ANNOUNCE));b.putString("category","preview");b.putBoolean("is_preview",true);b.putString("preview_nonce",nonce);b.putString("message","Synthetic preview. Emergency vehicle behind.");WarningEvent e=WarningCodec.decode(b,time.now);add(e);return CompletableFuture.completedFuture(new WarningConnection.Candidate(e,e));
        }
    }
    static String repeat(char c,int count){char[] v=new char[count];Arrays.fill(v,c);return new String(v);}
}
