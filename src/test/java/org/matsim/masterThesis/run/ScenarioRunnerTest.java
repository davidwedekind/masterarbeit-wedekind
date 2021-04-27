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

        final String configPath = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/stuttgart-v1.0-0.001pct.config_m5_ex.xml";
        final String outputDir = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/output/m5_ex";



        final int lastIteration = 2;

        String[] args = new String[]{ configPath };
        Config config = ScenarioRunner.prepareConfig( args );

        // TEST MODIFIERS
        config.controler().setLastIteration(lastIteration);
        config.controler().setOutputDirectory(outputDir);

        Scenario scenario = ScenarioRunner.prepareScenario( config );
        ScenarioRunner.validateModifications( scenario );

        final Controler controler = ScenarioRunner.prepareControler( scenario );
        controler.run();

        // Exclude when having run time issues
        ScenarioRunner.postprocessing( controler );

    }

}
