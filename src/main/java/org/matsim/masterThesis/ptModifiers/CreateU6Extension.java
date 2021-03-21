package org.matsim.masterThesis.ptModifiers;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.opengis.feature.simple.SimpleFeature;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author dwedekind
 */

public class CreateU6Extension {
    private static final Logger log = Logger.getLogger(CreateU6Extension.class);

    public static void main(String[] args) {

        CreateU6Extension.Input input = new CreateU6Extension.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Input network file: " + input.networkFile);
        log.info("Input transit schedule file: " + input.transitSchedule);
        log.info("Input transit vehicle file: " + input.transitSchedule);
        log.info("Input shape file: " + input.shapeFile);
        log.info("Output network file: " + input.outputFile);

        Config config = ConfigUtils.createConfig();

        Scenario scenario = ScenarioUtils.loadScenario(config);
        final String epsgCode = "25832";

        config.global().setCoordinateSystem("EPSG:" + epsgCode);
        config.transit().setTransitScheduleFile(input.transitSchedule);
        config.network().setInputFile(input.networkFile);
        config.vehicles().setVehiclesFile(input.transitVehicles);

        CreateU6Extension extension = new CreateU6Extension();
        extension.runExtensionModifications(scenario, input.shapeFile);

        TransitScheduleValidator.ValidationResult resultAfterModifying = TransitScheduleValidator.validateAll(
                scenario.getTransitSchedule(), scenario.getNetwork());
        log.info("Transit validator results after modifying:");
        for (String errorMessage:resultAfterModifying.getErrors()){
            log.error(errorMessage);
        }

        new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(input.outputFile);

    }


    public void runExtensionModifications(Scenario scenario, String shapeFilePath){
        PtUtils utils = new PtUtils(scenario);
        Network network = scenario.getNetwork();
        TransitSchedule tS = scenario.getTransitSchedule();


        // Read data from shape file
        final Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFilePath);

        // Create additional infrastructure

        // Create stop nodes if not exist
        for (SimpleFeature feature: features){
            Id<Node> nodeId = Id.createNodeId(feature.getAttribute("node2").toString());

            if (! network.getNodes().containsKey(nodeId)){
                MultiLineString multiLineString = (MultiLineString) feature.getDefaultGeometry();
                LineString lastVertex = (LineString) multiLineString.getGeometryN(multiLineString.getNumGeometries() - 1);
                Node node = network.getFactory().createNode(
                        nodeId,
                        new Coord(lastVertex.getEndPoint().getCoordinate().x, lastVertex.getEndPoint().getCoordinate().y)
                );
                network.addNode(node);

            }

        }


        // Create links if not exist
        for (SimpleFeature feature: features){
            Id<Link> linkId = Id.createLinkId(feature.getAttribute("linkId").toString());

            if (! network.getLinks().containsKey(linkId)){
                Link link = utils.createLink(
                        linkId,
                        network.getNodes().get(Id.createNodeId(feature.getAttribute("node1").toString())),
                        network.getNodes().get(Id.createNodeId(feature.getAttribute("node2").toString())),
                        "pt"
                );
                network.addLink(link);

            }

        }


        // Create additional stop facilities
        for (SimpleFeature feature: features){
            Id<TransitStopFacility> stopFacilityId = Id.create(feature.getAttribute("tStopFac").toString(), TransitStopFacility.class);

            if (! tS.getFacilities().containsKey(stopFacilityId)){
                Node node = network.getNodes().get(Id.createNodeId(feature.getAttribute("node2").toString()));
                TransitStopFacility stop = utils.createStopFacility(
                        stopFacilityId,
                        node.getCoord(),
                        Id.createLinkId(feature.getAttribute("linkId").toString()),
                        feature.getAttribute("tStopName").toString(),
                        Id.create(node.getId().toString(), TransitStopArea.class)
                );
                tS.addStopFacility(stop);

            }

        }


        // Create Transit Routes
        TreeMap<Integer, TransitRouteStop> transitRouteStopsDir0 = new TreeMap<>();
        TreeMap<Integer, TransitRouteStop> transitRouteStopsDir1 = new TreeMap<>();

        for (SimpleFeature feature: features){
            TransitStopFacility transitStop = tS.getFacilities().get(Id.create(feature.getAttribute("tStopFac").toString(), TransitStopFacility.class));
            int seq = Integer.parseInt(feature.getAttribute("seq").toString());
            double arrOffset = Double.parseDouble(feature.getAttribute("arrOffset").toString());
            double depOffset = Double.parseDouble(feature.getAttribute("depOffset").toString());
            TransitRouteStop routeStop = tS.getFactory().createTransitRouteStop(transitStop, arrOffset, depOffset);

            if (Integer.parseInt(feature.getAttribute("dir").toString())==0){
                transitRouteStopsDir0.put(seq, routeStop);

            } else {
                transitRouteStopsDir1.put(seq, routeStop);

            }

        }


        // Extend U6
        utils.extendTransitLine(
                "STB U6 - 1",
                new ArrayList<>(transitRouteStopsDir0.values()),
                new ArrayList<>(transitRouteStopsDir1.values())
        );

    }


    private static class Input {

        @Parameter(names = "-networkFile")
        private String networkFile;

        @Parameter(names = "-transitScheduleFile")
        private String transitSchedule;

        @Parameter(names = "-transitVehicleFile")
        private String transitVehicles;

        @Parameter(names = "-shapeFile")
        private String shapeFile;

        @Parameter(names = "-output")
        private String outputFile;

    }

}
