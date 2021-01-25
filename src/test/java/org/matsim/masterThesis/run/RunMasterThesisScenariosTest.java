package org.matsim.masterThesis.run;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URISyntaxException;


public class RunMasterThesisScenariosTest {
    final Logger log = Logger.getLogger(RunMasterThesisScenariosTest.class);

    @Test
    @Ignore
    public void testRunnerWithAllExtensions() {

        try{

            RunMasterThesisScenarios.main(new String[]{
                    "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/stuttgart-v1.0-0.001pct.config_m1_extreme.xml",
                    "--config:controler.outputDirectory",
                    "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/output/output-m1_extreme"
            });

            RunMasterThesisScenarios.main(new String[]{
                    "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/stuttgart-v1.0-0.001pct.config_m2_extreme.xml",
                    "--config:controler.outputDirectory",
                    "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/output/output-m2_extreme"
            });

            RunMasterThesisScenarios.main(new String[]{
                    "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/stuttgart-v1.0-0.001pct.config_m3_extreme.xml",
                    "--config:controler.outputDirectory",
                    "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/output/output-m3_extreme"
            });

            RunMasterThesisScenarios.main(new String[]{
                    "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/stuttgart-v1.0-0.001pct.config_m4_extreme.xml",
                    "--config:controler.outputDirectory",
                    "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/output/output-m4_extreme"
            });

            RunMasterThesisScenarios.main(new String[]{
                    "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/stuttgart-v1.0-0.001pct.config_m5_extreme.xml",
                    "--config:controler.outputDirectory",
                    "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/05_stuttgart-v1.0_scenariotest/output/output-m5_extreme"
            });

        } catch (URISyntaxException e){
            log.error("URISyntaxException!: " + e);

        }




    }

}
