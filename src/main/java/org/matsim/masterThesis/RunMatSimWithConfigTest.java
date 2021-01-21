package org.matsim.masterThesis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.stuttgart.ptFares.PtFaresConfigGroup;

public class RunMatSimWithConfigTest {

    public static void main(String[] args) {

        String inputConfigPath = "C:/Users/david/desktop/configOutput.xml";
        String outputConfigPath = "C:/Users/david/desktop/configOutput_v2.xml";

        Config config = ConfigUtils.createConfig(inputConfigPath);

        PtFaresConfigGroup configGroup = ConfigUtils.addOrGetModule(config,
                PtFaresConfigGroup.class);

        if (configGroup.getParameterSets().isEmpty()) {

            PtFaresConfigGroup.FaresGroup faresGroup = new PtFaresConfigGroup.FaresGroup();
            faresGroup.setOutOfZonePrice(10.);
            faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(1, 1.));
            faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(2, 2.));
            faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(3, 3.));
            faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(4, 4.));
            faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(5, 5.));
            faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(6, 6.));
            faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(7, 7.));
            faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(8, 8.));
            configGroup.setFaresGroup(faresGroup);

        }

        Scenario scenario = ScenarioUtils.loadScenario(config);
        new ConfigWriter(config).write(outputConfigPath);

    }
}
