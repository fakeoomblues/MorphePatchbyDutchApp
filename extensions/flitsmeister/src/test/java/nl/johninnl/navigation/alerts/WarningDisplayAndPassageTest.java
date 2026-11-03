package nl.johninnl.navigation.alerts;
import static org.junit.Assert.*;
import android.os.Bundle;
import android.location.Location;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=32,manifest=Config.NONE)
public class WarningDisplayAndPassageTest {
    final WarningSchedulerTest fixtures=new WarningSchedulerTest();
    WarningEvent state(String id,int priority,long revision){return fixtures.event(id,priority,revision,WarningEvent.Operation.UPSERT);}
    @Test public void multipleUrgentWarningsStayVisibleAndNonwarningContentIsExcluded(){
        WarningView view=new WarningView();WarningEvent camera=state("camera",1,1),behind=state("ambulance behind",0,1),person=state("person on road",0,1);
        Bundle b=WarningCodec.encode(state("parking",0,1));b.putString("category","display_only");view.events(Arrays.asList(camera,behind,person,WarningCodec.decode(b,1000)));
        String shown=view.messages(1100,e->true);assertTrue(shown.contains("ambulance behind"));assertTrue(shown.contains("person on road"));assertFalse(shown.contains("parking"));assertEquals(3,shown.split("\n").length);
    }
    @Test public void staleSnapshotCannotOverwriteANewerRevision(){
        WarningView view=new WarningView();WarningEvent old=state("road",1,1),current=state("road",1,2);view.events(Arrays.asList(old,current));
        view.snapshot(new WarningConnection.Snapshot(old.providerSession,old.sequence,false,true,"UNOWNED",new ArrayList<>(Arrays.asList(old)),new ArrayList<>(),new ArrayList<>(),new ArrayList<>()));
        assertTrue(view.messages(1100,e->true).contains("revision 2"));assertEquals("",view.messages(31000,e->true));
    }
    @Test public void explicitPoliceSelectionMatchesTheActualPinnedNativeEnum(){
        android.content.SharedPreferences prefs=org.robolectric.RuntimeEnvironment.getApplication().getSharedPreferences("native-police",0);prefs.edit().clear().putInt("waze_alerts_kinds",1).commit();WarningPreferences choices=new WarningPreferences(prefs,true);
        Bundle b=WarningCodec.encode(state("police",1,1));b.putString("native_type","POLICEHOTSPOT");assertTrue(choices.enabled(WarningCodec.decode(b,1000)));
        b.putString("category","emergency_vehicle");assertFalse(choices.enabled(WarningCodec.decode(b,1000)));
    }
    @Test public void pointPassageNeedsFreshDirectedEvidenceAndNeverFiltersEmergency(){
        Bundle b=WarningCodec.encode(state("point",1,1));b.putString("relevance","point_ahead");b.putDouble("latitude",0);b.putDouble("longitude",0);b.putLong("position_observed_elapsed_ms",1000);b.putBoolean("position_estimated",false);WarningEvent point=WarningCodec.decode(b,1000);
        Location fix=new Location("gps");fix.setLatitude(.001);fix.setLongitude(0);fix.setAccuracy(5);fix.setSpeed(10);fix.setBearing(0);fix.setElapsedRealtimeNanos(1000000000);
        assertTrue(WarningPassage.passed(point,fix,1100));assertFalse(WarningPassage.passed(point,fix,7000));fix.setLongitude(.001);assertFalse(WarningPassage.passed(point,fix,1100));fix.setLongitude(0);fix.setSpeed(0);assertFalse(WarningPassage.passed(point,fix,1100));
        assertFalse(WarningPassage.passedOnRoute(point,fix,1100,100d));assertTrue(WarningPassage.passedOnRoute(point,fix,1100,-100d));assertFalse(WarningPassage.passedOnRoute(point,fix,1100,null));
        b.putString("relevance","moving_emergency");assertFalse(WarningPassage.passed(WarningCodec.decode(b,1000),fix,1100));assertFalse(WarningPassage.passed(point,null,1100));
    }
    @Test public void producerSpecificFamiliesUseThePromisedAudibleCategory(){
        Bundle b=WarningCodec.encode(state("families",1,1));
        for(String family:new String[]{"service_vehicle","slow_vehicle","roadworks","weather","closure"}){
            b.putString("category",family);assertEquals(family,WarningPreferences.ROAD_DANGER,WarningPreferences.categoryOf(WarningCodec.decode(b,1000)));
        }
        b.putString("category","traffic_camera");assertEquals(WarningPreferences.CAMERA,WarningPreferences.categoryOf(WarningCodec.decode(b,1000)));
        b.putString("category","police_control");assertEquals(WarningPreferences.POLICE,WarningPreferences.categoryOf(WarningCodec.decode(b,1000)));
    }
}
