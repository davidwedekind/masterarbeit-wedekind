package org.matsim.masterThesis.ptModifiers;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import graphql.AssertException;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaktModifier {
    private static final Logger log = Logger.getLogger(TaktModifier.class);
    private final Scenario scenario;
    VehiclesFactory vF;
    TransitSchedule tS;


    public TaktModifier(Scenario scenario){
        this.scenario = scenario;
        this.tS = scenario.getTransitSchedule();
        this.vF = scenario.getTransitVehicles().getFactory();
    }


    public static void main(String[] args) {
        TaktModifier.Input input = new TaktModifier.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Input network file: " + input.networkFile);
        log.info("Input transit schedule file: " + input.transitSchedule);
        log.info("Input transit vehicle file: " + input.transitVehicles);
        log.info("Output file directory: " + input.outputFile);

        Config config = ConfigUtils.createConfig();


        final String epsgCode = "25832";

        config.global().setCoordinateSystem("EPSG:" + epsgCode);
        config.transit().setTransitScheduleFile(input.transitSchedule);
        config.network().setInputFile(input.networkFile);
        config.transit().setVehiclesFile(input.transitVehicles);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        TaktModifier modifier = new TaktModifier(scenario);

        double newTakt = 15*60;
        TransitLine lineS60 = scenario.getTransitSchedule().getTransitLines().get(Id.create("S 60 - 1", TransitLine.class));

        TransitRoute route1 = lineS60.getRoutes().get(Id.create("1", TransitRoute.class));
        modifier.doubleTakt(route1, newTakt, 999990100);

        TransitRoute route2 = lineS60.getRoutes().get(Id.create("2", TransitRoute.class));
        modifier.doubleTakt(route2, newTakt, 999990200);

        TransitRoute route3 = lineS60.getRoutes().get(Id.create("3", TransitRoute.class));
        modifier.doubleTakt(route3, newTakt, 999990300);

        TransitScheduleValidator.ValidationResult resultAfterModifying = TransitScheduleValidator.validateAll(
                scenario.getTransitSchedule(), scenario.getNetwork());
        log.info("Transit validator results after modifying:");
        for (String errorMessage:resultAfterModifying.getErrors()){
            throw new AssertException(errorMessage);
        }

        new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(input.outputFile + "/modifiedSchedule.xml.gz");
        new NetworkWriter(scenario.getNetwork()).write(input.outputFile + "/modifiedNetwork.xml.gz");
        new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(input.outputFile + "/modifiedVehicles.xml.gz");

    }


    public void doubleTakt(TransitRoute route, double newTakt, int idStarter) {
        doubleTakt(route, newTakt, 0, 30*60*60, idStarter);
    }

    public void doubleTakt(TransitRoute route, double newTakt, double start, double end, int idStarter) {
        Map<Id<Departure>, Departure> routeDepartures = route.getDepartures();

        List<Departure> departuresToAdd = new ArrayList<>();
        List<Id<Vehicle>> vehiclesToAdd = new ArrayList<>();

        for (var departure: routeDepartures.values()){
            if (departure.getDepartureTime() >= start && departure.getDepartureTime() <= end){
                double newDepartureTime = departure.getDepartureTime() + newTakt;
                Departure newDeparture = tS.getFactory().createDeparture(Id.create(idStarter, Departure.class), newDepartureTime);
                Id<Vehicle> newVehicleId = Id.createVehicleId(idStarter);
                newDeparture.setVehicleId(newVehicleId);
                departuresToAdd.add(newDeparture);
                vehiclesToAdd.add(newVehicleId);
                idStarter++;
            }

        }

        for (var departure: departuresToAdd) {
            route.addDeparture(departure);
        }

        VehicleType type = scenario.getTransitVehicles().getVehicleTypes().get(Id.create("defaultTransitVehicleType", VehicleType.class));
        for (var vehicleId: vehiclesToAdd){
            Vehicle vehicle = vF.createVehicle(vehicleId, type);
            scenario.getTransitVehicles().addVehicle(vehicle);
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
