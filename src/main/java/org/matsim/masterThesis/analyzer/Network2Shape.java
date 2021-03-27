/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.masterThesis.analyzer;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory.Builder;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author ikaddoura
 * @author dwedekind
 */

public class Network2Shape {
    private final static Logger log = Logger.getLogger(Network2Shape.class);

    public static void main(String[] args) {
        Network2Shape.Input input = new Network2Shape.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Network file: " + input.network);
        String outputDirectory = Paths.get(input.network).getParent().toString();

        Config config = ConfigUtils.createConfig();
        config.controler().setRunId(input.runId);
        config.global().setCoordinateSystem("epsg:25832");
        config.network().setInputFile(input.network);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Network2Shape.exportNetwork2Shp(scenario, outputDirectory, "epsg:25832", TransformationFactory.getCoordinateTransformation("epsg:25832", "epsg:25832"));
    }


    public static void exportNetwork2Shp(Scenario scenario, String outputDirectory, String scenarioCrs, CoordinateTransformation ct){

        String outputPath = outputDirectory + "/network-shp/";
        File file = new File(outputPath);
        file.mkdirs();

        log.info("Writing shape file...");

        Builder featureFactoryBuilder = new PolylineFeatureFactory.Builder();
        CoordinateReferenceSystem crs;
        try {
            crs = MGC.getCRS(scenarioCrs);
            featureFactoryBuilder.setCrs(crs);
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("Assuming all coordinates to be in the correct coordinate reference system.");
            crs = null;
        }


        // Standard Attributes
        featureFactoryBuilder.setName("Link");
        featureFactoryBuilder.addAttribute("Id", String.class);
        featureFactoryBuilder.addAttribute("Length", Double.class);
        featureFactoryBuilder.addAttribute("capacity", Double.class);
        featureFactoryBuilder.addAttribute("lanes", Double.class);
        featureFactoryBuilder.addAttribute("Freespeed", Double.class);
        featureFactoryBuilder.addAttribute("Modes", String.class);

        // Additional Attributes
        featureFactoryBuilder.addAttribute("zone_name", String.class);
        featureFactoryBuilder.addAttribute("zone_group", String.class);
        featureFactoryBuilder.addAttribute("h_costs", Double.class);
        featureFactoryBuilder.addAttribute("dmax_costs", Double.class);
        featureFactoryBuilder.addAttribute("max_time", Double.class);
        featureFactoryBuilder.addAttribute("penalty", Double.class);
        featureFactoryBuilder.addAttribute("res_costs", Double.class);

        PolylineFeatureFactory featureFactory = featureFactoryBuilder.create();

        Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();

        for (Link link : scenario.getNetwork().getLinks().values()){

            Coord fromCoord = ct.transform(link.getFromNode().getCoord());
            Coord toCoord = ct.transform(link.getToNode().getCoord());

            Coordinate[] coordinates = new Coordinate[]{ new Coordinate(MGC.coord2Coordinate(fromCoord)), new Coordinate(MGC.coord2Coordinate(toCoord))};
            Map<String, Object> attributes = new LinkedHashMap<String, Object>();

            // Standard Attributes
            attributes.put("Id", link.getId().toString());
            attributes.put("Length", link.getLength());
            attributes.put("capacity", link.getCapacity());
            attributes.put("lanes", link.getNumberOfLanes());
            attributes.put("Freespeed", link.getFreespeed());
            attributes.put("Modes", String.join(",", link.getAllowedModes()));

            // Additional Attributes
            // Note: OneHourPCosts and extraHourPCost have the same value in this scenario
            attributes.put("zone_name", (String) link.getAttributes().getAttribute("zoneName"));
            attributes.put("zone_group", (String) link.getAttributes().getAttribute("zoneGroup"));
            attributes.put("h_costs", (Double) link.getAttributes().getAttribute("oneHourPCost"));
            attributes.put("dmax_costs", (Double) link.getAttributes().getAttribute("maxDailyPCost"));
            attributes.put("max_time", (Double) link.getAttributes().getAttribute("maxPTime"));
            attributes.put("penalty", (Double) link.getAttributes().getAttribute("pFine"));
            attributes.put("res_costs", (Double) link.getAttributes().getAttribute("resPCosts"));

            SimpleFeature feature = featureFactory.createPolyline(coordinates , attributes , link.getId().toString());
            features.add(feature);
        }

        log.info("Writing network to shapefile... ");
        if (scenario.getConfig().controler().getRunId() == null) {
            ShapeFileWriter.writeGeometries(features, outputPath + "output_network.shp");
        } else {
            ShapeFileWriter.writeGeometries(features, outputPath + scenario.getConfig().controler().getRunId() + ".output_network.shp");
        }
        log.info("Writing network to shapefile... Done.");
    }


    private static class Input {

        @Parameter(names = "-network")
        private String network;

        @Parameter(names = "-runId")
        private String runId;

    }

}
