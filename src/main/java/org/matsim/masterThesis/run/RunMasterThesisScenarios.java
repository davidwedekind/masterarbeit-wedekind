package org.matsim.masterThesis.run;

import graphql.AssertException;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.masterThesis.BanCarsFromSmallerStreets;
import org.matsim.masterThesis.prep.CleanPopulationAfterCalibration;
import org.matsim.masterThesis.ptModifiers.*;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
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

public class RunMasterThesisScenarios {

    public static void main(String[] args) throws URISyntaxException {
        final Logger log = Logger.getLogger(RunMasterThesisScenarios.class);

        for (String arg : args) {
            log.info( arg );
        }


        // ------ CONFIG ------
        // Read all scenario relevant parameters from config
        Config config = StuttgartMasterThesisRunner.prepareConfig(args, new StuttgartMasterThesisExperimentalConfigGroup());
        Path configPath = Paths.get(config.getContext().toURI()).getParent();

        // After calibration, ride should be excluded from subtour mode choice
        List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
        modes.remove("ride");
        config.subtourModeChoice().setModes(modes.toArray(new String[0]));

        // In addition to the calibration case, master thesis experimental config group is needed
        // for receiving some additional scenario parameters
        StuttgartMasterThesisExperimentalConfigGroup thesisExpConfigGroup =
                ConfigUtils.addOrGetModule(config, StuttgartMasterThesisExperimentalConfigGroup.class);


        // ------ SCENARIO ------
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
            new CreateSupershuttle().runExtensionModifications(scenario);

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

        if (thesisExpConfigGroup.getFlughafenConnectionAlignment()){
            new OptimizeConnections().runOptimizer(scenario);

        }

        // Finally include fare Zones and parking shapes according to the scenario
        // and finish the scenario
        StuttgartMasterThesisRunner.finishScenario(
                scenario,
                configPath.resolve(thesisExpConfigGroup.getFareZoneShapeFile()).toString(),
                configPath.resolve(thesisExpConfigGroup.getParkingZoneShapeFile()).toString()
                );


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
        IntermodalTripFareCompensatorsConfigGroup compensatorsCfg = ConfigUtils.addOrGetModule(config, IntermodalTripFareCompensatorsConfigGroup.GROUP_NAME, IntermodalTripFareCompensatorsConfigGroup.class);

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


        PtFaresConfigGroup configFares = ConfigUtils.addOrGetModule(config, PtFaresConfigGroup.GROUP, PtFaresConfigGroup.class);

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
        log.info("Flughafen Connection Alignment Extension: " + (thesisExpConfigGroup.getFlughafenConnectionAlignment()));
        log.info("------------");

        // ------ CONTROLER ------

        Controler controler = StuttgartMasterThesisRunner.prepareControler(scenario) ;
        controler.run() ;

    }

}
