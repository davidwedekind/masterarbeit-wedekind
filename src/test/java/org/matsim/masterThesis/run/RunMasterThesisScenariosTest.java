package org.matsim.masterThesis.run;

import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;

public class RunMasterThesisScenariosTest {

    @Test
    @Ignore
    public void testRunnerWithAllExtensions() {

        String configInput = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/00_preparation/templates/stuttgart-v1.0-1pct.config_xxx.xml";
        RunMasterThesisScenarios.main(new String[]{configInput});

    }

}
