package org.matsim.masterThesis.ptModifiers;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
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
        log.info("Input transit vehicle file: " + input.transitVehicles);
        log.info("Output directory: " + input.outputDir);

        Config config = ConfigUtils.createConfig();


        final String epsgCode = "25832";

        config.global().setCoordinateSystem("EPSG:" + epsgCode);
        config.transit().setTransitScheduleFile(input.transitSchedule);
        config.network().setInputFile(input.networkFile);
        config.vehicles().setVehiclesFile(input.transitVehicles);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        new CreateBBLESupershuttle().runExtensionModifications(scenario);

        TransitScheduleValidator.ValidationResult resultAfterModifying = TransitScheduleValidator.validateAll(
                scenario.getTransitSchedule(), scenario.getNetwork());
        log.info("Transit validator results after modifying:");
        for (String errorMessage:resultAfterModifying.getErrors()){
            log.error(errorMessage);
        }

        new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(input.outputDir + "/outputSchedule.xml.gz");
        new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(input.outputDir + "/outputTransitVehicles.xml.gz");
        new NetworkWriter(scenario.getNetwork()).write(input.outputDir + "/outputNetwork.xml.gz");

    }


    public void runExtensionModifications(Scenario scenario) {
        PtUtils utils = new PtUtils(scenario);
        Network network = scenario.getNetwork();
        TransitSchedule tS = scenario.getTransitSchedule();


        // Create additional infrastructure

        Node stopSindelfingen = network.getNodes().get(Id.createNodeId("tr8005574"));
        Node stopBoeblingen = network.getNodes().get(Id.createNodeId("tr8001055"));
        Node stopLeinfelden = network.getNodes().get(Id.createNodeId("tr8003622"));
        Node stopEchterdingen = network.getNodes().get(Id.createNodeId("tr8001650"));

        Link linkTr99999901 = utils.createLink(Id.createLinkId("tr99999901"), stopSindelfingen, stopSindelfingen, TransportMode.pt);
        Link linkTr99999902 = utils.createLink(Id.createLinkId("tr99999902"), stopSindelfingen, stopBoeblingen, TransportMode.pt);
        Link linkTr99999903 = utils.createLink(Id.createLinkId("tr99999903"), stopBoeblingen, stopLeinfelden, TransportMode.pt);
        Link linkTr99999904 = utils.createLink(Id.createLinkId("tr99999904"), stopLeinfelden, stopEchterdingen, TransportMode.pt);

        Link linkTr99999905 = utils.createLink(Id.createLinkId("tr99999905"), stopEchterdingen, stopEchterdingen, TransportMode.pt);
        Link linkTr99999906 = utils.createLink(Id.createLinkId("tr99999906"), stopEchterdingen, stopLeinfelden, TransportMode.pt);
        Link linkTr99999907 = utils.createLink(Id.createLinkId("tr99999907"), stopLeinfelden, stopBoeblingen, TransportMode.pt);
        Link linkTr99999908 = utils.createLink(Id.createLinkId("tr99999908"), stopBoeblingen, stopSindelfingen, TransportMode.pt);

        network.addLink(linkTr99999901);
        network.addLink(linkTr99999902);
        network.addLink(linkTr99999903);
        network.addLink(linkTr99999904);
        network.addLink(linkTr99999905);
        network.addLink(linkTr99999906);
        network.addLink(linkTr99999907);
        network.addLink(linkTr99999908);


        // Create additional stop facilities
        TransitStopFacility stopFacilityTr8005574_91 = utils.createStopFacility(
                Id.create("8005574.91", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8005574")).getCoord(),
                linkTr99999901.getId(),
                tS.getFacilities().get(Id.create("8005574",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8005574",TransitStopFacility.class)).getStopAreaId()
        );

        TransitStopFacility stopFacilityTr8001920_91 = utils.createStopFacility(
                Id.create("8001920.91", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8001920")).getCoord(),
                linkTr99999902.getId(),
                tS.getFacilities().get(Id.create("8001920",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8001920",TransitStopFacility.class)).getStopAreaId()
        );

        TransitStopFacility stopFacilityTr8003622_91 = utils.createStopFacility(
                Id.create("8003622.91", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8003622")).getCoord(),
                linkTr99999903.getId(),
                tS.getFacilities().get(Id.create("8003622",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8003622",TransitStopFacility.class)).getStopAreaId()
        );

        TransitStopFacility stopFacilityTr8001650_91 = utils.createStopFacility(
                Id.create("8001650.91", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8001650")).getCoord(),
                linkTr99999904.getId(),
                tS.getFacilities().get(Id.create("8001650",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8001650",TransitStopFacility.class)).getStopAreaId()
        );

        TransitStopFacility stopFacilityTr8001650_92 = utils.createStopFacility(
                Id.create("8001650.92", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8001650")).getCoord(),
                linkTr99999905.getId(),
                tS.getFacilities().get(Id.create("8001650",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8001650",TransitStopFacility.class)).getStopAreaId()
        );

        TransitStopFacility stopFacilityTr8003622_92 = utils.createStopFacility(
                Id.create("8003622.92", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8003622")).getCoord(),
                linkTr99999906.getId(),
                tS.getFacilities().get(Id.create("8003622",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8003622",TransitStopFacility.class)).getStopAreaId()
        );

        TransitStopFacility stopFacilityTr8001920_92 = utils.createStopFacility(
                Id.create("8001920.92", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8001920")).getCoord(),
                linkTr99999907.getId(),
                tS.getFacilities().get(Id.create("8001920",TransitStopFacility.class)).getName(),
                tS.getFacilities().get(Id.create("8001920",TransitStopFacility.class)).getStopAreaId()
        );

        TransitStopFacility stopFacilityTr8005574_92 = utils.createStopFacility(
                Id.create("8005574.92", TransitStopFacility.class),
                network.getNodes().get(Id.createNodeId("tr8005574")).getCoord(),
                linkTr99999908.getId(),
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

        @Parameter(names = "-outputDir")
        private String outputDir;

    }

}
