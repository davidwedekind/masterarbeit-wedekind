package org.matsim.stuttgart.prepare;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.stuttgart.Utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class CreateNetwork {

    private static final Logger log = Logger.getLogger(CreateNetwork.class);

    private static final String senozonNetworkPath = "projects\\matsim-stuttgart\\stuttgart-v0.0-snz-original\\optimizedNetwork.xml.gz";
    private static final String outputNetwork = "projects\\matsim-stuttgart\\stuttgart-v2.0\\input\\network-stuttgart.xml.gz";
    private static final String osmFile = "projects\\mosaik-2\\raw-data\\osm\\germany-20200715.osm.pbf";

    public static void main(String[] args) {

        var arguments = Utils.parseSharedSvn(args);
        var network = createNetwork(Paths.get(arguments.getSharedSvn()));
        writeNetwork(network, Paths.get(arguments.getSharedSvn()));
    }

    public static Network createNetwork(Path svnPath) {

        var bbox = getBBox(svnPath);

        log.info("Starting to parse osm network. This will not output anything for a while until it reaches the interesting part of the osm file.");

        var allowedModes = Set.of(TransportMode.car, TransportMode.ride); // maybe bike as well?

        var network = new SupersonicOsmNetworkReader.Builder()
                .setCoordinateTransformation(Utils.getTransformationWGS84ToUTM32())
                .setIncludeLinkAtCoordWithHierarchy((coord, id) -> bbox.covers(MGC.coord2Point(coord)))
                .setAfterLinkCreated((link, map, direction) -> link.setAllowedModes(allowedModes))
                .build()
                .read(svnPath.resolve(osmFile));

        log.info("Done parsing osm file. ");
        log.info("Starting network cleaner");

        var cleaner = new MultimodalNetworkCleaner(network);
        cleaner.run(Set.of(TransportMode.car));
        cleaner.run(Set.of(TransportMode.ride));

        log.info("Finished network cleaner");
        return network;
    }

    public static void writeNetwork(Network network, Path svn) {
        log.info("Writing network to " + svn.resolve(outputNetwork));
        new NetworkWriter(network).write(svn.resolve(outputNetwork).toString());

        log.info("");
        log.info("Finished \uD83C\uDF89");
    }

    private static PreparedGeometry getBBox(Path sharedSvn) {

        log.info("Reading senozon network");
        var senozonNetwork = NetworkUtils.createNetwork();
        new MatsimNetworkReader(senozonNetwork).readFile(sharedSvn.resolve(senozonNetworkPath).toString());

        return BoundingBox.fromNetwork(senozonNetwork).toGeometry();
    }
}
