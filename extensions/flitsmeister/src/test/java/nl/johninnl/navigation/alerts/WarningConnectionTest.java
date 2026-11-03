package nl.johninnl.navigation.alerts;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.SystemClock;
import java.util.*;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowProcess;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32,manifest=Config.NONE)
public class WarningConnectionTest {
    WarningEndpoint source;WarningConnection client;
    @After public void close(){if(client!=null)client.close();if(source!=null)source.close();}
    @Test public void authenticatedHandshakeConsumesSnapshotBeforeLiveDelivery() throws Exception {
        ShadowProcess.setUid(20000);Context context=RuntimeEnvironment.getApplication();
        PackageInfo info=new PackageInfo();info.packageName="app.provider";info.applicationInfo=new ApplicationInfo();info.applicationInfo.packageName=info.packageName;info.applicationInfo.uid=20000;
        Shadows.shadowOf(context.getPackageManager()).installPackage(info);
        Set<String> caps=new HashSet<>(Collections.singleton("native_text"));char[] hash=new char[64];Arrays.fill(hash,'a');
        source=new WarningEndpoint(20000,u->u==20000?"app.consumer":null,new WarningCapabilities(new String(hash),caps,true),
                ()->new WarningEndpoint.Storage("boot-count:7",new PlaybackStore(){public PlaybackGrant load(){return null;}public void save(PlaybackGrant g){}},null),SystemClock::elapsedRealtime,()->20000);
        BlockingQueue<WarningConnection.Snapshot> snapshots=new LinkedBlockingQueue<>();BlockingQueue<List<WarningEvent>> deliveries=new LinkedBlockingQueue<>();
        client=new WarningConnection(context,caps,EnumSet.allOf(WarningAudioOwner.Mode.class),new WarningConnection.Listener(){
            public void snapshot(WarningConnection.Snapshot value){snapshots.add(value);}
            public void events(List<WarningEvent> values){deliveries.add(values);}
            public void ownerRevoked(long epoch){}
            public void disconnected(String reason){}
        });
        assertTrue(client.open("app.provider",source.binder()).get(4,TimeUnit.SECONDS));
        assertTrue(client.ping().get(3,TimeUnit.SECONDS));
        WarningConnection.Snapshot initial=snapshots.poll(1,TimeUnit.SECONDS);assertNotNull(initial);assertEquals(0,initial.headSequence);assertTrue(initial.state.isEmpty());
        Bundle e=new Bundle();e.putString("operation","ANNOUNCE");e.putString("warning_id","road-a");e.putLong("revision",1);e.putLong("announcement_index",1);e.putString("phase","notice");e.putString("native_type","Incident");e.putString("category","road_danger");e.putInt("priority",0);e.putString("message","Person on road");e.putString("language_tag","en");e.putString("message_origin","native_text");e.putString("relevance","notice");e.putString("relationship","unknown");e.putLong("observed_elapsed_ms",SystemClock.elapsedRealtime());
        assertTrue(source.emit(e));List<WarningEvent> pushed=deliveries.poll(3,TimeUnit.SECONDS);assertNotNull(pushed);assertEquals("Person on road",pushed.get(0).message);
        WarningConnection.Claim claim=client.claim(WarningAudioOwner.Mode.SILENT,false).get(3,TimeUnit.SECONDS);assertEquals("OK",claim.status);assertNotNull(claim.lease);
        WarningConnection.Attempt muted=client.decide(claim.lease,pushed.get(0),UUID.randomUUID().toString(),PlaybackGrant.Outcome.USER_MUTED).get(3,TimeUnit.SECONDS);
        assertEquals("OK",muted.status);assertEquals(PlaybackGrant.Outcome.USER_MUTED,muted.grant.outcome);
        WarningConnection.Claim voiced=client.claim(WarningAudioOwner.Mode.VOICE,true).get(3,TimeUnit.SECONDS);
        assertTrue(client.supportsPreview());
        WarningConnection.Candidate preview=client.preview(voiced.lease,"nl",UUID.randomUUID().toString()).get(3,TimeUnit.SECONDS);
        assertTrue(Boolean.TRUE.equals(preview.payload.fields().get("is_preview")));assertTrue(preview.payload.message.startsWith("Synthetisch voorbeeld"));
        List<WarningEvent> previewDelivery=deliveries.poll(3,TimeUnit.SECONDS);assertNotNull(previewDelivery);assertEquals(preview.authority.warningId,previewDelivery.get(0).warningId);
        e.putString("warning_id","road-b");e.putLong("observed_elapsed_ms",SystemClock.elapsedRealtime());assertTrue(source.emit(e));
        List<WarningEvent> second=deliveries.poll(3,TimeUnit.SECONDS);assertNotNull(second);
        WarningConnection.Attempt granted=client.begin(voiced.lease,second.get(0),UUID.randomUUID().toString()).get(3,TimeUnit.SECONDS);
        assertEquals("GRANTED",granted.status);
        client.retire();
        assertEquals("OK",client.outcome(granted.grant,PlaybackGrant.Outcome.NOT_STARTED,true).get(3,TimeUnit.SECONDS));
        assertEquals("OK",client.release(voiced.lease).get(3,TimeUnit.SECONDS));
    }
}
