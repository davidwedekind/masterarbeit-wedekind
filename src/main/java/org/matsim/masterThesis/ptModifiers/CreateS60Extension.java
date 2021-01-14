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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.TransitScheduleValidator;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author dwedekind
 */

public class CreateS60Extension {
    private static final Logger log = Logger.getLogger(CreateS60Extension.class);

    public static void main(String[] args) {

        CreateS60Extension.Input input = new CreateS60Extension.Input();
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


    public void runExtensionModifications(Scenario scenario){
        PtUtils utils = new PtUtils(scenario);
        Network network = scenario.getNetwork();
        TransitSchedule tS = scenario.getTransitSchedule();

        // Create additional infrastructure (Rohrer Kurve)
        Node stopGoldberg = network.getNodes().get(Id.createNodeId("tr8005201"));
        Node stopLeinfelden = network.getNodes().get(Id.createNodeId("tr8003622"));

        Link linkTrNew0001 = utils.createLink("trNew0001", stopGoldberg, stopLeinfelden);
        Link linkTrNew0002 = utils.createLink("trNew0002", stopLeinfelden, stopGoldberg);
        network.addLink(linkTrNew0001);
        network.addLink(linkTrNew0002);

        // Create additional stop facilities
        TransitStopFacility stopFacilityTr8003622_1 = tS.getFacilities().get(Id.create("8003622.1", TransitStopFacility.class));
        TransitStopFacility stopFacilityTr8003622_2 = utils.createStopFacility("8003622.2", stopFacilityTr8003622_1.getCoord(), linkTrNew0001.getId(), stopFacilityTr8003622_1.getName(), stopFacilityTr8003622_1.getStopAreaId());

        TransitStopFacility stopFacilityTr8005201_0 = tS.getFacilities().get(Id.create("8005201", TransitStopFacility.class));
        TransitStopFacility stopFacilityTr8005201_2 = utils.createStopFacility("8005201.2", stopFacilityTr8005201_0.getCoord(), linkTrNew0001.getId(), stopFacilityTr8005201_0.getName(), stopFacilityTr8005201_0.getStopAreaId());

        // Specify links to add on network routes of S60
        List<Id<Link>> linksToAddDirFilderstadt = (Arrays.asList("tr672168", "trNew0001", "tr673031", "tr673032", "tr673033")).stream()
                .map((Function<String, Id<Link>>) Id::createLinkId).collect(Collectors.toList());
        List<Id<Link>> linksToAddDirBoeblingen = (Arrays.asList("tr672997", "tr672998", "tr672999", "tr673000", "trNew0002", "tr672157")).stream()
                .map((Function<String, Id<Link>>) Id::createLinkId).collect(Collectors.toList());

        // Specify transit route stops to add on network routes of S60
        List<TransitRouteStop> transitRouteStopsDirFilderstadt = Arrays.asList(
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8005201.1", TransitStopFacility.class)), 120, 120),
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8003622.2", TransitStopFacility.class)), 540, 540),
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8001650.1", TransitStopFacility.class)), 720, 720),
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8005768.1", TransitStopFacility.class)), 900, 900),
               tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8001984.1", TransitStopFacility.class)), 1020, 1020));
        List<TransitRouteStop> transitRouteStopsDirBoeblingen = Arrays.asList(
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8001984", TransitStopFacility.class)), 0, 0),
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8005768", TransitStopFacility.class)), 120, 120),
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8001650", TransitStopFacility.class)), 300, 300),
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8003622", TransitStopFacility.class)), 480, 480),
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8005201.2", TransitStopFacility.class)), 900, 900),
                tS.getFactory().createTransitRouteStop(tS.getFacilities().get(Id.create("8001055.4", TransitStopFacility.class)), 1080, 1080));

        // Extend S60
        TransitLine lineS60 = tS.getTransitLines().get(Id.create("S 60 - 1", TransitLine.class));
        List<TransitRoute> routesToAdd = new ArrayList<>();
        List<TransitRoute> routesToRemove = new ArrayList<>();

        for (TransitRoute oldRoute: lineS60.getRoutes().values()){
            TransitRoute newRoute;
            if (oldRoute.getStops().get(oldRoute.getStops().size() - 1).getStopFacility().getId()
                    .equals(Id.create("8001055.6", TransitStopFacility.class))){

                newRoute = tS.getFactory().createTransitRoute(
                        oldRoute.getId(),
                        utils.extendNetworkRoute(oldRoute.getRoute().getLinkIds(), linksToAddDirFilderstadt, false),
                        utils.extendRouteStopList(oldRoute.getStops(), transitRouteStopsDirFilderstadt, false),
                        TransportMode.pt);

                for (Departure departure: oldRoute.getDepartures().values()){
                    newRoute.addDeparture(departure);
                }

            } else if (oldRoute.getStops().get(0).getStopFacility().getId()
                    .equals(Id.create("8001055.3", TransitStopFacility.class))){

                final double offSet = transitRouteStopsDirBoeblingen.get(transitRouteStopsDirBoeblingen.size() - 1)
                        .getDepartureOffset().seconds();

                newRoute = tS.getFactory().createTransitRoute(
                        oldRoute.getId(),
                        utils.extendNetworkRoute(oldRoute.getRoute().getLinkIds(), linksToAddDirBoeblingen, true),
                        utils.extendRouteStopList(oldRoute.getStops(), transitRouteStopsDirBoeblingen, true),
                        TransportMode.pt);

                for (Departure departure: oldRoute.getDepartures().values()){
                    if ((departure.getDepartureTime() - offSet) > 0){
                        Departure newDeparture = tS.getFactory().createDeparture(departure.getId(), departure.getDepartureTime() - offSet);
                        newDeparture.setVehicleId(departure.getVehicleId());
                        newRoute.addDeparture(newDeparture);

                    }

                }

            } else {
                continue;

            }

            routesToRemove.add(oldRoute);
            routesToAdd.add(newRoute);

        }

        for (TransitRoute oldRoute: routesToRemove){
            lineS60.removeRoute(oldRoute);
        }

        for (TransitRoute newRoute: routesToAdd){
            lineS60.addRoute(newRoute);
        }

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
