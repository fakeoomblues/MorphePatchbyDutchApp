package org.ungoogled.ui;

import static org.junit.Assert.*;
import nl.johninnl.navigation.alerts.WarningAudioOwner.Mode;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32,manifest=Config.NONE) @LooperMode(LooperMode.Mode.PAUSED)
public class MapsWarningModeTest {
    @Test public void unavailableBindingPreservesOutputIntentAndReportsNotReady(){
        MapsWarningSpeaker speaker=new MapsWarningSpeaker(RuntimeEnvironment.getApplication());
        assertEquals(Mode.VOICE,speaker.effectiveMode(Mode.VOICE));assertEquals(Mode.TONE,speaker.effectiveMode(Mode.TONE));
        assertFalse(speaker.isReady("en",Mode.VOICE));assertFalse(speaker.isReady("und",Mode.TONE));
        assertEquals(Mode.SILENT,speaker.effectiveMode(Mode.SILENT));assertTrue(speaker.isReady("und",Mode.SILENT));speaker.close();
    }
    @Test public void onlyVerifiedNativeMuteCapsAnAudiblePreference(){
        assertEquals(Mode.SILENT,MapsGuidanceCoordinator.capNativeMode(Mode.VOICE,"MUTED"));
        assertEquals(Mode.SILENT,MapsGuidanceCoordinator.capNativeMode(Mode.TONE,"MUTED"));
        assertEquals(Mode.TONE,MapsGuidanceCoordinator.capNativeMode(Mode.VOICE,"MINIMAL"));
        assertEquals(Mode.TONE,MapsGuidanceCoordinator.capNativeMode(Mode.TONE,"MINIMAL"));
        assertEquals(Mode.VOICE,MapsGuidanceCoordinator.capNativeMode(Mode.VOICE,"UNMUTED"));
        assertNull(MapsGuidanceCoordinator.capNativeMode(Mode.VOICE,"UNKNOWN"));
    }
}
