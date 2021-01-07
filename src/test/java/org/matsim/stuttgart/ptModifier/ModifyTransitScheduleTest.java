package org.matsim.stuttgart.ptModifier;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.pt.utils.TransitScheduleValidator.ValidationResult;
import org.matsim.stuttgart.run.RunStuttgartScenarios;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ModifyTransitScheduleTest {
    private static final Logger log = Logger.getLogger(ModifyTransitScheduleTest.class);

    Path transitSchedulePath = Paths.get("C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/02_stuttgart-v1.0_test/input/optimizedSchedule.xml.gz");
    Path networkPath = Paths.get("C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/02_stuttgart-v1.0_test/input/optimizedNetwork.xml.gz");
    Path outputPath = Paths.get("C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/02_stuttgart-v1.0_test/transitScheduleTest.xml");

    @Test
    public void testRemoveLine(){
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new TransitScheduleReader(scenario).readFile(transitSchedulePath.toString());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath.toString());

        ModifyTransitSchedule modifier = new ModifyTransitSchedule(scenario);
        modifier.removeLine("ALT 1002 - 1");
        List<String> stopsToCancel = Arrays.asList("801176.2", "800691", "800689.3");
        modifier.shortenLine("Bus 365 - 12", stopsToCancel);

        new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(outputPath.toString());

    }

    @Test
    public void testShortenLine(){
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        TransitSchedule schedule = scenario.getTransitSchedule();
        new TransitScheduleReader(scenario).readFile(transitSchedulePath.toString());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath.toString());

        ModifyTransitSchedule modifier = new ModifyTransitSchedule(scenario);
        List<String> stopsToCancel = Arrays.asList("557985", "557983", "557997", "557980", "561004", "555473", "555472", "561003", "557977", "560458", "560457");
        modifier.shortenLine("Bus 120 - 11", stopsToCancel);

        ValidationResult result = TransitScheduleValidator.validateAll(schedule, scenario.getNetwork());

        for (String s: result.getErrors()){
            log.error(s);
        };


        new TransitScheduleWriter(schedule).writeFile(outputPath.toString());
    }

}