package org.matsim.stuttgart.prepare;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class BoundingBox {

    private static final Logger log = Logger.getLogger(BoundingBox.class);

    private double minX = Double.POSITIVE_INFINITY;
    private double minY = Double.POSITIVE_INFINITY;
    private double maxX = Double.NEGATIVE_INFINITY;
    private double maxY = Double.NEGATIVE_INFINITY;

    static BoundingBox fromNetwork(Network network) {
        log.info("Calculating bounding box of car network");
        // take the bounding box of the senozon network
        var bbox = new BoundingBox();
        for (Link link : network.getLinks().values()) {

            if (link.getAllowedModes().contains("car")) {
                bbox.adjust(link.getFromNode().getCoord());
                bbox.adjust(link.getToNode().getCoord());
            }
        }

        log.info("Done calculating bounding box of car network.");
        log.info("Bbox is: " + bbox.toString());

        return bbox;
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    public synchronized void adjust(Coord coord) {
        if (minX > coord.getX()) minX = coord.getX();
        if (minY > coord.getY()) minY = coord.getY();
        if (maxX < coord.getX()) maxX = coord.getX();
        if (maxY < coord.getY()) maxY = coord.getY();
    }

    PreparedGeometry toGeometry() {

        var gFactory = new GeometryFactory();
        var geometry = gFactory.createPolygon(new Coordinate[]{
                new Coordinate(getMinX(), getMinY()),
                new Coordinate(getMaxX(), getMinY()),
                new Coordinate(getMaxX(), getMaxY()),
                new Coordinate(getMinX(), getMaxY()),
                new Coordinate(getMinX(), getMinY())
        });

        var pFactory = new PreparedGeometryFactory();
        return pFactory.create(geometry);
    }
}