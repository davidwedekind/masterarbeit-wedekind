package org.matsim.masterThesis.analyzer;


import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.inject.Module;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsModule;
import org.matsim.extensions.pt.replanning.singleTripStrategies.ChangeSingleTripModeAndRoute;
import org.matsim.extensions.pt.replanning.singleTripStrategies.RandomSingleTripReRoute;
import org.matsim.extensions.pt.routing.EnhancedRaptorIntermodalAccessEgress;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesModule;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.masterThesis.run.ScenarioRunner;
import org.matsim.stuttgart.run.StuttgartAnalysisMainModeIdentifier;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PTComparisonAnalyzer {
    private static final Logger log = Logger.getLogger(PTComparisonAnalyzer.class);

    private final String separator;
    private final String[] HEADER = {"person_id", "trip_id", "routing_mode", "start_time", "end_time", "trav_time", "isBikeAndRide", "containsS60"};

    private final Config config;
    private final Scenario scenario;

    private final List<List<String>> results = new ArrayList<>();

    public PTComparisonAnalyzer(Scenario scenario){
        this.config = scenario.getConfig();
        this.scenario = scenario;
        this.separator = scenario.getConfig().global().getDefaultDelimiter();

    }


    public static void main(String[] args) {

        Config config = ScenarioRunner.prepareConfig( args );

        // Do not perform scenario creation of scenario runner again
        Scenario scenario = ScenarioUtils.loadScenario( config );

        PTComparisonAnalyzer analyzer = new PTComparisonAnalyzer( scenario );
        analyzer.calculatePTRouteOptions();
        analyzer.printResults( config.getContext().toString() );

    }


    public void calculatePTRouteOptions(Controler controler){
        // Calculator version which can be used as postprocessor after controler.run() has been executed
        // controler.run() does the module installation and injector creation by itself
        controler.getConfig().controler().setOutputDirectory(controler.getConfig().controler().getOutputDirectory() + "/ptComparator");
        com.google.inject.Injector injector = controler.getInjector();
        calculatePTRouteOptions(injector);

    }


    public void calculatePTRouteOptions(){
        // Calculator version which can be executed right away but does installing of modules itself
        // For single usage not after controler.run() has been executed
        config.controler().setOutputDirectory(config.controler().getOutputDirectory() + "/ptComparator");
        calculatePTRouteOptions(installModulesManually());

    }


    public void calculatePTRouteOptions(com.google.inject.Injector injector){

        TripRouter tripRouter = injector.getInstance(TripRouter.class);

        log.info("Start calculating routes...");
        for (Person person : scenario.getPopulation().getPersons().values()) {
            var trips = TripStructureUtils.getTrips(person.getSelectedPlan());

            int tripCount = 1;
            for (TripStructureUtils.Trip trip :
                    trips) {
                Facility fromFacility = FacilitiesUtils.wrapActivity(trip.getOriginActivity());
                Facility toFacility = FacilitiesUtils.wrapActivity(trip.getDestinationActivity());


                // Determine regular pt route with parameters
                List<String> routeResult = new ArrayList<>();
                var route = tripRouter.calcRoute(TransportMode.pt, fromFacility, toFacility, trip.getOriginActivity().getEndTime().seconds(), person);

                String person_id = person.getId().toString();
                routeResult.add(person_id);

                String trip_id = person_id + "_" + tripCount;
                routeResult.add(trip_id);

                routeResult.add(TransportMode.pt);

                Leg firstLegOnRoute1 = (Leg) route.get(0);
                Leg lastLegOnRoute1 = (Leg) route.get(route.size() - 1);
                Double startTimeOnRoute1 = firstLegOnRoute1.getDepartureTime().seconds();
                Double endTimeOnRoute1 = lastLegOnRoute1.getDepartureTime().seconds() + lastLegOnRoute1.getTravelTime().seconds();
                Double travelTimeOnRoute1 = endTimeOnRoute1 - startTimeOnRoute1;
                routeResult.add(String.valueOf(startTimeOnRoute1));
                routeResult.add(String.valueOf(endTimeOnRoute1));
                routeResult.add(String.valueOf(travelTimeOnRoute1));

                routeResult.add(String.valueOf(false));
                routeResult.add(String.valueOf(false));

                results.add(routeResult);


                // Determine route and parameters for pt with the option of using bike on access and egress
                List<String> routeWithBikeAllowedResult = new ArrayList<>();
                var routeWithBikeAllowed = tripRouter.calcRoute("pt_w_bike_allowed", fromFacility, toFacility, trip.getOriginActivity().getEndTime().seconds(), person);

                routeWithBikeAllowedResult.add(person_id);

                routeWithBikeAllowedResult.add(trip_id);

                routeWithBikeAllowedResult.add("pt_w_bike_allowed");

                Leg firstLegOnRoute2 = (Leg) routeWithBikeAllowed.get(0);
                Leg lastLegOnRoute2 = (Leg) routeWithBikeAllowed.get(routeWithBikeAllowed.size() - 1);
                Double startTimeOnRoute2 = firstLegOnRoute2.getDepartureTime().seconds();
                Double endTimeOnRoute2 = lastLegOnRoute2.getDepartureTime().seconds() + firstLegOnRoute2.getTravelTime().seconds();
                Double travelTimeOnRoute2 = endTimeOnRoute2 - startTimeOnRoute2;
                routeWithBikeAllowedResult.add(String.valueOf(startTimeOnRoute2));
                routeWithBikeAllowedResult.add(String.valueOf(endTimeOnRoute2));
                routeWithBikeAllowedResult.add(String.valueOf(travelTimeOnRoute2));


                boolean isBikeAndRide = route.stream()
                        .filter(pe -> pe instanceof Leg)
                        .map(pe -> (Leg) pe)
                        .anyMatch(leg -> leg.getMode().equals(TransportMode.bike));

                routeWithBikeAllowedResult.add(String.valueOf(isBikeAndRide));
                routeWithBikeAllowedResult.add(String.valueOf(false));

                results.add(routeWithBikeAllowedResult);

                tripCount ++;
            }

        }

    }


    public void printResults(String path){
        String fileName = path + "ptComparatorResults.csv.gz";

        try {

            CSVPrinter csvPrinter = new CSVPrinter(IOUtils.getBufferedWriter(fileName),
                    CSVFormat.DEFAULT.withDelimiter(separator.charAt(0)).withHeader(HEADER));

            csvPrinter.printRecords(results);

            csvPrinter.close();
            log.info("ptComparatorResults written to: " + fileName);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public com.google.inject.Injector installModulesManually(){
        // Start modules manually as I do not want
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
            }
        };

        module = AbstractModule.override(Collections.singletonList(module), new SwissRailRaptorModule());

        module = AbstractModule.override(Collections.singletonList(module), new AbstractModule() {
            @Override
            public void install() {
                addPlanStrategyBinding("RandomSingleTripReRoute").toProvider(RandomSingleTripReRoute.class);
                addPlanStrategyBinding("ChangeSingleTripModeAndRoute").toProvider(ChangeSingleTripModeAndRoute.class);
                bind(RaptorIntermodalAccessEgress.class).to(EnhancedRaptorIntermodalAccessEgress.class);
            }
        });

        module = AbstractModule.override(Collections.singletonList(module), new IntermodalTripFareCompensatorsModule());
        module = AbstractModule.override(Collections.singletonList(module), new PtIntermodalRoutingModesModule());

        return org.matsim.core.controler.Injector.createInjector(config, module);
    }


}

