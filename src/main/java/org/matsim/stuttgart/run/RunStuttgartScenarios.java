package org.matsim.stuttgart.run;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

public class RunStuttgartScenarios {
    private static final Logger log = Logger.getLogger(RunStuttgartScenarios.class);
    private Config config;
    private Scenario scenario;
    private Controler controler;

/*    private RunStuttgartScenarios(RunArguments arguments){

    }

    public static void main(String[] args) {

        // The first two arguments are config setting arguments passed to the runner class
        RunArguments arguments = new RunArguments();
        JCommander.newBuilder().addObject(arguments).build().parse(args);
        new RunStuttgartScenarios(arguments).run();

    }

    public void run() {
        BikeAndRideSwitch bikeAndRide = BikeAndRideSwitch.BASE_CASE;
        ParkingSwitch parking = ParkingSwitch.BASE_CASE;
        PTFaresSwitch ptFares = PTFaresSwitch.BASE_CASE;

        // -- BIKE AND RIDE SWITCH --
        double bikeTeleportedModeSpeed;
        double bikeIntermodalTransferCosts;
        String[] chainModes;

        switch(bikeAndRide) {
            case LOW_IMPROVEMENT:
                bikeTeleportedModeSpeed = 4.0579732 * 1.05;
                bikeIntermodalTransferCosts = -0.3 * 0.7;
                chainModes = new String[]{"car"};

            case MEDIUM_IMPROVEMENT:
                bikeTeleportedModeSpeed = 4.0579732 * 1.10;
                bikeIntermodalTransferCosts = -0.3 * 0.5;
                chainModes = new String[]{"car"};

            case HIGH_IMPROVEMENT:
                bikeTeleportedModeSpeed = 4.0579732 * 1.20;
                bikeIntermodalTransferCosts = -0.3 * 0.2;
                chainModes = new String[]{"car"};

            default:
                bikeTeleportedModeSpeed = 4.0579732;
                bikeIntermodalTransferCosts = -0.3;
                chainModes = new String[]{"car, bike"};
        }

        Config config = RunStuttgartWedekindCalibration.prepareConfig(bikeTeleportedModeSpeed, bikeIntermodalTransferCosts, chainModes, arguments);

        // -- PARKING SWITCH --
        String parkingZoneShapeFileName;

        switch(parking) {
            case LOW_FARE_INCREASE:
                parkingZoneShapeFileName = "parkingShapes_lowFareIncrease.shp";

            case MEDIUM_FARE_INCREASE:
                parkingZoneShapeFileName = "parkingShapes_mediumFareIncrease.shp";

            case HIGH_FARE_INCREASE:
                parkingZoneShapeFileName = "parkingShapes_highFareIncrease.shp";

            default:
                parkingZoneShapeFileName = "parkingShapes_baseCase.shp";
        }


        // -- PT FARES SWITCH --
        String fareZonesShapeFileName;

        switch(ptFares) {
            case LOW_FARE_DECREASE:
                fareZonesShapeFileName = "fareZones_lowFareDecrease.shp";

            case MEDIUM_FARE_DECREASE:
                fareZonesShapeFileName = "fareZones_mediumFareDecrease.shp";

            case HIGH_FARE_DECREASE:
                fareZonesShapeFileName = "fareZones_highFareDecrease.shp";

            default:
                fareZonesShapeFileName = "fareZones_baseCase.shp";
        }

        Scenario scenario = RunStuttgartWedekindCalibration.prepareScenario(config, fareZonesShapeFileName, parkingZoneShapeFileName);


        // -- PT NETWORK IMPROVEMENTS --
        //ToDo: PT network improvements


        // -- ROAD INFRASTRUCTURE CLOSURE --
        //ToDo: Road infrastructure closure

        Controler controler = RunStuttgartWedekindCalibration.prepareControler(scenario);
        controler.run();
    }

    private static class RunArguments {

        @Parameter(names = "-inputDir", required = true)
        String inputDir;

        @Parameter(names = "-outputDir", required = true)
        String outputDir;

        @Parameter(names = "-runId", required = true)
        String runId;

        @Parameter(names = "-countsWeight")
        double countsWeight = 150;

        @Parameter(names = "-marginalsWeight")
        double marginalsWeight = 150;

        @Parameter(names = "-startMode")
        String startMode = "pt";
    }

    enum BikeAndRideSwitch {
        BASE_CASE,
        LOW_IMPROVEMENT,
        MEDIUM_IMPROVEMENT,
        HIGH_IMPROVEMENT
    }

    enum ParkingSwitch {
        BASE_CASE,
        LOW_FARE_INCREASE,
        MEDIUM_FARE_INCREASE,
        HIGH_FARE_INCREASE
    }

    enum PTFaresSwitch {
        BASE_CASE,
        LOW_FARE_DECREASE,
        MEDIUM_FARE_DECREASE,
        HIGH_FARE_DECREASE
    }*/

}
