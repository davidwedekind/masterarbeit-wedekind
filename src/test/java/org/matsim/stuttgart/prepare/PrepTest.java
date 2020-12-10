package org.matsim.stuttgart.prepare;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTWriter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.stuttgart.prepare.AddAdditionalNetworkAttributes;
import org.matsim.stuttgart.prepare.PrepareTransitSchedule;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class PrepTest {

    Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
    String inputSchedule = "./test/input/prep/optimizedSchedule.xml.gz";
    String inputNetwork = "./test/input/prep/optimizedNetwork.xml.gz";
    String ptShapeFile = "./test/input/prep/fareZones_sp.shp";
    String parkingShapeFile = "./test/input/prep/parkingShapes.shp";


    @Test
    @Ignore
    public final void main() throws IOException {

        new TransitScheduleReader(scenario).readFile(inputSchedule);
        Network network = scenario.getNetwork();
        new MatsimNetworkReader(network).readFile(inputNetwork);

        // Add fareZones and VVSBikeAndRideStops
        PrepareTransitSchedule ptPreparer = new PrepareTransitSchedule();
        ptPreparer.run(scenario, ptShapeFile);

        // Add parking costs to network
        AddAdditionalNetworkAttributes parkingPreparer = new AddAdditionalNetworkAttributes();
        parkingPreparer.run(scenario, parkingShapeFile);

        testNetworkAttributeAdder();
        testTransitSchedulePreparer();

        // ToDo: Sample test Asserts

    }

    private void testTransitSchedulePreparer() throws IOException {

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        WKTWriter writer = new WKTWriter();

        TransitSchedule transitSchedule = scenario.getTransitSchedule();

        Map<String,String> transitStopsWkts = transitSchedule.getFacilities().values()
                .stream()
                .map(transitStopFacility -> {
                    Coordinate coord  =
                            new Coordinate(transitStopFacility.getCoord().getX(), transitStopFacility.getCoord().getY());
                    return Map.entry(transitStopFacility.getId().toString(),writer.write(geometryFactory.createPoint(coord)));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        FileWriter fileWriter = new FileWriter("./test/output/transitFacilities.csv");

        fileWriter.write("transitFacilitiesId,pointWKT,zone");
        fileWriter.write("\n");

        for (Map.Entry<String, String> entry : transitStopsWkts.entrySet()){

            String zone = transitSchedule.getFacilities().get(Id.create(entry.getKey(),TransitStopFacility.class)).getAttributes().getAttribute("ptFareZone").toString();

            fileWriter.write("\"" + entry.getKey() + "\",\"" + entry.getValue() + "\",\"" + zone + "\"");
            fileWriter.write("\n");
        }

        fileWriter.close();


    }

    private void testNetworkAttributeAdder() throws IOException {

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        WKTWriter writer = new WKTWriter();

        Network network = scenario.getNetwork();

        Map<String,String> parkingLinkWKTs = network.getLinks().values()
                .stream()
                .filter(link -> {
                    boolean r = false;
                    if (!link.getAllowedModes().contains("pt")) {
                        if ((Double) link.getAttributes().getAttribute("oneHourPCost") > 0.) {
                            r = true;
                        } else if (((Integer) link.getAttributes().getAttribute("maxPTime")) < 1800) {
                            r = true;
                        }
                    }

                    return r;
                })
                .map(link -> {
                    Coordinate[] coords  =
                            new Coordinate[] {new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()), new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY())};
                    return Map.entry(link.getId().toString(),writer.write(geometryFactory.createLineString(coords)));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        FileWriter fileWriter = new FileWriter("./test/output/parkingLinks.csv");

        fileWriter.write("linkId,linkWKT,zoneGroup,zoneName");
        fileWriter.write("\n");

        for (Map.Entry<String, String> entry : parkingLinkWKTs.entrySet()){

            String zoneGroup = network.getLinks().get(Id.createLinkId(entry.getKey())).getAttributes().getAttribute("zoneGroup").toString();
            String zoneName = network.getLinks().get(Id.createLinkId(entry.getKey())).getAttributes().getAttribute("zoneName").toString();

            fileWriter.write("\"" + entry.getKey() + "\",\"" + entry.getValue() + "\",\"" + zoneGroup + "\",\"" + zoneName + "\"");
            fileWriter.write("\n");
        }

        fileWriter.close();

    }


}
