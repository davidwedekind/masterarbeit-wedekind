package org.matsim.masterThesis.analyzer;


import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
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
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.stuttgart.run.StuttgartAnalysisMainModeIdentifier;


import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PTComparisonAnalyzer {
    private static final Logger log = Logger.getLogger(PTComparisonAnalyzer.class);

    private final String separator;
    private final String[] HEADER = {"person_id", "trip_id", "routing_mode", "start_time", "end_time", "trav_time", "containsS60", "time_on_s60", "is_bike_and_ride", "is_walk", "route_description"};

    private final Config config;
    private final Scenario scenario;

    private final List<List<String>> results = new ArrayList<>();

    public PTComparisonAnalyzer(Scenario scenario){
        this.config = scenario.getConfig();
        this.scenario = scenario;
        this.separator = scenario.getConfig().global().getDefaultDelimiter();

    }


    public static void main(String[] args) throws URISyntaxException {


        Config config = ScenarioRunner.prepareConfig( args );
        File file = new File(config.getContext().toURI());
        String outputDir = file.getParentFile().getAbsolutePath();


        System.out.println(outputDir);

        // Do not perform scenario creation of scenario runner again
        Scenario scenario = ScenarioUtils.loadScenario( config );

        PTComparisonAnalyzer analyzer = new PTComparisonAnalyzer( scenario );
        analyzer.calculatePTRouteOptions();
        
        analyzer.printResults( outputDir + "/");

    }


    public void calculatePTRouteOptions(Controler controler){
        // Calculator version which can be used as postprocessor after controler.run() has been executed
        // controler.run() does the module installation and injector creation by itself


        // Reset config file settings of output directory, plans, network, schedule etc...
        String outputDirectory = controler.getConfig().controler().getOutputDirectory();
        controler.getConfig().controler().setOutputDirectory(outputDirectory + "/ptComparatorDump");

        controler.getConfig().plans().setInputFile(getOutputPathLogic(controler, "plans.xml.gz"));
        controler.getConfig().network().setInputFile(getOutputPathLogic(controler, "network.xml.gz"));
        controler.getConfig().transit().setTransitScheduleFile(getOutputPathLogic(controler, "transitSchedule.xml.gz"));
        controler.getConfig().transit().setVehiclesFile(getOutputPathLogic(controler, "transitVehicles.xml.gz"));
        controler.getConfig().vehicles().setVehiclesFile(getOutputPathLogic(controler, "vehicles.xml.gz"));

        com.google.inject.Injector injector = controler.getInjector();
        calculatePTRouteOptions(injector);

    }


    private String getOutputPathLogic(Controler controler, String fileTypeSpecificNaming){
        String outputDirectory = controler.getConfig().controler().getOutputDirectory();
        String runId = controler.getConfig().controler().getRunId();

        return outputDirectory + "/" + runId + ".output_" + fileTypeSpecificNaming;
    }


    public void calculatePTRouteOptions(){
        // Calculator version which can be executed right away but does installing of modules itself
        // For single usage not after controler.run() has been executed
        config.controler().setOutputDirectory(config.controler().getOutputDirectory() + "/ptComparatorDump");
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
                var route = tripRouter.calcRoute(TransportMode.pt, fromFacility, toFacility, trip.getOriginActivity().getEndTime().seconds(), person);

                List<String> routeRecords = processRouteInformation(
                        person.getId().toString(),
                        person.getId().toString() + "_" + tripCount,
                        TransportMode.pt,
                        route
                );

                results.add(routeRecords);

                // Determine route and parameters for pt with the option of using bike on access and egress
                var routeWithBikeAllowed = tripRouter.calcRoute("pt_w_bike_allowed", fromFacility, toFacility, trip.getOriginActivity().getEndTime().seconds(), person);

                List<String> routeWithBikeAllowedRecords = processRouteInformation(
                        person.getId().toString(),
                        person.getId().toString() + "_" + tripCount,
                        "pt_w_bike_allowed",
                        routeWithBikeAllowed
                );

                results.add(routeWithBikeAllowedRecords);

                tripCount ++;
            }

        }

    }


    public List<String> processRouteInformation(String personId, String tripId, String routingMode, List<? extends PlanElement> route){
        List<String> relevantRecords = new ArrayList<>();

        relevantRecords.add(personId);
        relevantRecords.add(tripId);
        relevantRecords.add(routingMode);

        Leg firstLeg = (Leg) route.get(0);
        Double tripStartTime = firstLeg.getDepartureTime().seconds();

        Leg lastLeg = (Leg) route.get(route.size() - 1);
        Double tripEndTime = firstLeg.getDepartureTime().seconds() + lastLeg.getTravelTime().seconds();

        Double tripTravelTime = tripEndTime - tripStartTime;

        relevantRecords.add(String.valueOf(tripStartTime));
        relevantRecords.add(String.valueOf(tripEndTime));
        relevantRecords.add(String.valueOf(tripTravelTime));

        String s60LineId = "S60 - 1";
        boolean containsS60 = route.stream()
                .filter(pe -> pe instanceof Leg)
                .map(pe -> (Leg) pe)
                .filter(leg -> leg.getRoute() instanceof TransitPassengerRoute)
                .map(leg -> (TransitPassengerRoute) leg.getRoute())
                .anyMatch(ptRoute -> ptRoute.getLineId().toString().equals(s60LineId));

        double timeOnS60 = 0.;
        if (containsS60){
            timeOnS60 = route.stream()
                    .filter(pe -> pe instanceof Leg)
                    .map(pe -> (Leg) pe)
                    .filter(leg -> leg.getRoute() instanceof TransitPassengerRoute)
                    .map(leg -> (TransitPassengerRoute) leg.getRoute())
                    .filter(ptRoute -> ptRoute.getLineId().toString().equals(s60LineId))
                    .mapToDouble(ptRoute -> ptRoute.getTravelTime().seconds())
                    .sum();
        }

        relevantRecords.add(String.valueOf(containsS60));
        relevantRecords.add(String.valueOf(timeOnS60));

        boolean isBikeAndRide = route.stream()
                .filter(pe -> pe instanceof Leg)
                .map(pe -> (Leg) pe)
                .anyMatch(leg -> leg.getMode().equals(TransportMode.bike));

        relevantRecords.add(String.valueOf(isBikeAndRide));

        boolean hasWalk = route.stream()
                .filter(pe -> pe instanceof Leg)
                .map(pe -> (Leg) pe)
                .anyMatch(leg -> leg.getMode().equals(TransportMode.walk));

        boolean hasOtherThanWalk = route.stream()
                .filter(pe -> pe instanceof Leg)
                .map(pe -> (Leg) pe)
                .anyMatch(leg -> ! leg.getMode().equals(TransportMode.walk));

        boolean isWalk = ((hasWalk) && (! hasOtherThanWalk));

        relevantRecords.add(String.valueOf(isWalk));

        String routeDescription = route.toString();

        relevantRecords.add(String.valueOf(routeDescription));

        return relevantRecords;
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

