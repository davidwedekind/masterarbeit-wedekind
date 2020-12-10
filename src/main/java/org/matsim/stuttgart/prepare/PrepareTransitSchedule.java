package org.matsim.stuttgart.prepare;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.feature.simple.SimpleFeature;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Adds fare zone and bike-and-ride attribute to transit stop facilities
 *
 * @author davidwedekind
 */

public class PrepareTransitSchedule {

    private static final Logger log = Logger.getLogger(PrepareTransitSchedule.class);


    public static void main(String[] args) {

        PrepareTransitSchedule.Input input = new PrepareTransitSchedule.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Input transit schedule file: " + input.scheduleFile);
        log.info("Input shape file: " + input.shapeFile);
        log.info("Output transit schedule file: " + input.outputFile);

        // Parse Transit Schedule
        PrepareTransitSchedule preparer = new PrepareTransitSchedule();
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new TransitScheduleReader(scenario).readFile(input.scheduleFile);

        // Do manipulations
        preparer.run(scenario, input.shapeFile);

        // Write network output
        new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(input.outputFile);


    }


    public void run(Scenario scenario, String shapeFile) {

        TransitSchedule schedule = scenario.getTransitSchedule();
        Collection<SimpleFeature> fareZoneFeatures = ShapeFileReader.getAllFeatures(shapeFile);

        log.info("Start manipulating transit facility attributes ...");
        // Create map with all bikeAndRideAssignments in vvs area
        Set<Id<TransitStopFacility>> bikeAndRideAssignment = tagBikeAndRide(schedule);

        for (var transitStopFacility: schedule.getFacilities().values()){

            String fareZone = findFareZone(transitStopFacility, fareZoneFeatures);
            transitStopFacility.getAttributes().putAttribute("ptFareZone", fareZone);

            // Facilities in vvs zone are considered only
            if (fareZone.equals("out")){
                transitStopFacility.getAttributes().putAttribute("VVSBikeAndRide", false);

            }else{
                if (bikeAndRideAssignment.contains(transitStopFacility.getId())){
                    transitStopFacility.getAttributes().putAttribute("VVSBikeAndRide", true);
                }else{
                    transitStopFacility.getAttributes().putAttribute("VVSBikeAndRide", false);
                }

            }

        }

        log.info("Fare zones manipulated successfully!");

    }


    private String findFareZone(TransitStopFacility transitStopFacility, Collection<SimpleFeature> features) {

        // Facilities which are not whithin the fare zone shapes are located outside of vvs area and thus marked accordingly
        String fareZone = "out";

        Coord homeCoord = transitStopFacility.getCoord();
        Point point = MGC.coord2Point(homeCoord);

        for (SimpleFeature feature : features ) {

            Geometry geometry = (Geometry) feature.getDefaultGeometry();

            if (geometry.covers(point)) {
                fareZone = feature.getAttribute("FareZone").toString();
            }

        }

        return fareZone;

    }


    private Set<Id<TransitStopFacility>> tagBikeAndRide(TransitSchedule schedule) {

        // This method writes all stops in a list that have departures of mode "tram" or "train"
        // It is assumed that at tram or train stations Bike-and-Ride is possible

        var modes = Set.of(TransportMode.train, "tram");

        return schedule.getTransitLines().values().stream()
                .flatMap(line -> line.getRoutes().values().stream())
                .filter(route -> modes.contains(route.getTransportMode()))
                        .flatMap(route -> route.getStops().stream())
                        .map(stop -> stop.getStopFacility().getId())
                        .collect(Collectors.toSet());

    }


    private static class Input {

        @Parameter(names = "-scheduleFile")
        private String scheduleFile;

        @Parameter(names = "-shapeFile")
        private String shapeFile;

        @Parameter(names = "-output")
        private String outputFile;

    }

}
