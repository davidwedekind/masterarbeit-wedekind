package org.matsim.masterThesis.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.masterThesis.BanCarsFromSmallerStreets;
import org.matsim.masterThesis.ptModifiers.*;
import org.matsim.stuttgart.run.StuttgartMasterThesisRunner;

import java.util.Arrays;
import java.util.List;

/**
 * @author dwedekind
 */

public class RunMasterThesisScenarios {

    public static void main(String[] args) {
        final Logger log = Logger.getLogger(RunMasterThesisScenarios.class);

        for (String arg : args) {
            log.info( arg );
        }


        // ------ CONFIG ------

        // ---- BASE CASE CONFIG PREPARATION ----
        Config config = StuttgartMasterThesisRunner.prepareConfig(args, new StuttgartMasterThesisExperimentalConfigGroup()) ;

        // ---- FURTHER CONFIG ADJUSTMENTS ----

        // -- BIKE AND RIDE --
        // Teleported mode parameters are read from input config directly
        // Chain based modes are read from input config directly
        // CompensationMoneyPerTrip for intermodal trips is read from input directly

        StuttgartMasterThesisExperimentalConfigGroup thesisExpConfigGroup =
                ConfigUtils.addOrGetModule(config, StuttgartMasterThesisExperimentalConfigGroup.class);

        // -- PT FARES --
        // Pt fares are read from input config directly

        // --------------------



        // ------ SCENARIO ------

        // ---- ADJUSTED SCENARIO PREPARATION ----

        // -- PT FARES AND PARKING --
        // Provide FareZoneShapeFile and ParkingZoneShapeFile to the scenario accordingly via the thesisExperimentalConfigGroup
        Scenario scenario = StuttgartMasterThesisRunner.prepareScenario(
                config,
                thesisExpConfigGroup.getFareZoneShapeFile(),
                thesisExpConfigGroup.getParkingZoneShapeFile());


        // ToDo: Read threshholds for each closure area individually from shape file

        // -- BAN CARS FROM LIVING STREETS --
        // If measure 'reducedCarInfrastructure' is switched on, then 'BanCarsFromSmallerStreets'
        if (! thesisExpConfigGroup.getReducedCarInfrastructureShapeFile().isEmpty()){
            new BanCarsFromSmallerStreets(scenario.getNetwork()).run(
                    thesisExpConfigGroup.getReducedCarInfrastructureShapeFile(),
                    10.,
                    650.);

        }


        // -- PT MODIFICATIONS --
        if (thesisExpConfigGroup.getSupershuttleExtension()){
            new CreateSupershuttle().runExtensionModifications(scenario);

        }

        if (thesisExpConfigGroup.getS60Extension()){
            new CreateS60Extension().runExtensionModifications(scenario);

        }

        if (thesisExpConfigGroup.getU6ExtensionShapeFile() != null){
            new CreateU6Extension().runExtensionModifications(scenario, thesisExpConfigGroup.getU6ExtensionShapeFile());

            PtUtils modifier = new PtUtils(scenario);
            modifier.removeLine("Bus 122 - 9");

            List<String> stopsToCancel = Arrays.asList("557985", "557983", "557997", "557980", "561004", "555473", "555472", "561003", "557977", "560458", "560457");
            modifier.shortenLine("Bus 120 - 11", stopsToCancel);

        }

        if (thesisExpConfigGroup.getFlughafenConnectionAlignment()){
            new OptimizeConnections().runOptimizer(scenario);

        }

        // ----------------------



        // ---- PRINT OUT ALL SCENARIO RELEVANT SETTINGS TO LOG ----
        log.info("---- SCENARIO RELEVANT SETTINGS ----");
        log.info("BIKE AND RIDE");
        log.info("Bike Teleported Mode Speed [m/s]: " + config.plansCalcRoute().getModeRoutingParams().get(TransportMode.bike).getTeleportedModeSpeed().toString());


        log.info("Bike compensation money per (intermodal) trip:");

        //




        // ------ CONTROLER ------

/*        Controler controler = StuttgartMasterThesisRunner.prepareControler( scenario ) ;
        controler.run() ;*/

        // -----------------------


    }

}
