package org.matsim.masterThesis;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.pt.utils.TransitScheduleValidator.ValidationResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ModifyTransitScheduleTest {
    private static final Logger log = Logger.getLogger(ModifyTransitScheduleTest.class);

    Path transitSchedulePath = Paths.get("C:/Users/david/Documents/03_Repositories/masterarbeit-wedekind/test/input/ptModifier/optimizedSchedule.xml");
    Path networkPath = Paths.get("C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/02_stuttgart-v1.0_test/input/optimizedNetwork.xml.gz");
    Path outputPath = Paths.get("C:/Users/david/Documents/03_Repositories/masterarbeit-wedekind/test/output/ptModifier/optimizedSchedule.xml");

    @Test
    public void testRemoveLine(){
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new TransitScheduleReader(scenario).readFile(transitSchedulePath.toString());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath.toString());

        ModifyPublicTransit modifier = new ModifyPublicTransit(scenario);
        modifier.removeLine("ALT 1002 - 1");

        new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(outputPath.toString());

    }

    @Test
    public void testShortenLine(){
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        TransitSchedule schedule = scenario.getTransitSchedule();
        new TransitScheduleReader(scenario).readFile(transitSchedulePath.toString());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath.toString());

        ValidationResult resultBeforeModifying = TransitScheduleValidator.validateAll(schedule, scenario.getNetwork());
        log.info("Transit validator results before modifying:");
        for (String errorMessage:resultBeforeModifying.getErrors()){
            log.error(errorMessage);
        }


        ModifyPublicTransit modifier = new ModifyPublicTransit(scenario);
        List<String> stopsToCancel = Arrays.asList("557985", "557983", "557997", "557980", "561004", "555473", "555472", "561003", "557977", "560458", "560457");
        modifier.shortenLine("Bus 120 - 11", stopsToCancel);

        ValidationResult result = TransitScheduleValidator.validateAll(schedule, scenario.getNetwork());

        ValidationResult resultAfterModifying = TransitScheduleValidator.validateAll(schedule, scenario.getNetwork());
        log.info("Transit validator results after modifying:");
        for (String errorMessage:resultAfterModifying.getErrors()){
            log.error(errorMessage);
        }


        new TransitScheduleWriter(schedule).writeFile(outputPath.toString());
    }

}