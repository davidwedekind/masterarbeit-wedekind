package org.matsim.masterThesis;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
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
        log.info("Output network file: " + input.outputNetwork);

        Network network = NetworkUtils.readNetwork(input.inputNetwork);
        new BanCarsFromSmallerStreets(network).run(input.shapeFilePath);
        new NetworkWriter(network).write(input.outputNetwork);

    }


    public void run(String shapeFilePath){
        final Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFilePath);

        log.info("Start removing mode 'car and ride' from links that are located in banZones and undergo both threshold pairs (if exist) ...");

        network.getLinks().values().parallelStream()
                .filter(link -> !link.getId().toString().startsWith("tr"))
                .filter(link -> {

                    var feature = features.stream()
                            .filter(simpleFeature -> ((Geometry) simpleFeature.getDefaultGeometry()).covers(MGC.coord2Point(link.getCoord())))
                            .findFirst();

                    if (feature.isPresent()) {
                        var capacityThreshold_1 = feature.get().getAttribute("capTh1");
                        var freeSpeedThreshold_1 = feature.get().getAttribute("frSpTh1");
                        var capacityThreshold_2 = feature.get().getAttribute("capTh2");
                        var freeSpeedThreshold_2 = feature.get().getAttribute("frSpTh2");

                        if (capacityThreshold_1 != null && capacityThreshold_2 == null) {
                            return !(link.getFreespeed() >= (double) freeSpeedThreshold_1) || !(link.getCapacity() >= (double) capacityThreshold_1);

                        } else if (capacityThreshold_1 != null){
                            return (!(link.getFreespeed() >= (double) freeSpeedThreshold_1) || !(link.getCapacity() >= (double) capacityThreshold_1)) &&
                                    (!(link.getFreespeed() >= (double) freeSpeedThreshold_2) || !(link.getCapacity() >= (double) capacityThreshold_2));

                        }

                    }
                    return false;

                })
                .forEach(link -> {
                            var allowedModes = link.getAllowedModes().stream()
                                    .filter(mode -> !mode.equals(TransportMode.car) && !mode.equals(TransportMode.ride))
                                    .collect(Collectors.toSet());
                            link.setAllowedModes(allowedModes);
                        });


        log.info("Links successfully adjusted!");
        log.info("Start cleaning the network for modes car and ride...");

        Set<String> modes = new HashSet<>();
        modes.add(TransportMode.car);
        modes.add(TransportMode.ride);
        new MultimodalNetworkCleaner(network).run(modes);

        log.info("Network sucessfully modified!");
    }


    private static class Input {

        @Parameter(names = "-networkFile")
        private String inputNetwork;

        @Parameter(names = "-shapeFile")
        private String shapeFilePath;

        @Parameter(names = "-output")
        private String outputNetwork;

    }
}
