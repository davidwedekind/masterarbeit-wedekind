package org.matsim.masterThesis.ptModifiers;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.vehicles.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CreateSupershuttle {
    private static final Logger log = Logger.getLogger(CreateSupershuttle.class);

    public static void main(String[] args) {

        CreateSupershuttle.Input input = new CreateSupershuttle.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Input network file: " + input.networkFile);
        log.info("Input transit schedule file: " + input.transitSchedule);
        log.info("Input transit vehicle file: " + input.transitSchedule);
        log.info("Output network file: " + input.outputFile);

        Config config = ConfigUtils.createConfig();

        Scenario scenario = ScenarioUtils.loadScenario(config);
        final String epsgCode = "25832";

        config.global().setCoordinateSystem("EPSG:" + epsgCode);
        config.transit().setTransitScheduleFile(input.transitSchedule);
        config.network().setInputFile(input.networkFile);
        config.vehicles().setVehiclesFile(input.transitVehicles);



        TransitScheduleValidator.ValidationResult resultAfterModifying = TransitScheduleValidator.validateAll(
                scenario.getTransitSchedule(), scenario.getNetwork());
        log.info("Transit validator results after modifying:");
        for (String errorMessage:resultAfterModifying.getErrors()){
            log.error(errorMessage);
        }

        new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(input.outputFile);

    }


    public void runExtensionModifications(Scenario scenario) {
        PtUtils utils = new PtUtils(scenario);
        Network network = scenario.getNetwork();
        TransitSchedule tS = scenario.getTransitSchedule();


        // Create additional infrastructure
        // Create two links from Esslingen Bhf to Boeblingen Bhf and back

        Node stopBoeblingen = network.getNodes().get(Id.createNodeId("tr8001920"));
        Node stopEsslingen = network.getNodes().get(Id.createNodeId("tr8001055"));

        Link linkTrNew9001 = utils.createLink(Id.createLinkId("trNew9001"), stopBoeblingen, stopBoeblingen, TransportMode.train);
        Link linkTrNew9002 = utils.createLink(Id.createLinkId("trNew9002"), stopBoeblingen, stopEsslingen, TransportMode.train);
        Link linkTrNew9003 = utils.createLink(Id.createLinkId("trNew9003"), stopEsslingen, stopEsslingen, TransportMode.train);
        Link linkTrNew9004 = utils.createLink(Id.createLinkId("trNew9004"), stopEsslingen, stopBoeblingen, TransportMode.train);

        network.addLink(linkTrNew9001);
        network.addLink(linkTrNew9002);
        network.addLink(linkTrNew9003);
        network.addLink(linkTrNew9004);


        // Create additional stop facilities
        TransitStopFacility stopFacilityTr8001920_10 = utils.createStopFacility(
                Id.create("8001920.10", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8001920")).getCoord(),
                linkTrNew9001.getId(),
                tS.getFacilities().get(Id.create("8001920",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8001920",TransitStopFacility.class)).getStopAreaId()
        );

        TransitStopFacility stopFacilityTr8001055_10 = utils.createStopFacility(
                Id.create("8001055.10", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8001055")).getCoord(),
                linkTrNew9002.getId(),
                tS.getFacilities().get(Id.create("8001055",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8001055",TransitStopFacility.class)).getStopAreaId()
        );

        TransitStopFacility stopFacilityTr8001055_11 = utils.createStopFacility(
                Id.create("8001055.11", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8001055")).getCoord(),
                linkTrNew9003.getId(),
                tS.getFacilities().get(Id.create("8001055",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8001055",TransitStopFacility.class)).getStopAreaId()
        );

        TransitStopFacility stopFacilityTr8001920_11 = utils.createStopFacility(
                Id.create("8001920.11", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8001920")).getCoord(),
                linkTrNew9004.getId(),
                tS.getFacilities().get(Id.create("8001920",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8001920",TransitStopFacility.class)).getStopAreaId()
        );

        tS.addStopFacility(stopFacilityTr8001920_10);
        tS.addStopFacility(stopFacilityTr8001055_10);
        tS.addStopFacility(stopFacilityTr8001055_11);
        tS.addStopFacility(stopFacilityTr8001920_11);


        // Create a transit vehicle type
        Vehicles vehicles = scenario.getTransitVehicles();
        VehiclesFactory vF = vehicles.getFactory();
        VehicleType type = vF.createVehicleType(Id.create("SuperShuttle-Train", VehicleType.class));
        type.setLength(200);
        VehicleCapacity capacity = type.getCapacity();
        capacity.setSeats(400);
        capacity.setStandingRoom(600);
        type.setPcuEquivalents(0);
        vehicles.addVehicleType(type);
        VehicleUtils.setDoorOperationMode(type, VehicleType.DoorOperationMode.serial);
        VehicleUtils.setAccessTime(type, 2); // 1 person takes 2 seconds to board
        VehicleUtils.setEgressTime(type, 2);


        // Create a transit line
        TransitLine line = scenario.getTransitSchedule().getFactory().createTransitLine(Id.create("SL - 1", TransitLine.class));
        line.setName("SuperShuttle-Line-1");

        List<TransitRouteStop> transitStops_dir1 = Arrays.asList(
                tS.getFactory().createTransitRouteStop(stopFacilityTr8001920_10, 0, 0),
                tS.getFactory().createTransitRouteStop(stopFacilityTr8001055_10, 300, 300)
        );
        List<Id<Link>> links_dir1 = transitStops_dir1.stream()
                .map(transitRouteStop -> transitRouteStop.getStopFacility().getLinkId())
                .collect(Collectors.toList());

        List<TransitRouteStop> transitStops_dir2 = Arrays.asList(
                tS.getFactory().createTransitRouteStop(stopFacilityTr8001055_11, 0, 0),
                tS.getFactory().createTransitRouteStop(stopFacilityTr8001920_11, 300, 300)
                );
        List<Id<Link>> links_dir2 = transitStops_dir2.stream()
                .map(transitRouteStop -> transitRouteStop.getStopFacility().getLinkId())
                .collect(Collectors.toList());

        TransitRoute transitRoute_dir1 = tS.getFactory().createTransitRoute(
                Id.create(1, TransitRoute.class),
                RouteUtils.createNetworkRoute(links_dir1),
                transitStops_dir1,
                "Hyperloop"
        );
        TransitRoute transitRoute_dir2 = tS.getFactory().createTransitRoute(
                Id.create(2, TransitRoute.class),
                RouteUtils.createNetworkRoute(links_dir2),
                transitStops_dir2,
                "Hyperloop"
        );

        for (int i = 0; i < 288; i++){
            double dep = i*300;

            // Create vehicles
            Vehicle veh_1 = vF.createVehicle(Id.create("SL-Train-" + String.format("%03d", i) + "-1", Vehicle.class), type);
            vehicles.addVehicle(veh_1);
            Vehicle veh_2 = vF.createVehicle(Id.create("SL-Train-" + String.format("%03d", i) + "-2", Vehicle.class), type);
            vehicles.addVehicle(veh_2);

            // Create departure
            Departure departure_1 = tS.getFactory().createDeparture(Id.create("SL-Train-" + String.format("%03d", i) + "-1", Departure.class), dep);
            Departure departure_2 = tS.getFactory().createDeparture(Id.create("SL-Train-" + String.format("%03d", i) + "-2", Departure.class), dep);

            departure_1.setVehicleId(veh_1.getId());
            departure_2.setVehicleId(veh_2.getId());

            transitRoute_dir1.addDeparture(departure_1);
            transitRoute_dir2.addDeparture(departure_2);

        }

        line.addRoute(transitRoute_dir1);
        line.addRoute(transitRoute_dir2);

        tS.addTransitLine(line);

    }


    private static class Input {

        @Parameter(names = "-networkFile")
        private String networkFile;

        @Parameter(names = "-transitScheduleFile")
        private String transitSchedule;

        @Parameter(names = "-transitVehicleFile")
        private String transitVehicles;

        @Parameter(names = "-output")
        private String outputFile;

    }

}
