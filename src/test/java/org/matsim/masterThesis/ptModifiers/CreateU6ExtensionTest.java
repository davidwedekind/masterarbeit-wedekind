package org.matsim.masterThesis.ptModifiers;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.utils.TransitScheduleValidator;


public class CreateU6ExtensionTest {
    private static final Logger log = Logger.getLogger(CreateU6ExtensionTest.class);

    @Test
    public void testRunExtensionModifications() throws Exception {
        final String transitSchedule = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/02_stuttgart-v1.0_test/input/optimizedSchedule.xml.gz";
        final String network = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/02_stuttgart-v1.0_test/input/optimizedNetwork.xml.gz";
        final String transitVehicles = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/02_stuttgart-v1.0_test/input/optimizedVehicles.xml.gz";
        final String output = "C:/Users/david/Documents/03_Repositories/masterarbeit-wedekind/test/output/ptModifier/optimizedSchedule.xml";
        final String shapeFile = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/01_prep/07_Measures/02_pt_extension/newU6.shp";
        final String epsgCode = "25832";

        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem("EPSG:" + epsgCode);
        config.transit().setTransitScheduleFile(transitSchedule);
        config.network().setInputFile(network);
        config.vehicles().setVehiclesFile(transitVehicles);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        new CreateU6Extension().runExtensionModifications(scenario, shapeFile);

        TransitScheduleValidator.ValidationResult resultAfterModifying = TransitScheduleValidator.validateAll(
                scenario.getTransitSchedule(), scenario.getNetwork());
        log.info("Transit validator results AFTER modifying:");
        for (String errorMessage:resultAfterModifying.getErrors()){
            throw new Exception(errorMessage);
        }
        log.info("----------------");

        new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(output);

    }


}