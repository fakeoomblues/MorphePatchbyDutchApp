package nl.johninnl.vela;

import static org.junit.Assert.*;
import android.app.Application;
import android.graphics.*;
import android.os.*;
import java.lang.reflect.Field;
import java.util.*;
import nl.johninnl.navigation.alerts.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32,manifest=Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class CarWarningFrameTest {
    Application app;WarningView view;
    public static class Renderer {
        public boolean I=false,drive=false;public int snapshots;public Paint m0=new Paint(Paint.ANTI_ALIAS_FLAG);
        public void s(){snapshots++;}public Rect u(){return new Rect(25,20,295,210);}public boolean n(){return drive;}public boolean l(){return false;}
    }
    @Before public void setup()throws Exception{
        Field reset=CarUi.class.getDeclaredField("RENDERERS");reset.setAccessible(true);((Map<?,?>)reset.get(null)).clear();reset=CarUi.class.getDeclaredField("lastWarningFrameKey");reset.setAccessible(true);reset.set(null,"");
        app=RuntimeEnvironment.getApplication();app.getSharedPreferences("vela_settings",0).edit().clear().commit();TelemetryRuntime.resetForTest();TelemetryRuntime.init(app,()->{});
        TelemetryRuntime.select(TelemetryStore.FLITSMEISTER);TelemetryRuntime.setCarEnabled(true);
        Field field=TelemetryRuntime.class.getDeclaredField("nativeWarnings");field.setAccessible(true);view=(WarningView)field.get(null);
        field=TelemetryRuntime.class.getDeclaredField("warningPreferences");field.setAccessible(true);WarningPreferences choices=(WarningPreferences)field.get(null);choices.categories(127);choices.confirm();view.events(Collections.singletonList(event("emergency_vehicle")));
    }
    @After public void cleanup(){TelemetryRuntime.resetForTest();}
    private WarningEvent event(String category){long now=SystemClock.elapsedRealtime();Bundle b=new Bundle();b.putInt("protocol_version",1);b.putString("provider_session",UUID.randomUUID().toString());b.putLong("sequence",1);b.putString("operation","ANNOUNCE");b.putString("warning_id","early/AMBULANCE:native");b.putLong("revision",1);b.putLong("announcement_index",1);b.putString("phase","notice");b.putString("native_type","INCIDENT");b.putString("native_subtype","AMBULANCE");b.putString("category",category);b.putInt("priority",0);b.putString("message","Ambulance approaching");b.putString("language_tag","en-US");b.putString("message_origin","native_text");b.putString("relevance","moving_emergency");b.putString("relationship","unknown");b.putLong("observed_elapsed_ms",now);b.putLong("issued_elapsed_ms",now);b.putLong("announce_until_elapsed_ms",now+5000);b.putLong("state_until_elapsed_ms",now+30000);return WarningCodec.decode(b,now);}
    private Bitmap draw(Renderer renderer){Bitmap image=Bitmap.createBitmap(320,240,Bitmap.Config.ARGB_8888);CarUi.warningFrame(renderer,new Canvas(image));return image;}
    private int pixels(Bitmap image){int count=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if(image.getPixel(x,y)!=0)count++;return count;}
    @Test public void browseWarningDrawsWithoutNavigationLocationSpeedOrNativeSpeedDisplay(){Renderer renderer=new Renderer();renderer.m0.setColor(Color.MAGENTA);renderer.m0.setTextSize(11);app.getSharedPreferences("vela_settings",0).edit().putBoolean("car_speed_on",false).commit();Bitmap image=draw(renderer);assertTrue("Native warning must draw on a browsing frame with no GPS/speed fields",pixels(image)>20);assertEquals(Color.MAGENTA,renderer.m0.getColor());assertEquals(11,renderer.m0.getTextSize(),0);for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if(x<25||x>=295||y<20||y>=210)assertEquals("Host control area unchanged",0,image.getPixel(x,y));}
    @Test public void nativeDriveCompactIsNotDuplicatedButDisabledSpeedStillAllowsWarnings(){Renderer renderer=new Renderer();renderer.drive=true;app.getSharedPreferences("vela_settings",0).edit().putBoolean("car_speed_on",true).commit();assertEquals("Existing compact renderer owns the native drive frame",0,pixels(draw(renderer)));app.getSharedPreferences("vela_settings",0).edit().putBoolean("car_speed_on",false).commit();assertTrue("Warnings are independent of native SpeedDisplay",pixels(draw(renderer))>20);}
    @Test public void semanticChangesAndClearRefreshTheActualRendererWithoutAnotherGpsFrame(){Renderer renderer=new Renderer();draw(renderer);CarUi.warningChanged();int arrived=renderer.snapshots;assertTrue(arrived>0);CarUi.warningChanged();assertEquals("Unchanged semantic state does not regenerate snapshots",arrived,renderer.snapshots);view.clear();CarUi.warningChanged();assertEquals("Clear refreshes the original surface without GPS",arrived+1,renderer.snapshots);}
    @Test public void disabledCarCategoriesNativeSelectionAndDisplayOnlyRemainSilentLayers()throws Exception{Renderer renderer=new Renderer();TelemetryRuntime.setCarEnabled(false);assertEquals(0,pixels(draw(renderer)));TelemetryRuntime.setCarEnabled(true);Field field=TelemetryRuntime.class.getDeclaredField("warningPreferences");field.setAccessible(true);WarningPreferences choices=(WarningPreferences)field.get(null);choices.categories(0);assertEquals(0,pixels(draw(renderer)));choices.categories(127);TelemetryRuntime.select("");assertEquals(0,pixels(draw(renderer)));TelemetryRuntime.select(TelemetryStore.FLITSMEISTER);view.clear();view.events(Collections.singletonList(event("display_only")));assertEquals(0,pixels(draw(renderer)));}
}
