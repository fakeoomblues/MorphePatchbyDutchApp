package org.ungoogled.ui;

import static org.junit.Assert.*;
import android.content.Context;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33,manifest=Config.NONE)
public class ExternalTelemetryCarTest {
    Context app;Object first=new Object(),second=new Object();
    private void session(Object owner,boolean visible)throws Exception{ExternalTelemetry.class.getMethod("carSession",Context.class,Object.class,boolean.class).invoke(null,app,owner,visible);Shadows.shadowOf(Looper.getMainLooper()).idle();}
    private void surface(Object owner,ViewGroup view)throws Exception{ExternalTelemetry.class.getMethod("carSurface",Object.class,ViewGroup.class).invoke(null,owner,view);Shadows.shadowOf(Looper.getMainLooper()).idle();}
    private boolean active()throws Exception{Field f=ExternalTelemetry.class.getDeclaredField("startup");f.setAccessible(true);Object client=f.get(null);return client!=null&&(Boolean)client.getClass().getMethod("activeSession").invoke(client);}
    @Before public void setup(){app=RuntimeEnvironment.getApplication();app.getSharedPreferences(Shapes.PREFS,0).edit().clear().commit();ExternalTelemetry.select(app,TelemetryStore.WAZE);}
    @After public void cleanup()throws Exception{try{session(first,false);session(second,false);}catch(NoSuchMethodException absent){}ExternalTelemetry.select(app,"");}
    @Test public void realCarLifecycleKeepsSelectedProviderActiveWithoutPhoneActivity()throws Exception{
        assertFalse(active());session(first,true);assertTrue(active());session(second,true);session(first,false);assertTrue(active());session(second,false);assertFalse(active());
    }
    @Test public void temporarySurfaceLossDoesNotEndCarSessionAndFinalCloseRemovesWidget()throws Exception{
        FrameLayout root=new FrameLayout(app);session(first,true);surface(first,root);assertEquals(1,root.getChildCount());
        surface(first,null);assertTrue(active());assertEquals(0,root.getChildCount());surface(first,root);assertEquals(1,root.getChildCount());session(first,false);assertFalse(active());assertEquals(0,root.getChildCount());
    }
    @Test public void widgetFitsActualCarSurfaceWhenNativeDecorExtendsBelowVisibleViewport()throws Exception{
        FrameLayout root=new FrameLayout(app);root.layout(0,0,800,480);session(first,true);
        ExternalTelemetry.class.getMethod("carSurface",Object.class,ViewGroup.class,int.class,int.class).invoke(null,first,root,800,424);Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1,root.getChildCount());android.view.View value=root.getChildAt(0);FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)value.getLayoutParams();
        assertTrue("Warning panel must end above the real supplied surface bottom",lp.topMargin+value.getMeasuredHeight()<=424);
    }

}
