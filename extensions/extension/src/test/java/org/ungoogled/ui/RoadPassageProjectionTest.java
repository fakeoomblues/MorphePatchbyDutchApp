package org.ungoogled.ui;
import static org.junit.Assert.*;import org.junit.Test;
public class RoadPassageProjectionTest {
 @Test public void parallelPointWithinLegacySixtyMeterCorridorIsUnknownForPassage(){RoadWarningEngine.Route r=new RoadWarningEngine.Route(new double[]{0,.01},new double[]{0,0});assertNull(r.passageDistance(.005,0,.0044,.0002,3));}
 @Test public void parallelReturnLegMakesNearbyPositionAmbiguous(){RoadWarningEngine.Route r=new RoadWarningEngine.Route(new double[]{0,.01,.01,0},new double[]{0,0,.00005,.00005});assertNull(r.passageDistance(.005,0,.0044,0,3));}
 @Test public void adjacentSegmentsAtOrdinaryVertexAreNotAmbiguous(){RoadWarningEngine.Route r=new RoadWarningEngine.Route(new double[]{0,.005,.01},new double[]{0,0,0});assertEquals(-66.7,r.passageDistance(.005,0,.0044,0,3),1);}
}
