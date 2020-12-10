package org.matsim.stuttgart.prepare;

import org.matsim.stuttgart.Utils;

import java.nio.file.Paths;

public class PrepareScenario {

    public static void main(String[] args) {

        var arguments = Utils.parseSharedSvn(args);
        var svn = Paths.get(arguments.getSharedSvn());

        // have this scope so that the network can be collected by GC if not enough memory
        {
            // get network from osm
            var network = CreateNetwork.createNetwork(svn);

            // write pt schedule files and at pt routes to the network
            CreatePt.create(svn, network);

            // write the network with pt
            CreateNetwork.writeNetwork(network, svn);
        }

        // clean population from old network references and save it
        // downscaled populations are created via 'ReducePopulation'
        CleanPopulation.clean(svn);

        // clean facilities from old network references and save it
        CleanFacilities.clean(svn);

        // create vehicles
        CreateVehicleTypes.create(svn);
    }
}
