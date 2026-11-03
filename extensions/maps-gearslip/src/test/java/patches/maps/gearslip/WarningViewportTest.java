package patches.maps.gearslip;
import static org.junit.Assert.*;
import android.graphics.Rect;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class) @Config(sdk=32,manifest=Config.NONE)
public class WarningViewportTest {
 @Test public void hostNavigationBarAndStableInsetsRemainOutsideWarningBounds(){assertEquals(new Rect(40,20,780,424),ProjectionRenderer.warningViewport(800,480,new Rect(0,0,800,424),new Rect(40,20,780,424)));}
 @Test public void unspecifiedHostInsetsUseActualSurface(){assertEquals(new Rect(0,0,800,480),ProjectionRenderer.warningViewport(800,480,new Rect(),null));}
}
