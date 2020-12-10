/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.stuttgart.run;

import java.util.Arrays;
import java.util.Set;
import ch.sbb.matsim.config.SBBTransitConfigGroup;
import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
import org.apache.log4j.Logger;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.stuttgart.Utils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.stuttgart.prepare.AddAdditionalNetworkAttributes;
import org.matsim.stuttgart.prepare.PrepareTransitSchedule;
import org.matsim.stuttgart.ptFares.PtFaresConfigGroup;
import org.matsim.stuttgart.ptFares.PtFaresModule;
import playground.vsp.simpleParkingCostHandler.ParkingCostConfigGroup;
import playground.vsp.simpleParkingCostHandler.ParkingCostModule;

import static org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks;

/**
 * @author dwedekind, gleich
 */

public class RunStuttgartWithAdditionalModules {
    private static final Logger log = Logger.getLogger(RunStuttgartWithAdditionalModules.class );

    public static void main(String[] args) {

        for (String arg : args) {
            log.info( arg );
        }

        Config config = prepareConfig( args ) ;
        Scenario scenario = prepareScenario( config ) ;
        Controler controler = prepareControler( scenario ) ;
        controler.run() ;
    }


    public static Config prepareConfig(String [] args, ConfigGroup... customModules) {
        OutputDirectoryLogging.catchLogEntries();


        // -- PREPARE CUSTOM MODULES
        String[] typedArgs = Arrays.copyOfRange( args, 1, args.length );

        ConfigGroup[] customModulesToAdd = new ConfigGroup[]{ setupRaptorConfigGroup(), setupPTFaresGroup(), setupSBBTransit(), setupParkingCostConfigGroup()};
        ConfigGroup[] customModulesAll = new ConfigGroup[customModules.length + customModulesToAdd.length];

        int counter = 0;
        for (ConfigGroup customModule : customModules) {
            customModulesAll[counter] = customModule;
            counter++;
        }

        for (ConfigGroup customModule : customModulesToAdd) {
            customModulesAll[counter] = customModule;
            counter++;
        }

        // -- LOAD CONFIG WITH CUSTOM MODULES
        final Config config = ConfigUtils.loadConfig( args[ 0 ], customModulesAll );


        // -- CONTROLER --
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);


        // -- VSP DEFAULTS --
        config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.ignore );
        config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink); //setInsertingAccessEgressWalk( true );
        config.qsim().setUsingTravelTimeCheckInTeleportation( true );
        config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );


        // -- OTHER --
        config.qsim().setUsePersonIdForMissingVehicleId(false);
        config.controler().setRoutingAlgorithmType(FastAStarLandmarks);
        config.subtourModeChoice().setProbaForRandomSingleTripMode( 0.5 );
        config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles( true );


        // -- ACTIVITIES --
        final long minDuration = 600;
        final long maxDuration = 3600 * 27;
        final long difference = 600;

        Utils.createTypicalDurations("home", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));
        Utils.createTypicalDurations("work", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));
        Utils.createTypicalDurations("leisure", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));
        Utils.createTypicalDurations("shopping", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));
        Utils.createTypicalDurations("errands", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));
        Utils.createTypicalDurations("business", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));
        Utils.createTypicalDurations("educ_secondary", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));
        Utils.createTypicalDurations("educ_higher", minDuration, maxDuration, difference).forEach(params -> config.planCalcScore().addActivityParams(params));


        // -- SET OUTPUT DIRECTORY FOR HOME PC RUNS
        String outputDir = args[0].replace((args[0].substring(args[0].lastIndexOf("/") + 1)),"") + "output";
        config.controler().setOutputDirectory(outputDir);

        // config.controler().setWriteTripsInterval(50);

        // -- SET PROPERTIES BY BASH SCRIPT (FOR CLUSTER USAGE ONLY)
        ConfigUtils.applyCommandline( config, typedArgs ) ;

        log.info("Config successfully prepared...");

        return config ;
    }


    public static Scenario prepareScenario( Config config ) {
        Gbl.assertNotNull( config );

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // Add fareZones and VVSBikeAndRideStops
        String schedulePath = config.transit().getTransitScheduleFileURL(config.getContext()).getPath();
        PrepareTransitSchedule ptPreparer = new PrepareTransitSchedule();
        ptPreparer.run(scenario, schedulePath.substring(0, schedulePath.lastIndexOf('/')) + "/fareZones_sp.shp");

        // Add parking costs to network
        String networkPath = config.network().getInputFileURL(config.getContext()).getPath();
        AddAdditionalNetworkAttributes parkingPreparer = new AddAdditionalNetworkAttributes();
        parkingPreparer.run(scenario, networkPath.substring(0, networkPath.lastIndexOf('/')) + "/parkingShapes.shp");

        log.info("Scenario successfully prepared...");

        return scenario;
    }


    public static Controler prepareControler( Scenario scenario ) {
        Gbl.assertNotNull(scenario);

        final Controler controler = new Controler( scenario );

        // -- ADDITIONAL MODULES --
        if (controler.getConfig().transit().isUsingTransitInMobsim()) {
            // use the sbb pt raptor router
            controler.addOverridingModule( new AbstractModule() {
                @Override
                public void install() {
                    install( new SwissRailRaptorModule() );
                }
            } );
        } else {
            log.warn("Public transit will be teleported and not simulated in the mobsim! "
                    + "This will have a significant effect on pt-related parameters (travel times, modal split, and so on). "
                    + "Should only be used for testing or car-focused studies with a fixed modal split.  ");
        }

        // use the (congested) car travel time for the teleported ride modes
        controler.addOverridingModule( new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding( TransportMode.ride ).to( networkTravelTime() );
                addTravelDisutilityFactoryBinding( TransportMode.ride ).to( carTravelDisutilityFactoryKey() ); }
        } );

        // use scoring parameters for intermodal PT routing
        controler.addOverridingModule( new AbstractModule() {
            @Override
            public void install() {
                bind(RaptorIntermodalAccessEgress.class).to(StuttgartRaptorIntermodalAccessEgress.class);
            }
        } );

        controler.addOverridingModule( new AbstractModule() {
            @Override
            public void install() {
                bind(AnalysisMainModeIdentifier.class).to(StuttgartAnalysisMainModeIdentifier.class);
            }
        } );



        // use deterministic transport simulation of SBB
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                // To use the deterministic pt simulation (Part 1 of 2):
                install(new SBBTransitModule());
            }

            // To use the deterministic pt simulation (Part 2 of 2):
        });

        controler.configureQSimComponents(SBBTransitEngineQSimModule::configure);


        // use parking cost module
        controler.addOverridingModule(new ParkingCostModule());

        // use pt fares module
        controler.addOverridingModule(new PtFaresModule());

        log.info("Controler successfully prepared...");

        return controler;
    }




    private static SwissRailRaptorConfigGroup setupRaptorConfigGroup() {
        SwissRailRaptorConfigGroup configRaptor = new SwissRailRaptorConfigGroup();

        // -- Intermodal Routing --

        configRaptor.setUseIntermodalAccessEgress(true);

        // AcessEgressWalk
        SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet paramSetAEWalk = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
        paramSetAEWalk.setInitialSearchRadius(1000);
        paramSetAEWalk.setSearchExtensionRadius(500);
        paramSetAEWalk.setMode(TransportMode.walk);
        paramSetAEWalk.setMaxRadius(10000);
        configRaptor.addIntermodalAccessEgress(paramSetAEWalk);

        // AccessEgressBike
        SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet paramSetAEBike = new SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet();
        paramSetAEBike.setInitialSearchRadius(5000);
        paramSetAEBike.setSearchExtensionRadius(2000);
        paramSetAEBike.setMode(TransportMode.bike);
        paramSetAEBike.setMaxRadius(10000);
        paramSetAEBike.setStopFilterAttribute("VVSBikeAndRide");
        paramSetAEBike.setStopFilterValue("true");

        configRaptor.addIntermodalAccessEgress(paramSetAEBike);

        return configRaptor;
    }


    private static PtFaresConfigGroup setupPTFaresGroup() {

        PtFaresConfigGroup configFares = new PtFaresConfigGroup();

        // For values, see https://www.vvs.de/tickets/zeittickets-abo-polygo/jahresticket-jedermann/

        PtFaresConfigGroup.FaresGroup faresGroup = new PtFaresConfigGroup.FaresGroup();
        faresGroup.setOutOfZonePrice(10.);
        faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(1, 1.89));
        faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(2, 2.42));
        faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(3, 3.23));
        faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(4, 4.));
        faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(5, 4.68));
        faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(6, 5.51));
        faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(7, 6.22));
        faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(8, 6.22));
        configFares.setFaresGroup(faresGroup);

        PtFaresConfigGroup.ZonesGroup zonesGroup = new PtFaresConfigGroup.ZonesGroup();
        zonesGroup.setOutOfZoneTag("out");
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("1", false));
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("1/2", true, Set.of("1", "2")));
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("2", false));
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("2/3", true, Set.of("2", "3")));
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("3", false));
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("3/4", true, Set.of("3", "4")));
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("4", false));
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("4/5", true, Set.of("4", "5")));
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("5", false));
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("5/6", true, Set.of("5", "6")));
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("6", false));
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("6/7", true, Set.of("6", "7")));
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("7", false));
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("7/8", true, Set.of("7", "8")));
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("8", false));
        configFares.setZonesGroup(zonesGroup);

        return configFares;
    }


    private static SBBTransitConfigGroup setupSBBTransit() {

        SBBTransitConfigGroup sbbTransit = new SBBTransitConfigGroup();
        var modes = Set.of(TransportMode.train, "bus", "tram");
        sbbTransit.setDeterministicServiceModes(modes);
        sbbTransit.setCreateLinkEventsInterval(10);

        return sbbTransit;
    }


    private static ParkingCostConfigGroup setupParkingCostConfigGroup() {

        ParkingCostConfigGroup parkingCostConfigGroup = new ParkingCostConfigGroup();

        parkingCostConfigGroup.setFirstHourParkingCostLinkAttributeName("oneHourPCost");
        parkingCostConfigGroup.setExtraHourParkingCostLinkAttributeName("extraHourPCost");
        parkingCostConfigGroup.setMaxDailyParkingCostLinkAttributeName("maxDailyPCost");
        parkingCostConfigGroup.setMaxParkingDurationAttributeName("maxPTime");
        parkingCostConfigGroup.setParkingPenaltyAttributeName("pFine");
        parkingCostConfigGroup.setResidentialParkingFeeAttributeName("resPCosts");

        return parkingCostConfigGroup;
    }



}
