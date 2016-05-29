package app;

import data.DataCreator;
import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.util.StatusBar;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
    JRadioButton countries;
    JRadioButton points;
    JButton refresh;
    StatusBar statusBar;


    public static void main(String arguments[]) {
        new WorldWindApplication().launch();
    }

    public void launch() {
        frame = new JFrame("Active Fires");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel globe = new JPanel(new BorderLayout(10, 10));
        JPanel panel = new JPanel(new GridLayout(4, 1));
        JPanel panelSatellites = new JPanel(new GridLayout(2, 1));
        JPanel panelTime = new JPanel(new GridLayout(3, 1));
        JPanel panelView = new JPanel(new GridLayout(2, 1));
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

        countries = new JRadioButton("Countries", false);
        points = new JRadioButton("Points", true);
        Border borderView = BorderFactory.createTitledBorder("View");
        panelView.setBorder(borderView);
        ButtonGroup views = new ButtonGroup();
        views.add(countries);
        views.add(points);
        panelView.add(countries);
        panelView.add(points);

        refresh = new JButton("Refresh");
        ButtonGroup actions = new ButtonGroup();
        actions.add(refresh);
        panelActions.add(refresh);

        panel.add(panelSatellites);
        panel.add(panelTime);
        panel.add(panelView);
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

    }

    public void update() {
        String satname = "modis";
        if (viirs.isSelected())
            satname = "viirs";
        int days = 1;
        if (twodays.isSelected())
            days = 2;
        else if (sevendays.isSelected())
            days = 7;
        DataCreator.downloadAndUpdate(satname, days);
        RenderableLayer l;
        if (countries.isSelected())
            l = DataCreator.getFireCountries(satname);
        else
            l = DataCreator.getPointsLayer(satname);
        Layer last = wwd.getModel().getLayers().getLayerByName("countries");
        if (last != null)
            wwd.getModel().getLayers().remove(last);
        last = wwd.getModel().getLayers().getLayerByName("points");
        if (last != null)
            wwd.getModel().getLayers().remove(last);
        wwd.getModel().getLayers().add(l);
    }
}
