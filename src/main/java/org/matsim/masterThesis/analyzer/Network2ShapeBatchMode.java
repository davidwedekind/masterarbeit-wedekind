package org.matsim.masterThesis.analyzer;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class Network2ShapeBatchMode {
    private final static Logger log = Logger.getLogger(Network2ShapeBatchMode.class);

    public static void main(String[] args) {
        Network2ShapeBatchMode.Input input = new Network2ShapeBatchMode.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Input directory: " + input.inputDir);
        log.info("Output directory: " + input.outputDir);

        List<String> subDirs = getAllSubDirectories(input.inputDir);


        // Execute the Network2Shape for all runs (output directories)
        for (var subDir: subDirs){
            String runId = subDir.replace("output-", "");
            Config config = ConfigUtils.createConfig();
            config.controler().setRunId(runId);
            config.global().setCoordinateSystem("epsg:25832");

            String networkFile = input.inputDir + "/" + subDir + "/" + runId + "" + ".output_network.xml.gz";
            config.network().setInputFile(networkFile);
            Scenario scenario = ScenarioUtils.loadScenario(config);

            String outputDirectory = input.outputDir;
            Network2Shape.exportNetwork2Shp(scenario, outputDirectory, "epsg:25832", TransformationFactory.getCoordinateTransformation("epsg:25832", "epsg:25832"));
        }

    }

    private static List<String> getAllSubDirectories(String dir){
        File file = new File(dir);
        return Arrays.asList(Objects.requireNonNull(file.list((current, name) -> new File(current, name).isDirectory())));
    }

    private static class Input {

        @Parameter(names = "-inputDir")
        private String inputDir;

        @Parameter(names = "-outputDir")
        private String outputDir;

    }
}
