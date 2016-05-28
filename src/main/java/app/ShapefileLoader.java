
package app;

import gov.nasa.worldwind.formats.shapefile.Shapefile;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecord;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecordMultiPoint;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecordPoint;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecordPolygon;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecordPolyline;
import gov.nasa.worldwind.formats.shapefile.ShapefileUtils;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfacePolygons;
import gov.nasa.worldwind.render.SurfacePolylines;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.VecBuffer;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.WWMath;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwindx.examples.util.RandomShapeAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ShapefileLoader {
    protected static final RandomShapeAttributes randomAttrs = new RandomShapeAttributes();
    protected int numPolygonsPerLayer = 5000;

    public ShapefileLoader() {
    }

    public Layer createLayerFromSource(Object var1) {
        if(WWUtil.isEmpty(var1)) {
            String var7 = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(var7);
            throw new IllegalArgumentException(var7);
        } else {
            Shapefile var2 = null;
            Layer var3 = null;

            try {
                var2 = new Shapefile(var1);
                var3 = this.createLayerFromShapefile(var2);
            } finally {
                WWIO.closeStream(var2, var1.toString());
            }

            return var3;
        }
    }

    public List<Layer> createLayersFromSource(Object var1) {
        if(WWUtil.isEmpty(var1)) {
            String var7 = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(var7);
            throw new IllegalArgumentException(var7);
        } else {
            Shapefile var2 = null;

            List var3;
            try {
                var2 = new Shapefile(var1);
                var3 = this.createLayersFromShapefile(var2);
            } finally {
                WWIO.closeStream(var2, var1.toString());
            }

            return var3;
        }
    }

    public Layer createLayerFromShapefile(Shapefile var1) {
        if(var1 == null) {
            String var4 = Logging.getMessage("nullValue.ShapefileIsNull");
            Logging.logger().severe(var4);
            throw new IllegalArgumentException(var4);
        } else {
            Object var2 = null;
            if(Shapefile.isPointType(var1.getShapeType())) {
                var2 = new RenderableLayer();
                this.addRenderablesForPoints(var1, (RenderableLayer)var2);
            } else if(Shapefile.isMultiPointType(var1.getShapeType())) {
                var2 = new RenderableLayer();
                this.addRenderablesForMultiPoints(var1, (RenderableLayer)var2);
            } else if(Shapefile.isPolylineType(var1.getShapeType())) {
                var2 = new RenderableLayer();
                this.addRenderablesForPolylines(var1, (RenderableLayer)var2);
            } else if(Shapefile.isPolygonType(var1.getShapeType())) {
                ArrayList var3 = new ArrayList();
                this.addRenderablesForPolygons(var1, var3);
                var2 = (Layer)var3.get(0);
            } else {
                Logging.logger().warning(Logging.getMessage("generic.UnrecognizedShapeType", var1.getShapeType()));
            }

            return (Layer)var2;
        }
    }

    public List<Layer> createLayersFromShapefile(Shapefile var1) {
        if(var1 == null) {
            String var4 = Logging.getMessage("nullValue.ShapefileIsNull");
            Logging.logger().severe(var4);
            throw new IllegalArgumentException(var4);
        } else {
            ArrayList var2 = new ArrayList();
            RenderableLayer var3;
            if(Shapefile.isPointType(var1.getShapeType())) {
                var3 = new RenderableLayer();
                this.addRenderablesForPoints(var1, (RenderableLayer)var3);
                var2.add(var3);
            } else if(Shapefile.isMultiPointType(var1.getShapeType())) {
                var3 = new RenderableLayer();
                this.addRenderablesForMultiPoints(var1, (RenderableLayer)var3);
                var2.add(var3);
            } else if(Shapefile.isPolylineType(var1.getShapeType())) {
                var3 = new RenderableLayer();
                this.addRenderablesForPolylines(var1, (RenderableLayer)var3);
                var2.add(var3);
            } else if(Shapefile.isPolygonType(var1.getShapeType())) {
                this.addRenderablesForPolygons(var1, var2);
            } else {
                Logging.logger().warning(Logging.getMessage("generic.UnrecognizedShapeType", var1.getShapeType()));
            }

            return var2;
        }
    }

    public int getNumPolygonsPerLayer() {
        return this.numPolygonsPerLayer;
    }

    public void setNumPolygonsPerLayer(int var1) {
        if(var1 < 1) {
            String var2 = Logging.getMessage("generic.InvalidSize", new Object[]{Integer.valueOf(var1)});
            Logging.logger().severe(var2);
            throw new IllegalArgumentException(var2);
        } else {
            this.numPolygonsPerLayer = var1;
        }
    }

    protected void addRenderablesForPoints(Shapefile var1, RenderableLayer var2) {
        PointPlacemarkAttributes var3 = this.createPointAttributes((ShapefileRecord)null);

        while(var1.hasNext()) {
            ShapefileRecord var4 = var1.nextRecord();
            if(Shapefile.isPointType(var4.getShapeType())) {
                double[] var5 = ((ShapefileRecordPoint)var4).getPoint();
                var2.addRenderable(this.createPoint(var4, var5[1], var5[0], var3));
            }
        }

    }

    protected void addRenderablesForMultiPoints(Shapefile var1, RenderableLayer var2) {
        PointPlacemarkAttributes var3 = this.createPointAttributes((ShapefileRecord)null);

        while(true) {
            ShapefileRecord var4;
            do {
                if(!var1.hasNext()) {
                    return;
                }

                var4 = var1.nextRecord();
            } while(!Shapefile.isMultiPointType(var4.getShapeType()));

            Iterable var5 = ((ShapefileRecordMultiPoint)var4).getPoints(0);
            Iterator var6 = var5.iterator();

            while(var6.hasNext()) {
                double[] var7 = (double[])var6.next();
                var2.addRenderable(this.createPoint(var4, var7[1], var7[0], var3));
            }
        }
    }

    protected void addRenderablesForPolylines(Shapefile var1, RenderableLayer var2) {
        while(var1.hasNext()) {
            var1.nextRecord();
        }

        ShapeAttributes var3 = this.createPolylineAttributes((ShapefileRecord)null);
        var2.addRenderable(this.createPolyline(var1, var3));
    }

    protected void addRenderablesForPolygons(Shapefile var1, List<Layer> var2) {
        RenderableLayer var3 = new RenderableLayer();
        var2.add(var3);
        int var4 = 0;

        while(var1.hasNext()) {
            try {
                ShapefileRecord var5 = var1.nextRecord();
                var4 = var5.getRecordNumber();
                if(Shapefile.isPolygonType(var5.getShapeType())) {
                    ShapeAttributes var6 = this.createPolygonAttributes(var5);
                    this.createPolygon(var5, var6, var3);
                    if(var3.getNumRenderables() > this.numPolygonsPerLayer) {
                        var3 = new RenderableLayer();
                        var3.setEnabled(false);
                        var2.add(var3);
                    }
                }
            } catch (Exception var7) {
                Logging.logger().warning(Logging.getMessage("SHP.ExceptionAttemptingToConvertShapefileRecord", new Object[]{Integer.valueOf(var4), var7}));
            }
        }

    }

    protected Renderable createPoint(ShapefileRecord var1, double var2, double var4, PointPlacemarkAttributes var6) {
        PointPlacemark var7 = new PointPlacemark(Position.fromDegrees(var2, var4, 0.0D));
        var7.setAltitudeMode(1);
        var7.setAttributes(var6);
        return var7;
    }

    protected Renderable createPolyline(ShapefileRecord var1, ShapeAttributes var2) {
        SurfacePolylines var3 = new SurfacePolylines(Sector.fromDegrees(((ShapefileRecordPolyline)var1).getBoundingRectangle()), var1.getCompoundPointBuffer());
        var3.setAttributes(var2);
        return var3;
    }

    protected Renderable createPolyline(Shapefile var1, ShapeAttributes var2) {
        SurfacePolylines var3 = new SurfacePolylines(Sector.fromDegrees(var1.getBoundingRectangle()), var1.getPointBuffer());
        var3.setAttributes(var2);
        return var3;
    }

    protected void createPolygon(ShapefileRecord var1, ShapeAttributes var2, RenderableLayer var3) {
        Double var4 = this.getHeight(var1);
        if(var4 != null) {
            ExtrudedPolygon var5 = new ExtrudedPolygon(var4);
            var5.setAttributes(var2);
            var3.addRenderable(var5);

            for(int var6 = 0; var6 < var1.getNumberOfParts(); ++var6) {
                VecBuffer var7 = var1.getCompoundPointBuffer().subBuffer(var6);
                if(WWMath.computeWindingOrderOfLocations(var7.getLocations()).equals("gov.nasa.worldwind.avkey.ClockWise")) {
                    if(!var5.getOuterBoundary().iterator().hasNext()) {
                        var5.setOuterBoundary(var7.getLocations());
                    } else {
                        var5 = new ExtrudedPolygon();
                        var5.setAttributes(var2);
                        var5.setOuterBoundary(var1.getCompoundPointBuffer().getLocations());
                        var3.addRenderable(var5);
                    }
                } else {
                    var5.addInnerBoundary(var7.getLocations());
                }
            }
        } else {
            SurfacePolygons var8 = new SurfacePolygons(Sector.fromDegrees(((ShapefileRecordPolygon)var1).getBoundingRectangle()), var1.getCompoundPointBuffer());
            var8.setAttributes(var2);
            var8.setWindingRule("gov.nasa.worldwind.avkey.ClockWise");
            var8.setPolygonRingGroups(new int[]{0});
            var8.setPolygonRingGroups(new int[]{0});
            var3.addRenderable(var8);
        }

    }

    protected Double getHeight(ShapefileRecord var1) {
        return ShapefileUtils.extractHeightAttribute(var1);
    }

    protected PointPlacemarkAttributes createPointAttributes(ShapefileRecord var1) {
        return randomAttrs.nextPointAttributes();
    }

    protected ShapeAttributes createPolylineAttributes(ShapefileRecord var1) {
        return randomAttrs.nextPolylineAttributes();
    }

    protected ShapeAttributes createPolygonAttributes(ShapefileRecord var1) {
        return randomAttrs.nextPolygonAttributes();
    }
}
