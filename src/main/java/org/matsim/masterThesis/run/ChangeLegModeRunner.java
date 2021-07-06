package org.matsim.masterThesis.run;


import com.sun.jdi.connect.Transport;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.masterThesis.prep.CleanPopulationAfterCalibration;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


public class ChangeLegModeRunner {


    public static void main(String[] args) throws URISyntaxException {
        final Logger log = Logger.getLogger(ScenarioRunner.class);

        for (String arg : args) {
            log.info( arg );
        }

        Config config = ScenarioRunner.prepareConfig( args );

        // Set modes for change mode strategy
        String[] modes = {TransportMode.walk, TransportMode.bike, TransportMode.car, TransportMode.ride, "pt_w_bike_allowed"};
        config.changeMode().setModes(modes);

        // Replace subtour mode choice replanning strategy by change mode
        Collection<StrategyConfigGroup.StrategySettings> settings = config.strategy().getStrategySettings()
                .stream()
                .filter(setting -> ! setting.getStrategyName().equals("SubtourModeChoice"))
                .collect(Collectors.toList());

        StrategyConfigGroup.StrategySettings changeTripModeSetting = new StrategyConfigGroup.StrategySettings();
        changeTripModeSetting.setStrategyName("ChangeTripMode");
        changeTripModeSetting.setWeight(0.2);

        settings.add(changeTripModeSetting);

        config.strategy().clearStrategySettings();

        for (var setting: settings){
            config.strategy().addStrategySettings(setting);
        }


        // To remove after testing!!!
        config.controler().setLastIteration(1);

        Scenario scenario = ScenarioRunner.prepareScenario( config );

        // Reset mode of all existing trips in plans to pt
        ChangeLegModeRunner.resetPlans( scenario );

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
