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

public class CreateBBLESupershuttle {
    private static final Logger log = Logger.getLogger(CreateBBLESupershuttle.class);

    public static void main(String[] args) {

        CreateBBLESupershuttle.Input input = new CreateBBLESupershuttle.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Input network file: " + input.networkFile);
        log.info("Input transit schedule file: " + input.transitSchedule);
        log.info("Input transit vehicle file: " + input.transitSchedule);
        log.info("Output transit schedule file: " + input.outputFile);

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

        Node stopSindelfingen = network.getNodes().get(Id.createNodeId("tr8005574"));
        Node stopBoeblingen = network.getNodes().get(Id.createNodeId("tr8001920"));
        Node stopLeinfelden = network.getNodes().get(Id.createNodeId("tr8003622"));
        Node stopEchterdingen = network.getNodes().get(Id.createNodeId("tr8001650"));

        Link linkTrNew9001 = utils.createLink(Id.createLinkId("trNew9001"), stopSindelfingen, stopSindelfingen, TransportMode.train);
        Link linkTrNew9002 = utils.createLink(Id.createLinkId("trNew9002"), stopSindelfingen, stopBoeblingen, TransportMode.train);
        Link linkTrNew9003 = utils.createLink(Id.createLinkId("trNew9003"), stopBoeblingen, stopLeinfelden, TransportMode.train);
        Link linkTrNew9004 = utils.createLink(Id.createLinkId("trNew9004"), stopLeinfelden, stopEchterdingen, TransportMode.train);

        Link linkTrNew9005 = utils.createLink(Id.createLinkId("trNew9005"), stopEchterdingen, stopEchterdingen, TransportMode.train);
        Link linkTrNew9006 = utils.createLink(Id.createLinkId("trNew9006"), stopEchterdingen, stopLeinfelden, TransportMode.train);
        Link linkTrNew9007 = utils.createLink(Id.createLinkId("trNew9007"), stopLeinfelden, stopBoeblingen, TransportMode.train);
        Link linkTrNew9008 = utils.createLink(Id.createLinkId("trNew9008"), stopBoeblingen, stopSindelfingen, TransportMode.train);

        network.addLink(linkTrNew9001);
        network.addLink(linkTrNew9002);
        network.addLink(linkTrNew9003);
        network.addLink(linkTrNew9004);
        network.addLink(linkTrNew9005);
        network.addLink(linkTrNew9006);
        network.addLink(linkTrNew9007);
        network.addLink(linkTrNew9008);


        // Create additional stop facilities
        TransitStopFacility stopFacilityTr8005574_91 = utils.createStopFacility(
                Id.create("8005574.91", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8005574")).getCoord(),
                linkTrNew9001.getId(),
                tS.getFacilities().get(Id.create("8005574",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8005574",TransitStopFacility.class)).getStopAreaId()
        );

        TransitStopFacility stopFacilityTr8001920_91 = utils.createStopFacility(
                Id.create("8001920.91", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8001920")).getCoord(),
                linkTrNew9001.getId(),
                tS.getFacilities().get(Id.create("8001920",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8001920",TransitStopFacility.class)).getStopAreaId()
        );

        TransitStopFacility stopFacilityTr8003622_91 = utils.createStopFacility(
                Id.create("8003622.91", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8003622")).getCoord(),
                linkTrNew9001.getId(),
                tS.getFacilities().get(Id.create("8003622",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8003622",TransitStopFacility.class)).getStopAreaId()
        );

        TransitStopFacility stopFacilityTr8001650_91 = utils.createStopFacility(
                Id.create("8001650.91", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8001650")).getCoord(),
                linkTrNew9001.getId(),
                tS.getFacilities().get(Id.create("8001650",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8001650",TransitStopFacility.class)).getStopAreaId()
        );

        TransitStopFacility stopFacilityTr8001650_92 = utils.createStopFacility(
                Id.create("8001650.92", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8001650")).getCoord(),
                linkTrNew9001.getId(),
                tS.getFacilities().get(Id.create("8001650",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8001650",TransitStopFacility.class)).getStopAreaId()
        );

        TransitStopFacility stopFacilityTr8003622_92 = utils.createStopFacility(
                Id.create("8003622.92", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8003622")).getCoord(),
                linkTrNew9001.getId(),
                tS.getFacilities().get(Id.create("8003622",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8003622",TransitStopFacility.class)).getStopAreaId()
        );

        TransitStopFacility stopFacilityTr8001920_92 = utils.createStopFacility(
                Id.create("8001920.92", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8001920")).getCoord(),
                linkTrNew9001.getId(),
                tS.getFacilities().get(Id.create("8001920",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8001920",TransitStopFacility.class)).getStopAreaId()
        );

        TransitStopFacility stopFacilityTr8005574_92 = utils.createStopFacility(
                Id.create("8005574.92", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8005574")).getCoord(),
                linkTrNew9001.getId(),
                tS.getFacilities().get(Id.create("8005574",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8005574",TransitStopFacility.class)).getStopAreaId()
        );

        tS.addStopFacility(stopFacilityTr8005574_91);
        tS.addStopFacility(stopFacilityTr8001920_91);
        tS.addStopFacility(stopFacilityTr8003622_91);
        tS.addStopFacility(stopFacilityTr8001650_91);

        tS.addStopFacility(stopFacilityTr8001650_92);
        tS.addStopFacility(stopFacilityTr8003622_92);
        tS.addStopFacility(stopFacilityTr8001920_92);
        tS.addStopFacility(stopFacilityTr8005574_92);


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
                tS.getFactory().createTransitRouteStop(stopFacilityTr8005574_91, 0, 0),
                tS.getFactory().createTransitRouteStop(stopFacilityTr8001920_91, 60, 60),
                tS.getFactory().createTransitRouteStop(stopFacilityTr8003622_91, 120, 120),
                tS.getFactory().createTransitRouteStop(stopFacilityTr8001650_91, 180, 180)
        );
        List<Id<Link>> links_dir1 = transitStops_dir1.stream()
                .map(transitRouteStop -> transitRouteStop.getStopFacility().getLinkId())
                .collect(Collectors.toList());

        List<TransitRouteStop> transitStops_dir2 = Arrays.asList(
                tS.getFactory().createTransitRouteStop(stopFacilityTr8001650_92, 0, 0),
                tS.getFactory().createTransitRouteStop(stopFacilityTr8003622_92, 60, 60),
                tS.getFactory().createTransitRouteStop(stopFacilityTr8001920_92, 120, 120),
                tS.getFactory().createTransitRouteStop(stopFacilityTr8005574_92, 180, 180)
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
