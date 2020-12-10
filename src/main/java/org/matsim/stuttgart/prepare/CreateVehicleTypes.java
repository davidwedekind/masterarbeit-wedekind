package org.matsim.stuttgart.prepare;

import org.matsim.stuttgart.Utils;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CreateVehicleTypes {

    private static final String vehiclesFile = "projects\\matsim-stuttgart\\stuttgart-v2.0\\input\\vehicles-stuttgart.xml.gz";

    public static void main(String[] args) {

        var inputArgs = Utils.parseSharedSvn(args);
        create(Paths.get(inputArgs.getSharedSvn()));
    }

    public static void create(Path sharedSvn) {

        var container = VehicleUtils.createVehiclesContainer();
        var factory = VehicleUtils.getFactory();
        container.addVehicleType(Utils.createVehicleType("car", 7.5, 36.1111111111, 1, factory));
        container.addVehicleType(Utils.createVehicleType("ride", 7.5, 36.1111111111, 1, factory));

        new MatsimVehicleWriter(container).writeFile(sharedSvn.resolve(vehiclesFile).toString());
    }
}
