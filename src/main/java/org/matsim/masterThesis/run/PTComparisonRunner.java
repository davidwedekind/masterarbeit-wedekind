package org.matsim.masterThesis.run;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.masterThesis.prep.CleanPopulationAfterCalibration;
import java.net.URISyntaxException;


public class PTComparisonRunner {


    public static void main(String[] args) throws URISyntaxException {
        final Logger log = Logger.getLogger(ScenarioRunner.class);

        for (String arg : args) {
            log.info( arg );
        }

        Config config = ScenarioRunner.prepareConfig( args );

        // Limit subtour mode choice to pt only
        // Is this a good solution? Or should I rather remove subtour mode choice as replanning strategy
        // How do I limit that bike can only be used in pt_w_bike_allowed but not as single mode?

        String[] modes = new String[2];
        modes[0] = "pt";
        modes[1] = "pt_w_bike_allowed";
        config.subtourModeChoice().setModes(modes);

        // To remove after testing!!!
        config.controler().setLastIteration(1);

        Scenario scenario = ScenarioRunner.prepareScenario( config );

        // Reset mode of all existing trips in plans to pt
        PTComparisonRunner.resetPlans( scenario );

        ScenarioRunner.validateModifications( scenario );

        final Controler controler = ScenarioRunner.prepareControler( scenario );
        controler.run() ;

        ScenarioRunner.postprocessing( controler );

    }


    private static void resetPlans(Scenario scenario){
        scenario.getPopulation().getPersons().values().parallelStream()
                .forEach(person -> {

                    Plan selectedPlan = person.getSelectedPlan();
                    person.getPlans().clear();
                    person.addPlan(selectedPlan);
                    person.setSelectedPlan(selectedPlan);

                    TripsToLegsAlgorithm algorithm = new TripsToLegsAlgorithm(new CleanPopulationAfterCalibration.RoutingModeIdentifier());
                    algorithm.run(selectedPlan);

                    selectedPlan.getPlanElements().stream()
                            .filter(planElement -> planElement instanceof Leg)
                            .map(planElement -> (Leg) planElement)
                            .forEach(leg -> leg.setMode(TransportMode.pt));

                });
    }


}
