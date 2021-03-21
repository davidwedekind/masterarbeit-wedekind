package org.matsim.masterThesis.ptModifiers;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;


public class TSReaderTest {
    private static final Logger log = Logger.getLogger(TSReaderTest.class);

    @Test
    public void testTransitSchedulerReader()  {

        Config config_1 = ConfigUtils.createConfig();
        Scenario scenario_1 = ScenarioUtils.createScenario(config_1);

        // read in existing files

        String transitSchedule_Berlin = "C:/Users/david/Desktop/berlin-v5.5-transit-schedule.xml.gz";
        new TransitScheduleReader(scenario_1).readFile(transitSchedule_Berlin);

        Config config_2 = ConfigUtils.createConfig();
        Scenario scenario_2 = ScenarioUtils.createScenario(config_2);

        // read in existing files
        String transitSchedule_SenozonStuttgart = "C:/Users/david/Desktop/optimizedSchedule.xml.gz";
        new TransitScheduleReader(scenario_2).readFile(transitSchedule_SenozonStuttgart);

    }


}