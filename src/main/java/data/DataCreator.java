package data;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
//        -s_srs EPSG:4326 -t_srs EPSG:4326
//        String cmd = "shp2pgsql -s 4326 -d " + shp.getAbsolutePath() + " public."+satname+"fire | psql -h localhost -d postgres -U postgres";
        String cmd = "ogr2ogr -skipfailures -overwrite -f \"PostgreSQL\" PG:\"host=localhost user=postgres dbname=postgres password=postgres\"" +
                " " + shp.getAbsolutePath() + " -nln public." + satname + "fire";
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", cmd);
        try {
            Process p = pb.start();
//            int val = p.waitFor();
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

//            String s;
//            // read the output from the command
//            System.out.println("Here is the standard output of the command:\n");
////            while ((s = stdInput.readLine()) != null) {
////                System.out.println(s);
////            }
//
//            // read any errors from the attempted command
//            System.out.println("Here is the standard error of the command (if any):\n");
//            while ((s = stdError.readLine()) != null) {
//                System.out.println(s);
//            }
//            System.out.println("Exec "+p.waitFor());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void downloadAndUpdate(String satname, int days) {
        if (isUpToDate(satname, days))
            return;

        String zipfile = satname + ".zip";
        String folder = satname;
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
        DataCreator.unZipIt(zipfile, folder);

        File shp = new File(folder, shapefile);
        DataCreator.shpToDb(shp, satname);
        (new File(zipfile)).delete();
        try {
            FileUtils.deleteDirectory(shp.getParentFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isUpToDate(String satname, int days) {

        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/postgres",
                            "postgres", "postgres");
            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("select exists(select 1 from " + satname + "fire where current_date = acq_date+" + days + ");");
            while (rs.next()) {
                return rs.getBoolean(1);
            }
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            return false;
        }

        return false;
    }
}
