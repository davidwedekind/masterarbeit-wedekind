package org.matsim.masterThesis.prep;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import java.util.List;

/**
 * @author dwedekind
 */

public class CleanPopulationAfterCalibration {
    private static final Logger log = Logger.getLogger(CleanPopulationAfterCalibration.class);


    public static void main(String[] args) {
        var scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());

        CleanPopulationAfterCalibration.Input input = new CleanPopulationAfterCalibration.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Input population file: " + input.inputPopulation);
        log.info("Output population file: " + input.outputPopulation);

        log.info("loading population");
        new PopulationReader(scenario).readFile(input.inputPopulation);

        log.info("Clean population file..");
        new CleanPopulationAfterCalibration().clean(scenario);

        log.info("Write population to file..");
        new PopulationWriter(scenario.getPopulation()).write(input.outputPopulation);

    }


    public void clean(Scenario scenario){
        // This method cleans all detailed routes from the population file
        // Hereby, only selected plans are regarded and kept

        scenario.getPopulation().getPersons().values().parallelStream()
                .forEach(person -> {

                    Plan selectedPlan = person.getSelectedPlan();
                    person.getPlans().clear();
                    person.addPlan(selectedPlan);
                    person.setSelectedPlan(selectedPlan);

                    TripsToLegsAlgorithm algorithm = new TripsToLegsAlgorithm(new RoutingModeIdentifier());
                    algorithm.run(selectedPlan);

                });

    }


    public static final class RoutingModeIdentifier implements AnalysisMainModeIdentifier {
        @Override
        public String identifyMainMode(List<? extends PlanElement> planElements) {
            return (String) planElements.get(0).getAttributes().getAttribute("routingMode");
        }

    }


    private static class Input {

        @Parameter(names = "-inputPopulation")
        private String inputPopulation;

        @Parameter(names = "-outputPopulation")
        private String outputPopulation;

    }


}
