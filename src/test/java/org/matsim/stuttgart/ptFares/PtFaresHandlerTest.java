package org.matsim.stuttgart.ptFares;


import ch.sbb.matsim.config.SBBTransitConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.vividsolutions.jts.util.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.stuttgart.run.StuttgartRaptorIntermodalAccessEgress;

import java.util.*;

import static org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks;

public class PtFaresHandlerTest {
    private static final Logger log = Logger.getLogger(PtFaresHandlerTest.class);

    Map<Id<Person>, Double> transitFares = new HashMap<>();

    @Test
    public final void runPtFaresHandlerTest() {

        String configPath = "test/input/ptFares/config.xml";

        // Prepare config
        Config config = prepareConfig(configPath);

        // Prepare scenario
        Scenario scenario = prepareScenario(config);
        Controler controler = prepareControler(scenario);

        EventsManager events = controler.getEvents();
        PtFaresHandlerTest.EventsTester tester = new PtFaresHandlerTest.EventsTester();
        events.addHandler(tester);

        controler.run();

        // Check output param
        Assert.equals(transitFares.get(Id.createPersonId("1")), -300.);
        Assert.equals(transitFares.get(Id.createPersonId("2")), -200.);
        Assert.equals(transitFares.get(Id.createPersonId("3")), -100.);
        Assert.equals(transitFares.get(Id.createPersonId("4")), -100.);
        Assert.equals(transitFares.get(Id.createPersonId("5")), -100.);
        Assert.equals(transitFares.get(Id.createPersonId("6")), -200.);
        Assert.isTrue(! transitFares.containsKey(Id.createPersonId("7")));

    }


    public static Controler prepareControler(Scenario scenario) {
        Gbl.assertNotNull(scenario);

        final Controler controler = new Controler(scenario);

        // -- ADDITIONAL MODULES --
        if (controler.getConfig().transit().isUsingTransitInMobsim()) {
            // use the sbb pt raptor router
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    install(new SwissRailRaptorModule());
                }
            });
        } else {
            log.warn("Public transit will be teleported and not simulated in the mobsim! "
                    + "This will have a significant effect on pt-related parameters (travel times, modal split, and so on). "
                    + "Should only be used for testing or car-focused studies with a fixed modal split.  ");
        }

        // use the (congested) car travel time for the teleported ride modes
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
                addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
            }
        });

        // use scoring parameters for intermodal PT routing
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(RaptorIntermodalAccessEgress.class).to(StuttgartRaptorIntermodalAccessEgress.class);
            }
        });

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

        // use pt fares module
        controler.addOverridingModule(new PtFaresModule());

        return controler;

    }


    private Scenario prepareScenario(Config config) {

        Gbl.assertNotNull(config);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // write fareZones into transitScheduleFile
        TransitSchedule schedule = scenario.getTransitSchedule();

        schedule.getFacilities().get(Id.create("1", TransitStopFacility.class)).getAttributes().putAttribute("ptFareZone", "1");
        schedule.getFacilities().get(Id.create("2a", TransitStopFacility.class)).getAttributes().putAttribute("ptFareZone", "2");
        schedule.getFacilities().get(Id.create("2b", TransitStopFacility.class)).getAttributes().putAttribute("ptFareZone", "2");
        schedule.getFacilities().get(Id.create("3", TransitStopFacility.class)).getAttributes().putAttribute("ptFareZone", "1,2");


        schedule.getFacilities().get(Id.create("4", TransitStopFacility.class)).getAttributes().putAttribute("ptFareZone", "2");
        schedule.getFacilities().get(Id.create("5a", TransitStopFacility.class)).getAttributes().putAttribute("ptFareZone", "2");
        schedule.getFacilities().get(Id.create("5b", TransitStopFacility.class)).getAttributes().putAttribute("ptFareZone", "2");
        schedule.getFacilities().get(Id.create("6", TransitStopFacility.class)).getAttributes().putAttribute("ptFareZone", "out");

        return scenario;
    }


    private Config prepareConfig(String configPath) {

        // Add custom modules
        ConfigGroup[] customModulesToAdd = new ConfigGroup[]{setupPTFaresGroup(), setupRaptorConfigGroup(), setupSBBTransit()};
        ConfigGroup[] customModulesAll = new ConfigGroup[customModulesToAdd.length];

        int counter = 0;

        for (ConfigGroup customModule : customModulesToAdd) {
            customModulesAll[counter] = customModule;
            counter++;
        }

        // -- LOAD CONFIG WITH CUSTOM MODULES
        final Config config = ConfigUtils.loadConfig(configPath, customModulesAll);


        // -- CONTROLER --
        config.controler().setLastIteration(0);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);


        // -- VSP DEFAULTS --
        config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.ignore);
        config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink); //setInsertingAccessEgressWalk( true );
        config.qsim().setUsingTravelTimeCheckInTeleportation(true);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);


        // -- OTHER --
        config.qsim().setUsePersonIdForMissingVehicleId(false);
        config.controler().setRoutingAlgorithmType(FastAStarLandmarks);
        config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);
        config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);

        return config;

    }


    private static PtFaresConfigGroup setupPTFaresGroup() {

        PtFaresConfigGroup configFares = new PtFaresConfigGroup();

        PtFaresConfigGroup.FaresGroup faresGroup = new PtFaresConfigGroup.FaresGroup();
        faresGroup.setOutOfZonePrice(300.);
        faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(1,100.));
        faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(2,200.));

        configFares.setFaresGroup(faresGroup);

        PtFaresConfigGroup.ZonesGroup zonesGroup = new PtFaresConfigGroup.ZonesGroup();
        zonesGroup.setOutOfZoneTag("out");
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("1", false));
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("2", false));
        zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("1,2", true, Set.of("1", "2")));
        configFares.setZonesGroup(zonesGroup);

        return configFares;
    }


    private static SwissRailRaptorConfigGroup setupRaptorConfigGroup() {
        SwissRailRaptorConfigGroup configRaptor = new SwissRailRaptorConfigGroup();
        configRaptor.setUseIntermodalAccessEgress(false);

        return configRaptor;
    }


    private static SBBTransitConfigGroup setupSBBTransit() {

        SBBTransitConfigGroup sbbTransit = new SBBTransitConfigGroup();
        Set<String> modes = new HashSet<>(Arrays.asList("train"));
        sbbTransit.setDeterministicServiceModes(modes);
        sbbTransit.setCreateLinkEventsInterval(0);

        return sbbTransit;
    }


    private final class EventsTester implements PersonMoneyEventHandler {
        @Override
        public void handleEvent(PersonMoneyEvent event) {
            transitFares.put(event.getPersonId(), event.getAmount());
        }
    }

}
