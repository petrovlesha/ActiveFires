package data;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.*;
import org.apache.commons.io.FileUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.postgis.*;

import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.opengis.feature.Feature;
//import org.geotools.data.*;

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
        //
//        String cmd = "ogr2ogr -skipfailures -append -f \"PostgreSQL\" PG:\"host=localhost user=postgres dbname=postgres password=postgres\"" +
//                " " + shp.getAbsolutePath() + " -nln public." + satname + "fire";
        String cmd = "shp2pgsql -s 4326 -a -g wkb_geometry " + shp.getAbsolutePath() + " public."+satname+"fire " +
                "| psql -h localhost -d postgres -U postgres";
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", cmd);
        try {
            long time = System.currentTimeMillis();
            Process p = pb.start();
            String s;
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

            // read the output from the command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
            System.out.println("Here is the standard output of the command:\n");
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }

            p.waitFor();
            System.out.println(System.currentTimeMillis()-time);
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
        if (satname.equals("modis")) {
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
        writeReport(satname,days);
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
        boolean result;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/postgres",
                            "postgres", "postgres");
            c.setAutoCommit(false);

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery(String.format("select exists(select 1 from buffer " +
                    "where current_date <= acq_date and days>=%d and satname='%s');",days,satname));
            rs.next();
            result = rs.getBoolean(1);

            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            return false;
        }
        return result;

    }

    public static RenderableLayer getPointsLayer(String satname, int days) {
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

            String conf = satname.equals("modis") ? ">50" : "='nominal'";
            String query = String.format("select gc from " +
                    "(select ST_ConvexHull(unnest(ST_ClusterWithin(wkb_geometry, 0.6))) as gc " +
                    "from %sfire where confidence%s and acq_date+1+%d>current_date) f " +
                    "where not (st_area(gc)>0 and st_area(gc)<0.1)",satname,conf,days);

            ResultSet rs = stat.executeQuery(query);

            while (rs.next()) {
                Geometry geom = ((PGgeometry) rs.getObject(1)).getGeometry();
                String s = geom.getTypeString();
                if (s.equals("POLYGON")) {
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
                if (s.equals("POINT")) {
                    PointPlacemark p = new PointPlacemark(Position.fromDegrees(
                            ((Point) geom).y, ((Point) geom).x
                    ));
                    p.setAttributes(ppattributes);
                    result.addRenderable(p);
                }
                if (s.equals("LINESTRING")) {
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

    public static RenderableLayer getFireCountries(String satname,int days) {
        RenderableLayer result = new RenderableLayer();
        result.setName("countries");
        ResultSet rs;
        try {
            String dburl = "jdbc:postgresql://localhost/postgres";
            Connection conn = DriverManager.getConnection(dburl, "postgres", "postgres");
            Statement stat = conn.createStatement();

            String confidence;
            if (satname.equals("modis"))
                confidence = ">50";
            else
                confidence = "=nominal";
            rs = stat.executeQuery(String.format("SELECT ((ST_Dump(ST_Buffer(c.wkb_geometry,0.0))).geom)::geometry(Polygon,4326) geom, " +
                    "COUNT(DISTINCT m.wkb_geometry) FROM countries c LEFT JOIN %sfire m " +
                    "ON ST_Contains(c.wkb_geometry, m.wkb_geometry) " +
                    "WHERE confidence%s  AND acq_date+1+%d>current_date GROUP BY c.wkb_geometry " +
                    "HAVING COUNT(DISTINCT m.wkb_geometry)  > 0", satname, confidence, days));

            while (rs.next()) {
                Polygon p = (Polygon) ((PGgeometry) rs.getObject(1)).getGeometry();
                ArrayList<LatLon> pos = new ArrayList<LatLon>();
                for (int i = 0; i < p.numPoints(); i++) {
                    pos.add(new LatLon(Angle.fromDegrees((p.getPoint(i).y > 180) ? 180 : p.getPoint(i).y),
                            Angle.fromDegrees((p.getPoint(i).x > 180) ? 180 : p.getPoint(i).x)));
                }

                SurfacePolygon sp = new SurfacePolygon(pos);

                BasicShapeAttributes attributes = new BasicShapeAttributes();
                double intencity =  rs.getDouble(2) / 2000 + 0.1;
                if (intencity > 0.8)
                    intencity = 0.8;

                attributes.setInteriorMaterial(new Material(new Color(255, 0, 0)));
                attributes.setInteriorOpacity(intencity);
                attributes.setOutlineMaterial(new Material(new Color(0, 0, 0)));
                sp.setAttributes(attributes);
                result.addRenderable(sp);
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
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/postgres",
                            "postgres", "postgres");
            c.setAutoCommit(false);

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery(String.format("SELECT EXISTS (SELECT * " +
                    "FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE TABLE_SCHEMA = 'public' " +
                    "AND  TABLE_NAME = 'countries')"));
            rs.next();
            boolean result = rs.getBoolean(1);
            rs.close();
            stmt.close();
            c.close();
            if (!result)
                createCountries();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createCountries(){
        String cmd = "ogr2ogr -skipfailures -overwrite -f \"PostgreSQL\" PG:\"host=localhost user=postgres dbname=postgres password=postgres\"" +
                " countries/TM_WORLD_BORDERS_SIMPL-0.3.shp -nln public.countries -nlt MULTIPOLYGON";
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", cmd);
        try {
            Process p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void writeReport(String satname, int days){
        Connection c;
        Statement stmt;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/postgres",
                            "postgres", "postgres");
            c.setAutoCommit(false);


            stmt = c.createStatement();
            String query = String.format("INSERT INTO public.buffer(" +
                    "satname, days, acq_date) " +
                    "VALUES ('%s', %d, current_date) " +
                    "ON CONFLICT (satname) DO UPDATE SET " +
                    "days = %d, acq_date = current_date;", satname, days, days);
            System.out.println(query);
            System.out.println("Wrote report\nAffected rows: "+stmt.executeUpdate(query));
            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static FeatureSource<SimpleFeatureType, SimpleFeature> openShapeFile(String path) throws IOException {
        File file = new File(path);
        Map<String, Object> map = new TreeMap<String, Object>();

        try {
            map.put("url", file.toURI().toURL());
            map.put("create spatial index", Boolean.TRUE);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        ShapefileDataStore dataStore = (ShapefileDataStore) DataStoreFinder.getDataStore(map);
        /*
		 * You can comment out this line if you are using the createFeatureType method (at end of
		 * class file) rather than DataUtilities.createType
		 */
        dataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

        String typeName = dataStore.getTypeNames()[0];
        FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);





        return source;
    }

    private static void uploadFeaturesToPostgis(FeatureSource source) throws IOException {
        Connection c;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/postgres",
                            "postgres", "postgres");
            c.setAutoCommit(false);


            String query = "DELETE FROM PUBLIC.MODISFIRE; ";

            try (FeatureIterator iterator = source.getFeatures().features()) {
                while (iterator.hasNext()) {
                    SimpleFeature feature = (SimpleFeature) iterator.next();
                    query.concat(String.format("INSERT INTO public.modisfire(\n" +
                                    "            gid, latitude, longitude, brightness, scan, track, acq_date, \n" +
                                    "            acq_time, satellite, confidence, version, bright_t31, frp, daynight, \n" +
                                    "            wkb_geometry)\n" +
                                    "    VALUES (?, ?, ?, ?, ?, ?, ?, \n" +
                                    "            ?, ?, ?, ?, ?, ?, ?, \n" +
                                    "            ?);\n",
                            feature.getAttributes(),feature.getBounds()));
                    break;
                }
            }

//            stmt.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
