package app;

import data.DataCreator;
import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.formats.shapefile.Shapefile;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwindx.examples.Placemarks;
import org.apache.commons.io.FileUtils;
import org.postgis.PGgeometry;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static gov.nasa.worldwind.geom.Position.fromDegrees;

/**
 * @author Nastya Loginova
 * @since 5/24/16
 */
public class WorldWindApplication {
    JFrame frame;
    WorldWindowGLCanvas wwd;
    JRadioButton modis;
    JRadioButton viirs;
    JRadioButton oneday;
    JRadioButton twodays;
    JRadioButton sevendays;
    JButton refresh;
    StatusBar statusBar;


    public static void main(String arguments[]) {
//        DataCreator.getCountries();
//        DataCreator.isUpToDate("modis",1);
        new WorldWindApplication().launch();
    }

    public void launch() {
        frame = new JFrame("Active Fire Data Visualization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel globe = new JPanel(new BorderLayout(10, 10));
        JPanel panel = new JPanel(new GridLayout(3, 1));
        JPanel panelSatellites = new JPanel(new GridLayout(2, 1));
        JPanel panelTime = new JPanel(new GridLayout(3, 1));
        JPanel panelActions = new JPanel(new GridLayout(1, 1));

        wwd = new WorldWindowGLCanvas();
        wwd.setModel(new BasicModel());

        modis = new JRadioButton("MODIS 1km", true);
        viirs = new JRadioButton("VIIRS 375m", false);
        Border borderSatellites = BorderFactory.createTitledBorder("Data Source");
        panelSatellites.setBorder(borderSatellites);
        ButtonGroup satellites = new ButtonGroup();
        satellites.add(modis);
        satellites.add(viirs);
        panelSatellites.add(modis);
        panelSatellites.add(viirs);

        oneday = new JRadioButton("Past 24 hours", true);
        twodays = new JRadioButton("Past 48 hours", false);
        sevendays = new JRadioButton("Past 7 days", false);
        Border borderTime = BorderFactory.createTitledBorder("Time period");
        panelTime.setBorder(borderTime);
        ButtonGroup times = new ButtonGroup();
        times.add(oneday);
        times.add(twodays);
        times.add(sevendays);
        panelTime.add(oneday);
        panelTime.add(twodays);
        panelTime.add(sevendays);

        refresh = new JButton("Refresh");
        ButtonGroup actions = new ButtonGroup();
        actions.add(refresh);
        panelActions.add(refresh);

        panel.add(panelSatellites);
        panel.add(panelTime);
        panel.add(panelActions);

        refresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                update();
            }
        });

        globe.add(wwd, BorderLayout.CENTER);
        statusBar = new StatusBar();
        globe.add(statusBar, BorderLayout.PAGE_END);
        statusBar.setEventSource(wwd);

        frame.getContentPane().add(globe, BorderLayout.CENTER);
        frame.getContentPane().add(panel, BorderLayout.WEST);

        frame.pack();
        frame.setVisible(true);

        if (wwd.getModel().getLayers().getLayerByName("countries") == null) {
            ShapefileLoader loader = new ShapefileLoader();
            Shapefile countries = new Shapefile(new File("TM_WORLD_BORDERS_SIMPL-0.3/TM_WORLD_BORDERS_SIMPL-0.3.shp"));
            Layer c = loader.createLayerFromShapefile(countries);
            c.setName("countries");
            c.setOpacity(0.1);

            wwd.getModel().getLayers().add(c);
        }
    }

    public void update(){
        String satname = "modis";
        if (viirs.isSelected())
            satname = "viirs";
        int days = 1;
        if (twodays.isSelected())
            days = 2;
        else
            if (sevendays.isSelected())
                days=7;
        DataCreator.downloadAndUpdate(satname,days);

        File shpfile = new File("m.shp");

        String cmd = "pgsql2shp -f "+shpfile.getAbsolutePath()+" -h localhost " +
                "-u postgres -P postgres postgres \"select * from public."+satname+"fire\"";

        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", cmd);
        try {
            Process p = pb.start();
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));
            String s;
            // read the output from the command
            System.out.println("Here is the standard output of the command:\n");
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }

            // read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
            if (p.waitFor() != 0)
                throw new Exception("Database read problem");
        } catch (Exception e) {
            e.printStackTrace();
        }

        ShapefileLoader loader = new ShapefileLoader();
        Shapefile shp = new Shapefile(shpfile);
        Layer l = loader.createLayerFromShapefile(shp);
        l.setName("mylayer");
        l.setOpacity(0.2);
        Layer last = wwd.getModel().getLayers().getLayerByName("mylayer");
        if (last != null)
            wwd.getModel().getLayers().remove(last);
        wwd.getModel().getLayers().add(l);


//        try {
//            FileUtils.deleteDirectory(shpfile.getParentFile());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


    }


}
