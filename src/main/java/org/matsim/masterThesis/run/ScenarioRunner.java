package org.matsim.masterThesis.run;

import graphql.AssertException;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.masterThesis.BanCarsFromSmallerStreets;
import org.matsim.masterThesis.analyzer.*;
import org.matsim.masterThesis.prep.CleanPopulationAfterCalibration;
import org.matsim.masterThesis.ptModifiers.*;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.stuttgart.ptFares.PtFaresConfigGroup;
import org.matsim.stuttgart.run.StuttgartMasterThesisRunner;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author dwedekind
 */

public class ScenarioRunner {

    public static void main(String[] args) throws URISyntaxException {
        final Logger log = Logger.getLogger(ScenarioRunner.class);

        for (String arg : args) {
            log.info( arg );
        }

        Config config = ScenarioRunner.prepareConfig( args );
        Scenario scenario = ScenarioRunner.prepareScenario( config );

        ScenarioRunner.validateModifications( scenario );

        final Controler controler = ScenarioRunner.prepareControler( scenario );
        controler.run() ;

        ScenarioRunner.postprocessing( controler );
    }


    public static Config prepareConfig( String [] args, ConfigGroup... customModules ) {
        final Logger log = Logger.getLogger(ScenarioRunner.class);
        log.info("START CONFIG PREPARATION (SCENARIO RUNNER)");

        // Read all scenario relevant parameters from config input
        Config config = StuttgartMasterThesisRunner.prepareConfig(args);
        // After calibration, ride should be excluded from subtour mode choice
        List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
        modes.remove("ride");
        config.subtourModeChoice().setModes(modes.toArray(new String[0]));

        log.info("FINISH CONFIG PREPARATION (SCENARIO RUNNER)");
        log.info("------------");
        return config;
    }


    public static Scenario prepareScenario( Config config ) throws URISyntaxException {
        final Logger log = Logger.getLogger(ScenarioRunner.class);
        log.info("START SCENARIO PREPARATION (SCENARIO RUNNER)");

        Path configPath = Paths.get(config.getContext().toURI()).getParent();

        // In addition to the calibration case, master thesis experimental config group is needed
        // for receiving some additional scenario parameters
        StuttgartMasterThesisExperimentalConfigGroup thesisExpConfigGroup = ConfigUtils.addOrGetModule(config, StuttgartMasterThesisExperimentalConfigGroup.class);

        Scenario scenario = StuttgartMasterThesisRunner.prepareScenario(config);

        // Clean-up plans
        new CleanPopulationAfterCalibration().clean(scenario);

        // If needed for scenario, ban cars from smaller streets
        if (thesisExpConfigGroup.getReducedCarInfrastructureShapeFile() != null){
            new BanCarsFromSmallerStreets(scenario.getNetwork()).run(
                    configPath.resolve(thesisExpConfigGroup.getReducedCarInfrastructureShapeFile()).toString());

        }

        // If needed for scenario, perform the relevant pt modifications
        if (thesisExpConfigGroup.getSupershuttleExtension()){
            new CreateBBLESupershuttle().runExtensionModifications(scenario);
        }

        if (thesisExpConfigGroup.getS60Extension()){
            new CreateS60Extension().runExtensionModifications(scenario);

        }

        if (thesisExpConfigGroup.getU6ExtensionShapeFile() != null){
            new CreateU6Extension().runExtensionModifications(scenario, configPath.resolve(thesisExpConfigGroup.getU6ExtensionShapeFile()).toString());

            PtUtils modifier = new PtUtils(scenario);
            modifier.removeLine("Bus 122 - 9");

            List<String> stopsToCancel = Arrays.asList("557985", "557983", "557997", "557980", "561004", "555473", "555472", "561003", "557977", "560458", "560457");
            modifier.shortenLine("Bus 120 - 11", stopsToCancel);
        }

        if (thesisExpConfigGroup.getTaktAlignment()){
            alignTakts(scenario);
        }

        if (thesisExpConfigGroup.getConnectionImprovement()) {
            log.info("Move bus stops towards sbahn stations");

            // Move bus stops towards s-bahn stations for minimal walk times
            moveStopTowardsOtherStop(scenario, "561562", "8001650");
            moveStopTowardsOtherStop(scenario, "561573", "8003622");
            moveStopTowardsOtherStop(scenario, "562223", "8001984");
            moveStopTowardsOtherStop(scenario, "421958", "8001055");
            moveStopTowardsOtherStop(scenario, "555742", "8001055");
            moveStopTowardsOtherStop(scenario, "561333", "8005574");

        }




        // Finally include fare Zones and parking shapes according to the scenario
        // and finish the scenario
        StuttgartMasterThesisRunner.finishScenario(
                scenario,
                configPath.resolve(thesisExpConfigGroup.getFareZoneShapeFile()).toString(),
                configPath.resolve(thesisExpConfigGroup.getParkingZoneShapeFile()).toString()
        );

        log.info("FINISH SCENARIO PREPARATION (SCENARIO RUNNER)");
        log.info("------------");
        return scenario;
    }

    private static void moveStopTowardsOtherStop(Scenario scenario, String stopGroup1, String stopGroup2) {
        String node1 = "tr" + stopGroup1;
        String node2 = "tr" + stopGroup2;

        Coord targetCoord = scenario.getNetwork().getNodes().get(Id.createNodeId(node2)).getCoord();

        // First move network node
        scenario.getNetwork().getNodes().get(Id.createNodeId(node1)).setCoord(targetCoord);

        // Then move pt facilities
        scenario.getTransitSchedule().getFacilities().values().stream()
                .filter(transitStopFacility -> transitStopFacility.getId().toString().startsWith(stopGroup1))
                .forEach(transitStopFacility -> transitStopFacility.setCoord(targetCoord));

    }


    private static void alignTakts(Scenario scenario) {
        TaktModifier modifier = new TaktModifier(scenario);

        // Double Takt on line S60
        double newTakt = 15*60;
        TransitLine lineS60 = scenario.getTransitSchedule().getTransitLines().get(Id.create("S 60 - 1", TransitLine.class));

        for (int i = 1; i<=3; i++){
            TransitRoute route = lineS60.getRoutes().get(Id.create(i, TransitRoute.class));
            modifier.doubleTakt(route, newTakt, 999990000 + i * 100);
        }


        // Double Takt on bus lines Sindelfingen and Boeblingen

        TransitLine line706 = scenario.getTransitSchedule().getTransitLines().get(Id.create("Bus 706 - 8", TransitLine.class));
        for (int i = 1; i<=7; i++){
            TransitRoute route = line706.getRoutes().get(Id.create(i, TransitRoute.class));
            modifier.doubleTakt(route, newTakt, 999990400 + i * 20);
        }


        TransitLine line723 = scenario.getTransitSchedule().getTransitLines().get(Id.create("Bus 723 - 7", TransitLine.class));
        TransitRoute line723route1 = line723.getRoutes().get(Id.create("1", TransitRoute.class));

        double start = (7 * 60 * 60) + (20 * 60);
        double end = (11 * 60 * 60);
        modifier.doubleTakt(line723route1, newTakt, start, end, 999990600);

        start = (12 * 60 * 60);
        end = (20 * 60 * 60);
        modifier.doubleTakt(line723route1, newTakt, start, end, 999990700);


        TransitLine line726 = scenario.getTransitSchedule().getTransitLines().get(Id.create("Bus 726 - 7", TransitLine.class));
        TransitRoute line726route1 = line726.getRoutes().get(Id.create("1", TransitRoute.class));

        newTakt = 30*60;
        start = (6 * 60 * 60);
        end = (11 * 60 * 60);
        modifier.doubleTakt(line726route1, newTakt, start, end, 999990800);

        start = (12 * 60 * 60);
        end = (20 * 60 * 60);
        modifier.doubleTakt(line726route1, newTakt, start, end, 999990900);

        newTakt = 15*60;
        start = (6 * 60 * 60);
        end = (20 * 60 * 60);
        modifier.doubleTakt(line726route1, newTakt, start, end, 999991000);

        // Double Takt on bus lines in Leinfelden and Echterdingen
        TransitLine line38 = scenario.getTransitSchedule().getTransitLines().get(Id.create("Bus 38 - 17", TransitLine.class));
        TransitRoute line38route3 = line38.getRoutes().get(Id.create("3", TransitRoute.class));
        TransitRoute line38route4 = line38.getRoutes().get(Id.create("4", TransitRoute.class));

        newTakt = 10*60;
        start = (6 * 60 * 60);
        end = (7 * 60 * 60 + 30 * 60);
        modifier.doubleTakt(line38route3, newTakt, start, end, 999991100);

        newTakt = 15*60;
        start = (7 * 60 * 60 + 30 * 60);
        end = (21 * 60 * 60);
        modifier.doubleTakt(line38route3, newTakt, start, end, 999991200);

        start = (6 * 60 * 60);
        end = (21 * 60 * 60);
        modifier.doubleTakt(line38route4, newTakt, start, end, 999991300);


    }


    public static void validateModifications( Scenario scenario ){
        final Logger log = Logger.getLogger(ScenarioRunner.class);
        log.info("START VALIDATION (SCENARIO RUNNER)");

        final Config config = scenario.getConfig();

        StuttgartMasterThesisExperimentalConfigGroup thesisExpConfigGroup =
                (StuttgartMasterThesisExperimentalConfigGroup) config.getModules().get(StuttgartMasterThesisExperimentalConfigGroup.GROUP_NAME);

        // Validate transit schedule
        TransitScheduleValidator.ValidationResult resultAfterModifying = TransitScheduleValidator.validateAll(
                scenario.getTransitSchedule(), scenario.getNetwork());
        log.info("Transit validator results after modifying:");
        for (String errorMessage:resultAfterModifying.getErrors()){
            throw new AssertException(errorMessage);
        }
        log.info("No issues...");


        // ---- REPORT OF SCENARIO RELEVANT SETTINGS BEFORE RUN ----
        log.info("------------");
        log.info("---- SCENARIO RELEVANT CONFIG PARAMS AND SETTINGS ----");
        log.info("RunId: " + config.controler().getRunId());
        log.info("PlansFile: " + config.plans().getInputFile());

        // Bike and Ride
        log.info("BIKE AND RIDE");
        log.info("Bike Teleported Mode Speed [m/s]: " + config.plansCalcRoute().getModeRoutingParams().get(TransportMode.bike).getTeleportedModeSpeed().toString());
        IntermodalTripFareCompensatorsConfigGroup compensatorsCfg =
                (IntermodalTripFareCompensatorsConfigGroup) config.getModules().get(IntermodalTripFareCompensatorsConfigGroup.GROUP_NAME);

        OptionalDouble optionalTripCompensation = compensatorsCfg.getIntermodalTripFareCompensatorConfigGroups().stream()
                .filter(var1 -> var1.getDrtModesAsString().equals(TransportMode.bike))
                .mapToDouble(IntermodalTripFareCompensatorConfigGroup::getCompensationMoneyPerTrip)
                .findFirst();
        if (optionalTripCompensation.isPresent()) {
            log.info("Bike compensation money per (intermodal) trip:" + optionalTripCompensation.getAsDouble());
        } else {
            log.info("Bike compensation money per (intermodal) trip: 0");
        }
        log.info("Chain based modes: " + Arrays.toString(config.subtourModeChoice().getChainBasedModes()));

        // Fares
        log.info("FARES");
        log.info("Fare Zone Shape File: " + thesisExpConfigGroup.getFareZoneShapeFile());
        log.info("Fare Zone prices ... ");


        PtFaresConfigGroup configFares =
                (PtFaresConfigGroup) config.getModules().get(PtFaresConfigGroup.GROUP);

        Map<Integer, Double> allFares = configFares.getFaresGroup().getFaresAsMap();
        for (Map.Entry<Integer, Double> fare: allFares.entrySet()){
            log.info(String.format("Fare - %1$d zone(s): %2$f", fare.getKey(), fare.getValue()));
        }

        // Parking
        log.info("PARKING");
        log.info("Parking Zone Shape File: " + thesisExpConfigGroup.getParkingZoneShapeFile());

        // Reduced Street Network
        log.info("REDUCED STREET NETWORK");
        log.info("Reduced street network Shape File: " + thesisExpConfigGroup.getReducedCarInfrastructureShapeFile());

        // Pt Extension
        log.info("PT EXTENSION");
        log.info("S60 Extension: " + thesisExpConfigGroup.getS60Extension());
        log.info("U6 Extension: " + (thesisExpConfigGroup.getU6ExtensionShapeFile() == null ? "false" : thesisExpConfigGroup.getU6ExtensionShapeFile()));
        log.info("SuperShuttle Extension: " + (thesisExpConfigGroup.getSupershuttleExtension()));
        log.info("Takt Alignment Extension: " + (thesisExpConfigGroup.getTaktAlignment()));

        log.info("FINISH VALIDATION (SCENARIO RUNNER)");
        log.info("------------");
    }


    public static Controler prepareControler( Scenario scenario ) {
        Gbl.assertNotNull(scenario);
        return StuttgartMasterThesisRunner.prepareControler(scenario);
    }


    public static void postprocessing( Controler controler ) {
        final Logger log = Logger.getLogger(ScenarioRunner.class);
        log.info("START POST-PROCESSING");

        Link2PersonListAnalyzer link2PersonListAnalyzer = new Link2PersonListAnalyzer(controler.getScenario());
        PTRevenueAnalyzer ptRevenueAnalyzer = new PTRevenueAnalyzer(controler.getScenario());
        ParkingAnalyzer parkingAnalyzer = new ParkingAnalyzer(controler.getScenario());

        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(link2PersonListAnalyzer);
        events.addHandler(ptRevenueAnalyzer);
        events.addHandler(parkingAnalyzer);

        String outputDir = controler.getConfig().controler().getOutputDirectory();
        String runId = controler.getConfig().controler().getRunId();
        String eventsFile = outputDir + "/" + runId + ".output_events.xml.gz";

        MatsimEventsReader reader = new MatsimEventsReader(events);
        events.initProcessing();
        reader.readFile(eventsFile);
        events.finishProcessing();

        String outputFile = outputDir + "/" + runId + ".output_";
        link2PersonListAnalyzer.printResults(outputFile);
        ptRevenueAnalyzer.printResults(outputFile);
        parkingAnalyzer.printResults(outputFile);

        // This does not work on math cluster. Network needs to be handled on local machine.
        // Network2Shape.exportNetwork2Shp(controler.getScenario(), outputDir, "epsg:25832", TransformationFactory.getCoordinateTransformation("epsg:25832", "epsg:25832"));


        // Do always as last - as config parameters are changed within this analyzer
        PTComparisonAnalyzer comparisonAnalyzer = new PTComparisonAnalyzer(controler.getScenario());
        comparisonAnalyzer.calculatePTRouteOptions(controler);
        comparisonAnalyzer.printResults(outputFile);


        log.info("FINISH POST-PROCESSING");
        log.info("------------");
    }

}
