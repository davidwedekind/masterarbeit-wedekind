package org.matsim.stuttgart.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

public class ScenarioCreator {

    private static final Logger log = Logger.getLogger(ScenarioCreator.class);

    public void run(String args[]) {
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

        Config config = RunStuttgartWedekindCalibration.prepareConfig(bikeTeleportedModeSpeed, bikeIntermodalTransferCosts, chainModes, args);

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
    }

}
