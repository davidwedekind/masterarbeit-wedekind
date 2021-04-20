package org.matsim.masterThesis.analyzer;


import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.inject.Module;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsModule;
import org.matsim.extensions.pt.replanning.singleTripStrategies.ChangeSingleTripModeAndRoute;
import org.matsim.extensions.pt.replanning.singleTripStrategies.RandomSingleTripReRoute;
import org.matsim.extensions.pt.routing.EnhancedRaptorIntermodalAccessEgress;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesModule;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.stuttgart.run.StuttgartAnalysisMainModeIdentifier;

import java.net.URISyntaxException;
import java.util.Collections;

public class PTComparisonAnalyzer {

    public static void main(String[] args) throws URISyntaxException {

        Config config = ConfigUtils.loadConfig(args[0]);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        AbstractModule module = new AbstractModule() {
            @Override
            public void install() {
                install(new NewControlerModule());
                install(new ControlerDefaultCoreListenersModule());
                install(new ControlerDefaultsModule());
                install(new ScenarioByInstanceModule(scenario));
                new AbstractModule() {
                    public void install() {
                        this.bind(AnalysisMainModeIdentifier.class).to(StuttgartAnalysisMainModeIdentifier.class);
                    }
                };
/*                install(new IntermodalTripFareCompensatorsModule());
                install(new PtIntermodalRoutingModesModule());*/
            }
        };

        module = AbstractModule.override(Collections.singletonList(module), new SwissRailRaptorModule());

        module = AbstractModule.override(Collections.singletonList(module), new AbstractModule() {
            @Override
            public void install() {
                addPlanStrategyBinding("RandomSingleTripReRoute").toProvider(RandomSingleTripReRoute.class);
                addPlanStrategyBinding("ChangeSingleTripModeAndRoute").toProvider(ChangeSingleTripModeAndRoute.class);

                bind(RaptorIntermodalAccessEgress.class).to(EnhancedRaptorIntermodalAccessEgress.class);
/*                new IntermodalTripFareCompensatorsModule();
                new PtIntermodalRoutingModesModule();*/
            }
        });


        // I don't know how to configure these two modules in the context of this analyzer (without controler)...
        // ToDo: Find out how this could work...

/*        module = AbstractModule.override(Collections.singletonList(module), new IntermodalTripFareCompensatorsModule());
        module = AbstractModule.override(Collections.singletonList(module), new PtIntermodalRoutingModesModule());*/


        com.google.inject.Injector injector = org.matsim.core.controler.Injector.createInjector(config, module);
        TripRouter tripRouter = injector.getInstance(TripRouter.class);

        for (Person person : scenario.getPopulation().getPersons().values()) {
            var trips = TripStructureUtils.getTrips(person.getSelectedPlan());
            for (TripStructureUtils.Trip trip :
                    trips) {
                Facility fromFacility = FacilitiesUtils.wrapActivity(trip.getOriginActivity());
                Facility toFacility = FacilitiesUtils.wrapActivity(trip.getDestinationActivity());
                var router = tripRouter.calcRoute(TransportMode.pt, fromFacility, toFacility, trip.getOriginActivity().getEndTime().seconds(), person);
                System.out.println(router.toString());


            }

        }

    }


}

