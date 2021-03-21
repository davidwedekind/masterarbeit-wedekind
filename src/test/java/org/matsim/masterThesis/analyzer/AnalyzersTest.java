package org.matsim.masterThesis.analyzer;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.masterThesis.run.ScenarioRunner;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.MatsimVehicleReader;


public class AnalyzersTest {
    final Logger log = Logger.getLogger(AnalyzersTest.class);

    @Test
    @Ignore
    public void testAnalyzers() {

        final String outputDir = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/output/output-m5_1";
        final String runId = "m5_1";

        final String tSFileName = outputDir + "/" + runId + ".output_transitSchedule.xml.gz";
        String vehiclesFileName = outputDir + "/" + runId + ".output_transitVehicles.xml.gz";

        Config config = ConfigUtils.createConfig();
        MutableScenario scenario = ScenarioUtils.createMutableScenario(config);

        new TransitScheduleReader(scenario).readFile(tSFileName);
        new MatsimVehicleReader(scenario.getTransitVehicles()).readFile(vehiclesFileName);

        Link2PersonListAnalyzer link2PersonListAnalyzer = new Link2PersonListAnalyzer(scenario);
        PTRevenueAnalyzer ptRevenueAnalyzer = new PTRevenueAnalyzer(scenario);

        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(link2PersonListAnalyzer);
        events.addHandler(ptRevenueAnalyzer);

        String eventsFile = outputDir + "/" + runId + ".output_events.xml.gz";

        MatsimEventsReader reader = new MatsimEventsReader(events);
        events.initProcessing();
        reader.readFile(eventsFile);
        events.finishProcessing();

        String outputFile = outputDir + "/" + runId + ".output_";
        link2PersonListAnalyzer.printResults(outputFile);
        ptRevenueAnalyzer.printResults(outputFile);

    }

}
