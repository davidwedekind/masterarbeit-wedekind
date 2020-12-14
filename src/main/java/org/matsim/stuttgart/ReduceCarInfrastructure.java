package org.matsim.stuttgart;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author janekdererste, gleich
 * based on: https://github.com/matsim-vsp/mercator-nemo/blob/master/src/main/java/org/matsim/nemo/BanCarsFromLivingStreets.java
 */

public class ReduceCarInfrastructure {

    private static final String inputNetwork = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/ruhrgebiet/ruhrgebiet-v1.0-1pct/input/ruhrgebiet-v1.0-network-with-RSV.xml.gz";
    private static final String ruhrShape = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/ruhrgebiet/ruhrgebiet-v1.1-1pct/original_data/shapes/ruhrgebiet_boundary.shp";
    private static final String outputNetwork = "C:\\Users\\Janek\\repos\\shared-svn\\projects\\nemo_mercator\\data\\matsim_input\\healthy\\network.xml.gz";

    public static void main(String[] args) throws MalformedURLException {

        var network = NetworkUtils.readNetwork(inputNetwork);
        var shape = ShapeFileReader.getAllFeatures(new URL(ruhrShape)).stream()
                .map(feature -> (Geometry) feature.getDefaultGeometry())
                .collect(Collectors.toList());

        System.out.println("excluding cars on residential streets");
        // ban cars from residential streets
        network.getLinks().values().parallelStream()
                .filter(link -> link.getAttributes().getAttribute("type") != null)
                .filter(link -> link.getAttributes().getAttribute("type").equals("living_street") || link.getAttributes().getAttribute("type").equals("residential"))
                .filter(link -> shape.stream().anyMatch(g -> g.contains(MGC.coord2Point(link.getCoord()))))
                .forEach(link -> {
                    // remove car modes
                    var allowedModes = link.getAllowedModes().stream()
                            .filter(mode -> !mode.equals(TransportMode.car) && !mode.equals(TransportMode.ride))
                            .collect(Collectors.toSet());
                    link.setAllowedModes(allowedModes);
                });

        new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.car));
        new MultimodalNetworkCleaner(network).run(Set.of(TransportMode.ride));
        new NetworkWriter(network).write(outputNetwork);
    }
}
