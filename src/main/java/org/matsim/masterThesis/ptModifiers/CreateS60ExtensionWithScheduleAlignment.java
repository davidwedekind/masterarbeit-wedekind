package org.matsim.masterThesis.ptModifiers;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import graphql.AssertException;
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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.TransitScheduleValidator;

import java.util.Arrays;
import java.util.List;

/**
 * @author dwedekind
 */

public class CreateS60ExtensionWithScheduleAlignment {
    private static final Logger log = Logger.getLogger(CreateS60ExtensionWithScheduleAlignment.class);

    public static void main(String[] args) {

        CreateS60ExtensionWithScheduleAlignment.Input input = new CreateS60ExtensionWithScheduleAlignment.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Input network file: " + input.networkFile);
        log.info("Input transit schedule file: " + input.transitSchedule);
        log.info("Input transit vehicle file: " + input.transitSchedule);
        log.info("Output file directory: " + input.outputFile);

        Config config = ConfigUtils.createConfig();

        Scenario scenario = ScenarioUtils.loadScenario(config);
        final String epsgCode = "25832";

        config.global().setCoordinateSystem("EPSG:" + epsgCode);
        config.transit().setTransitScheduleFile(input.transitSchedule);
        config.network().setInputFile(input.networkFile);
        config.vehicles().setVehiclesFile(input.transitVehicles);

        new CreateS60ExtensionWithScheduleAlignment().runExtensionModifications(scenario);

        TransitScheduleValidator.ValidationResult resultAfterModifying = TransitScheduleValidator.validateAll(
                scenario.getTransitSchedule(), scenario.getNetwork());
        log.info("Transit validator results after modifying:");
        for (String errorMessage:resultAfterModifying.getErrors()){
            throw new AssertException(errorMessage);
        }

        new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(input.outputFile + "/modifiedSchedule.xml.gz");
        new NetworkWriter(scenario.getNetwork()).write(input.outputFile + "/modifiedNetwork.xml.gz");
    }


    public void runExtensionModifications(Scenario scenario){
        PtUtils utils = new PtUtils(scenario);
        Network network = scenario.getNetwork();
        TransitSchedule tS = scenario.getTransitSchedule();


        // Create additional infrastructure
        // (Rohrer Kurve)
        Node stopGoldberg = network.getNodes().get(Id.createNodeId("tr8005201"));
        Node stopLeinfelden = network.getNodes().get(Id.createNodeId("tr8003622"));

        Link linkTrNew0001 = utils.createLink(Id.createLinkId("trNew0001"), stopGoldberg, stopLeinfelden, TransportMode.pt);
        Link linkTrNew0002 = utils.createLink(Id.createLinkId("trNew0002"), stopLeinfelden, stopGoldberg, TransportMode.pt);

        network.addLink(linkTrNew0001);
        network.addLink(linkTrNew0002);


        // Create additional stop facilities
        TransitStopFacility stopFacilityTr8003622_1 = tS.getFacilities().get(Id.create("8003622.1", TransitStopFacility.class));
        TransitStopFacility stopFacilityTr8003622_2 = utils.createStopFacility(
                Id.create("8003622.2", TransitStopFacility.class),
                stopFacilityTr8003622_1.getCoord(),
                linkTrNew0001.getId(),
                stopFacilityTr8003622_1.getName(),
                stopFacilityTr8003622_1.getStopAreaId());

        TransitStopFacility stopFacilityTr8005201_0 = tS.getFacilities().get(Id.create("8005201", TransitStopFacility.class));
        TransitStopFacility stopFacilityTr8005201_2 = utils.createStopFacility(Id.create(
                "8005201.2", TransitStopFacility.class), stopFacilityTr8005201_0.getCoord(),
                linkTrNew0002.getId(),
                stopFacilityTr8005201_0.getName(),
                stopFacilityTr8005201_0.getStopAreaId());

        tS.addStopFacility(stopFacilityTr8003622_2);
        tS.addStopFacility(stopFacilityTr8005201_2);


        // Specify transit route stops to add on network routes of S60
        List<TransitRouteStop> transitRouteStopsDirFilderstadt = Arrays.asList(
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8001055.6", TransitStopFacility.class)), 240, 420),
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8005201.1", TransitStopFacility.class)), 540, 540),
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8003622.2", TransitStopFacility.class)), 960, 960),
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8001650.1", TransitStopFacility.class)), 1140, 1140),
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8005768.1", TransitStopFacility.class)), 1320, 1380),
               tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8001984.1", TransitStopFacility.class)), 1500, 1500));
        List<TransitRouteStop> transitRouteStopsDirBoeblingen = Arrays.asList(
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8001984", TransitStopFacility.class)), 0, 0),
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8005768", TransitStopFacility.class)), 120, 180),
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8001650", TransitStopFacility.class)), 360, 360),
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8003622", TransitStopFacility.class)), 540, 540),
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8005201.2", TransitStopFacility.class)), 960, 960),
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8001055.4", TransitStopFacility.class)), 1080, 1260));


        // Extend S60
        utils.extendTransitLine(
                "S 60 - 1",
                transitRouteStopsDirBoeblingen,
                transitRouteStopsDirFilderstadt
        );

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
