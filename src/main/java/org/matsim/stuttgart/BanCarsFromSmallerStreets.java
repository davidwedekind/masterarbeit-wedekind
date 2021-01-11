package org.matsim.stuttgart;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author dwedekind
 */

public class BanCarsFromSmallerStreets {
    private static final Logger log = Logger.getLogger(BanCarsFromSmallerStreets.class);
    private final Network network;


    public BanCarsFromSmallerStreets(Network network) {
        this.network = network;
    }


    public static void main(String[] args) {
        BanCarsFromSmallerStreets.Input input = new BanCarsFromSmallerStreets.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Input network file: " + input.inputNetwork);
        log.info("Input shape file: " + input.shapeFilePath);
        log.info("freespeedThreshold: " + input.freespeedThreshold);
        log.info("capacityThreshold: " + input.capacityThreshold);
        log.info("Output network file: " + input.outputNetwork);

        Network network = NetworkUtils.readNetwork(input.inputNetwork);
        new BanCarsFromSmallerStreets(network).run(input.shapeFilePath, input.freespeedThreshold, input.capacityThreshold);
        new NetworkWriter(network).write(input.outputNetwork);

    }


    public void run(String shapeFilePath, double freespeedThreshold, double capacityThreshold){
        final Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFilePath);

        log.info("Start removing mode 'car and ride' from links that are located in banZones and undergo threshold value...");

        network.getLinks().values().parallelStream()
                .filter(link -> !link.getId().toString().startsWith("tr"))
                .filter(link -> features.stream()
                        .map(simpleFeature -> (Geometry) simpleFeature.getDefaultGeometry())
                        .anyMatch(geometry -> geometry.covers(MGC.coord2Point(link.getCoord()))))
                .filter(link -> ((link.getFreespeed() < freespeedThreshold) && (link.getCapacity() < capacityThreshold)))
                .forEach(link -> {
                    var allowedModes = link.getAllowedModes().stream()
                            .filter(mode -> !mode.equals(TransportMode.car) && !mode.equals(TransportMode.ride))
                            .collect(Collectors.toSet());
                    link.setAllowedModes(allowedModes);
                });

        log.info("Network sucessfully modified!");
    }


    private static class Input {

        @Parameter(names = "-networkFile")
        private String inputNetwork;

        @Parameter(names = "-shapeFile")
        private String shapeFilePath;

        @Parameter(names = "-freespeedThreshold")
        private double freespeedThreshold;

        @Parameter(names = "-capacityThreshold")
        private double capacityThreshold;

        @Parameter(names = "-output")
        private String outputNetwork;

    }
}
