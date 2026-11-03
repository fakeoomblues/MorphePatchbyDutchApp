package nl.johninnl.navigation.alerts;
import static org.junit.Assert.*;
import android.content.SharedPreferences;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=32,manifest=Config.NONE)
public class WarningPreferencesTest {
    SharedPreferences prefs;
    @Before public void setup(){prefs=RuntimeEnvironment.getApplication().getSharedPreferences("warnings",0);prefs.edit().clear().commit();}
    @Test public void freshMapsUsesRecommendedVoiceAndRequiresOneExplicitReview(){WarningPreferences p=new WarningPreferences(prefs,true);assertEquals(WarningAudioOwner.Mode.VOICE,p.mode());assertEquals(WarningPreferences.RECOMMENDED,p.categories());assertFalse(p.reviewed());p.confirm();assertTrue(new WarningPreferences(prefs,true).reviewed());}
    @Test public void explicitLegacyPoliceAndMuteRemainSelected(){prefs.edit().putInt("waze_alerts_kinds",1).putBoolean("waze_alerts_chime",false).commit();WarningPreferences p=new WarningPreferences(prefs,true);assertEquals(WarningPreferences.POLICE,p.categories());assertEquals(WarningAudioOwner.Mode.SILENT,p.mode());assertTrue(p.origin().contains("legacy_explicit"));}
    @Test public void explicitLegacyChimeRemainsToneRatherThanEnablingVoice(){prefs.edit().putBoolean("waze_alerts_chime",true).commit();assertEquals(WarningAudioOwner.Mode.TONE,new WarningPreferences(prefs,true).mode());}
    @Test public void absentVelaPreferenceIsNotAnExplicitMute(){WarningPreferences p=new WarningPreferences(prefs,false);assertEquals(WarningAudioOwner.Mode.VOICE,p.mode());assertEquals("recommended",p.origin());}
    @Test public void explicitVelaMuteSurvivesRepeatedInitialization(){prefs.edit().putBoolean("morphe_telemetry_spoken",false).commit();WarningPreferences p=new WarningPreferences(prefs,false);assertEquals(WarningAudioOwner.Mode.SILENT,p.mode());p.categories(WarningPreferences.EMERGENCY);p.confirm();WarningPreferences again=new WarningPreferences(prefs,false);assertEquals(WarningPreferences.EMERGENCY,again.categories());assertTrue(again.reviewed());assertFalse(prefs.getBoolean("morphe_telemetry_spoken",true));}
}
