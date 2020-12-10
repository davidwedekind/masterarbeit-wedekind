package org.matsim.stuttgart.prepare;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;


/**
 * @author davidwedekind
 */

public class AddAdditionalNetworkAttributes {

    private static final Logger log = Logger.getLogger(AddAdditionalNetworkAttributes.class);

    public static void main(String[] args) {

        AddAdditionalNetworkAttributes.Input input = new AddAdditionalNetworkAttributes.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Input network file: " + input.networkFile);
        log.info("Input shape file: " + input.shapeFile);
        log.info("Output network file: " + input.outputFile);

        // Read-in network
        AddAdditionalNetworkAttributes extender = new AddAdditionalNetworkAttributes();
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Network network = scenario.getNetwork();
        new MatsimNetworkReader(network).readFile(input.networkFile);

        // Do manipulations
        extender.run(scenario, input.shapeFile);

        // Write network output
        new NetworkWriter(scenario.getNetwork()).write(input.outputFile);
    }


    public void run(Scenario scenario, String shapeFile) {

        Network network = scenario.getNetwork();
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);

        log.info("Start merging parking zones to network links...");
        mergeNetworkLinksWithParkingAttributes(network, features);
        log.info("Parking zone merge successful!");

    }


    private void mergeNetworkLinksWithParkingAttributes(Network network, Collection<SimpleFeature> features){


        for (var link : network.getLinks().values()){

            if (!link.getAllowedModes().contains("pt")){

                Coord coord = link.getCoord();
                Point point = MGC.coord2Point(coord);

                double oneHourPCost = 0.;
                double extraHourPCost = 0.;
                double maxDailyPCost = 0.;
                double maxParkingTime = 30.;
                double pFine = 0.;
                double resPCosts = 0.;
                String zoneName = "";
                String zoneGroup = "";


                for (SimpleFeature feature : features ) {
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();

                    if (geometry.covers(point)) {

                        if (feature.getAttribute("zone_name") != null){
                            zoneName = (String) feature.getAttribute("zone_name");
                        }

                        if (feature.getAttribute("zone_group") != null){
                            zoneGroup = (String) feature.getAttribute("zone_group");
                        }

                        if (feature.getAttribute("h_costs") != null){
                            oneHourPCost = (Double) feature.getAttribute("h_costs");
                        }

                        if (feature.getAttribute("h_costs") != null){
                            extraHourPCost = (Double) feature.getAttribute("h_costs");
                        }

                        if (feature.getAttribute("dmax_costs") != null){
                            maxDailyPCost = (Double) feature.getAttribute("dmax_costs");
                        }

                        if (feature.getAttribute("max_time") != null){
                            maxParkingTime = (Double) feature.getAttribute("max_time");
                        }

                        if (feature.getAttribute("penalty") != null){
                            pFine = (Double) feature.getAttribute("penalty");
                        }

                        if (feature.getAttribute("res_costs") != null){
                            resPCosts = (Double) feature.getAttribute("res_costs");
                        }

                        break;
                    }
                }

                link.getAttributes().putAttribute("oneHourPCost", oneHourPCost);
                link.getAttributes().putAttribute("extraHourPCost", extraHourPCost);
                link.getAttributes().putAttribute("maxDailyPCost", maxDailyPCost);
                link.getAttributes().putAttribute("maxPTime", maxParkingTime);
                link.getAttributes().putAttribute("pFine", pFine);
                link.getAttributes().putAttribute("resPCosts", resPCosts);
                link.getAttributes().putAttribute("zoneName", zoneName);
                link.getAttributes().putAttribute("zoneGroup", zoneGroup);

            }



        }

    }


    private static class Input {

        @Parameter(names = "-networkFile")
        private String networkFile;

        @Parameter(names = "-shapeFile")
        private String shapeFile;

        @Parameter(names = "-output")
        private String outputFile;

    }

}
