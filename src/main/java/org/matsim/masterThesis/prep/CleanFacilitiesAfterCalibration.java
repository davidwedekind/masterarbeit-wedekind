package org.matsim.masterThesis.prep;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;


public class CleanFacilitiesAfterCalibration {
    private static final Logger log = Logger.getLogger(CleanPopulationAfterCalibration.class);


    public static void main(String[] args) {
        var scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());

        CleanFacilitiesAfterCalibration.Input input = new CleanFacilitiesAfterCalibration.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Input facilities file: " + input.inputFacilities);
        log.info("Output facilities file: " + input.outputFacilities);

        log.info("loading facilities");
        new MatsimFacilitiesReader(scenario).readFile(input.inputFacilities);

        log.info("clean facilities");
        new CleanFacilitiesAfterCalibration().clean(scenario);

        log.info("write facilities output");
        new FacilitiesWriter(scenario.getActivityFacilities()).write(input.outputFacilities);

    }


    public void clean(Scenario scenario) {

        scenario.getActivityFacilities().getFacilities().values().parallelStream()
                .forEach(fac -> {
                    ((ActivityFacilityImpl)fac).setLinkId(null);
                });

    }


    private static class Input {

        @Parameter(names = "-inputFacilities")
        private String inputFacilities;

        @Parameter(names = "-outputFacilities")
        private String outputFacilities;

    }


}
