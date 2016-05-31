package data;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;
import org.apache.commons.io.FileUtils;
import org.postgis.*;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.postgis.Point;
import org.postgis.Polygon;

public class DataCreator {
    public static void downloadFileFromURL(String address, String localFileName) {
        OutputStream out = null;
        URLConnection conn;
        InputStream in = null;

        try {
            URL url = new URL(address);
            out = new BufferedOutputStream(new FileOutputStream(localFileName));
            conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];

            int numRead;

            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
            }

        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    public static void unZipIt(String zipFile, String outputFolder) {

        byte[] buffer = new byte[1024];

        try {
            File folder = new File(outputFolder);
            if (!folder.exists()) {
                folder.mkdir();
            }

            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry ze = zis.getNextEntry();
            String fileName = "";
            while (ze != null) {
                fileName = ze.getName();
                File newFile = new File(outputFolder + File.separator + fileName);

                new File(newFile.getParent()).mkdirs();

                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();
            PrintWriter out = new PrintWriter(outputFolder + File.separator + fileName.replaceFirst("\\.[a-zA-Z]+", ".prj"));
            out.print("GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]]," +
                    "PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]]");
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void shpToDb(File shp, String satname) {
        String cmd = "ogr2ogr -skipfailures -overwrite -f \"PostgreSQL\" PG:\"host=localhost user=postgres dbname=postgres password=postgres\"" +
                " " + shp.getAbsolutePath() + " -nln public." + satname + "fire";
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", cmd);
        try {
            Process p = pb.start();
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

            String s;
//             read the output from the command
            System.out.println("Here is the standard output of the ogr2ogr:\n");
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }

//             read any errors from the attempted command
            System.out.println("Here is the standard error of the ogr2ogr (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public static void downloadAndUpdate(String satname, int days) {
        if (isUpToDate(satname, days)) {
            System.out.println("Updated, satname: " + satname + "; days: " + days);
            return;
        } else
            System.out.println("Not updated, satname: " + satname + "; days: " + days);

        String zipfile = satname + ".zip";
        String url;
        String shapefile;
        if (satname == "modis") {
            if (days == 1) {
                url = "https://firms.modaps.eosdis.nasa.gov/active_fire/c6/shapes/zips/MODIS_C6_Global_24h.zip";
                shapefile = "MODIS_C6_Global_24h.shp";
            } else {
                if (days == 2) {
                    url = "https://firms.modaps.eosdis.nasa.gov/active_fire/c6/shapes/zips/MODIS_C6_Global_48h.zip";
                    shapefile = "MODIS_C6_Global_48h.shp";
                } else {
                    url = "https://firms.modaps.eosdis.nasa.gov/active_fire/c6/shapes/zips/MODIS_C6_Global_7d.zip";
                    shapefile = "MODIS_C6_Global_7d.shp";
                }
            }
        } else {
            if (days == 1) {
                url = "https://firms.modaps.eosdis.nasa.gov/active_fire/viirs/shapes/zips/VNP14IMGTDL_NRT_Global_24h.zip";
                shapefile = "VNP14IMGTDL_NRT_Global_24h.shp";
            } else {
                if (days == 2) {
                    url = "https://firms.modaps.eosdis.nasa.gov/active_fire/viirs/shapes/zips/VNP14IMGTDL_NRT_Global_48h.zip";
                    shapefile = "VNP14IMGTDL_NRT_Global_48h.shp";
                } else {
                    url = "https://firms.modaps.eosdis.nasa.gov/active_fire/viirs/shapes/zips/VNP14IMGTDL_NRT_Global_7d.zip";
                    shapefile = "VNP14IMGTDL_NRT_Global_7d.shp";
                }
            }
        }
        DataCreator.downloadFileFromURL(url, zipfile);
        System.out.println("Downloaded");
        DataCreator.unZipIt(zipfile, satname);
        System.out.println("Unziped");
        File shp = new File(satname, shapefile);
        DataCreator.shpToDb(shp, satname);
        System.out.println("Pushed to DB");
        (new File(zipfile)).delete();
        try {
            FileUtils.deleteDirectory(shp.getParentFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static boolean isUpToDate(String satname, int days) {
        Connection c;
        Statement stmt;
        boolean result = false;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/postgres",
                            "postgres", "postgres");
            c.setAutoCommit(false);

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("select exists(select 1 from " + satname + "fire where current_date <= acq_date+" + days + ");");
            rs.next();
            result = rs.getBoolean(1);

            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return result;
        }
    }

    public static RenderableLayer getPointsLayer(String satname) {
        RenderableLayer result = new RenderableLayer();
        result.setName("points");

        PointPlacemarkAttributes ppattributes = new PointPlacemarkAttributes();
        ppattributes.setUsePointAsDefaultImage(true);
        ppattributes.setLineMaterial(new Material(Color.red));
        ppattributes.setScale(4D);
        try {
            String dburl = "jdbc:postgresql://localhost/postgres";
            Connection conn = DriverManager.getConnection(dburl, "postgres", "postgres");
            Statement stat = conn.createStatement();
//            ResultSet rs = stat.executeQuery("SELECT distinct ST_SnapToGrid(wkb_geometry,0.01) as s from "+satname+"fire where confidence>80");
//            ResultSet rs = stat.executeQuery("select st_x(ST_Centroid(gc)),st_y(ST_Centroid(gc)), ST_NumGeometries(gc) " +
//                    "from (select unnest(ST_ClusterWithin(wkb_geometry, 1)) gc from "+satname+"fire) f");

            String query ;
            if (satname == "modis")
                query = "select gc from\n" +
                        "(select ST_ConvexHull(unnest(ST_ClusterWithin(wkb_geometry, 0.6))) as gc from modisfire where confidence>50) f\n" +
                        "where not (st_area(gc)>0 and st_area(gc)<0.1)";
            else
                query = "select gc from\n" +
                        "(select ST_ConvexHull(unnest(ST_ClusterWithin(wkb_geometry, 0.6))) as gc from viirsfire where confidence='nominal') f\n" +
                        "where not (st_area(gc)>0 and st_area(gc)<0.1)";
            ResultSet rs = stat.executeQuery(query);

            while (rs.next()) {
                Geometry geom = ((PGgeometry) rs.getObject(1)).getGeometry();
                String s = geom.getTypeString();
                if (s == "POLYGON") {
                    ArrayList<LatLon> pos = new ArrayList<LatLon>();
                    for (int i = 0; i < geom.numPoints(); i++) {
                        pos.add(new LatLon(Angle.fromDegrees(geom.getPoint(i).y), Angle.fromDegrees(geom.getPoint(i).x)));
                    }
                    SurfacePolygon sp = new SurfacePolygon(pos);
                    BasicShapeAttributes attributes = new BasicShapeAttributes();
                    attributes.setInteriorMaterial(new Material(new Color(255, 0, 0)));
                    attributes.setOutlineMaterial(new Material(new Color(255, 0, 0)));
                    attributes.setInteriorOpacity(0.5);
                    attributes.setOutlineOpacity(0.5);
                    attributes.setOutlineWidth(4);
                    sp.setAttributes(attributes);
                    result.addRenderable(sp);
                }
                if (s == "POINT") {
                    PointPlacemark p = new PointPlacemark(Position.fromDegrees(
                            ((Point) geom).y, ((Point) geom).x
                    ));
                    p.setAttributes(ppattributes);
                    result.addRenderable(p);
                }
                if (s == "LINESTRING") {
                    for (int i = 0; i < geom.numPoints(); i++) {
                        PointPlacemark p = new PointPlacemark(Position.fromDegrees(
                                geom.getPoint(i).y, geom.getPoint(i).x
                        ));
                        p.setAttributes(ppattributes);
                        result.addRenderable(p);
                    }
                }
            }
            stat.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static RenderableLayer getFireCountries(String satname) {
        RenderableLayer result = new RenderableLayer();
        result.setName("countries");
        try {
            String dburl = "jdbc:postgresql://localhost/postgres";
            Connection conn = DriverManager.getConnection(dburl, "postgres", "postgres");
            Statement stat = conn.createStatement();

//            ResultSet rs = stat.executeQuery("select wkb_geometry from countries");
            String confidence;
            if (satname == "modis")
                confidence = ">50";
            else
                confidence = "=nominal";
            ResultSet rs = stat.executeQuery("select c.wkb_geometry, count(distinct m.wkb_geometry) " +
                    "from countries c left join " + satname + "fire m\n" +
                    "on st_contains(c.wkb_geometry, m.wkb_geometry) " +
                    "where confidence" + confidence +" " +
                    "group by c.wkb_geometry\n" +
                    "having count(distinct m.wkb_geometry) > 0\n");

            while (rs.next()) {
                Polygon p = (Polygon) ((PGgeometry) rs.getObject(1)).getGeometry();
//                for (Polygon p : mp.getPolygons()) {
                    ArrayList<LatLon> pos = new ArrayList<LatLon>();
                    for (int i = 0; i < p.numPoints(); i++) {
                        pos.add(new LatLon(Angle.fromDegrees((p.getPoint(i).y > 180) ? 180 : p.getPoint(i).y),
                                Angle.fromDegrees((p.getPoint(i).x > 180) ? 180 : p.getPoint(i).x)));
                    }
                    SurfacePolygon sp = new SurfacePolygon(pos);

                    BasicShapeAttributes attributes = new BasicShapeAttributes();
//                    double intencity = 15D * rs.getDouble(2) / rs.getDouble(3) + 0.1;
//                    if (intencity > 1)
//                        intencity = 1;

                    attributes.setInteriorMaterial(new Material(new Color(255, 0, 0)));
                    attributes.setInteriorOpacity(0.1);
                    attributes.setOutlineMaterial(new Material(new Color(255, 0, 0)));
                    sp.setAttributes(attributes);
                    result.addRenderable(sp);
//                }
            }
            stat.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void checkCountries(){
        Connection c;
        Statement stmt;
        boolean result = false;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/postgres",
                            "postgres", "postgres");
            c.setAutoCommit(false);

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("select EXISTS (SELECT * \n" +
                    "FROM INFORMATION_SCHEMA.TABLES \n" +
                    "WHERE TABLE_SCHEMA = 'public' \n" +
                    "AND  TABLE_NAME = 'countries')");
            rs.next();
            result = rs.getBoolean(1);
            if (!result)
                createCountries();
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createCountries(){
        String cmd = "ogr2ogr -skipfailures -overwrite -f \"PostgreSQL\" PG:\"host=localhost user=postgres dbname=postgres password=postgres\"" +
                " countries/countries.shp -nln public.countries";
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", cmd);
        try {
            Process p = pb.start();
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

            String s;
            // read the output from the command
//            System.out.println("Here is the standard output of the ogr2ogr:\n");
//            while ((s = stdInput.readLine()) != null) {
//                System.out.println(s);
//            }

            // read any errors from the attempted command
//            System.out.println("Here is the standard error of the ogr2ogr (if any):\n");
//            while ((s = stdError.readLine()) != null) {
//                System.out.println(s);
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
