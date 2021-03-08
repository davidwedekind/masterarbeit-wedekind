package org.matsim.masterThesis.run;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

import java.net.URISyntaxException;


public class ScenarioRunnerTest {
    final Logger log = Logger.getLogger(ScenarioRunnerTest.class);

    @Test
    @Ignore
    public void testRunnerWithAllExtensions() throws URISyntaxException {
        final String configPath = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/stuttgart-v1.0-0.001pct.config_m5_1.xml";
        // final String configPath = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/stuttgart-v1.0-0.001pct.config_m1_extreme.xml";
        // final String configPath = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/stuttgart-v1.0-0.001pct.config_m2_extreme.xml";
        // final String configPath = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/stuttgart-v1.0-0.001pct.config_m3_extreme.xml";
        // final String configPath = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/stuttgart-v1.0-0.001pct.config_m4_extreme.xml";
        // final String configPath = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/stuttgart-v1.0-0.001pct.config_m5_2.xml";
        // final String configPath = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/stuttgart-v1.0-0.001pct.config_m5_3.xml";

        final String outputDir = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/output/output-m5_1";
        // final String outputDir = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/output/output-m1_extreme";
        // final String outputDir = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/output/output-m2_extreme";
        // final String outputDir = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/output/output-m3_extreme";
        // final String outputDir = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/output/output-m4_extreme";
        // final String outputDir = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/output/output-m5_2";
        // final String outputDir = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/output/output-m5_3";

        final int lastIteration = 1;

        String[] args = new String[]{ configPath };
        Config config = ScenarioRunner.prepareConfig( args );

        // TEST MODIFIERS
        config.controler().setLastIteration(lastIteration);
        config.controler().setOutputDirectory(outputDir);

        Scenario scenario = ScenarioRunner.prepareScenario( config );
        ScenarioRunner.validateModifications( scenario );

        final Controler controler = ScenarioRunner.prepareControler( scenario );
        controler.run();

        ScenarioRunner.postprocessing( controler );

    }

}
