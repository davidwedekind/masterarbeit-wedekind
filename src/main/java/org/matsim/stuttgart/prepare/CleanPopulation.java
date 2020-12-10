package org.matsim.stuttgart.prepare;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.stuttgart.Utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class CleanPopulation {

    private static final Logger log = Logger.getLogger(CleanPopulation.class);
    private static final Random random = new Random();
    private static final String inputPopulation = "projects\\matsim-stuttgart\\stuttgart-v0.0-snz-original\\optimizedPopulation.xml.gz";
    private static final String outputPopulation = "projects\\matsim-stuttgart\\stuttgart-v2.0\\input\\population-%dpct-stuttgart.xml.gz";

    public static void main(String[] args) {

        var arguments = Utils.parseSharedSvn(args);
        clean(Paths.get(arguments.getSharedSvn()));
    }

    public static void clean(Path sharedSvn) {
        var scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());

        log.info("loading population");
        new PopulationReader(scenario).readFile(sharedSvn.resolve(inputPopulation).toString());

        log.info("Removing route information and setting all legs to 'walk'");
        scenario.getPopulation().getPersons().values().parallelStream()
                .forEach(person -> {

                    var newPlan = PopulationUtils.createPlan();
                    var trips = TripStructureUtils.getTrips(person.getSelectedPlan());

                    var firstActivity = (Activity) person.getSelectedPlan().getPlanElements().get(0);
                    firstActivity.setLinkId(null);
                    newPlan.addActivity(firstActivity); // copy the first activity

                    // clear plans
                    person.setSelectedPlan(null);
                    person.getPlans().clear();

                    // add more activities and legs if there is more than one activity
                    for (var trip : trips) {

                        // put in all walk legs since we have to re-calibrate anyway
                        newPlan.addLeg(PopulationUtils.createLeg(TransportMode.walk));
                        var activity = trip.getDestinationActivity();
                        activity.setLinkId(null);
                        newPlan.addActivity(trip.getDestinationActivity());
                    }

                    person.addPlan(newPlan);
                });

        splitActivityTypesBasedOnDuration(scenario.getPopulation());

        new PopulationWriter(scenario.getPopulation()).write(sharedSvn.resolve(String.format(outputPopulation, 25)).toString());
        writeReducedPopulation(scenario, 0.4, sharedSvn.resolve(String.format(outputPopulation, 10)).toString());
        writeReducedPopulation(scenario, 0.04, sharedSvn.resolve(String.format(outputPopulation, 1)).toString());
        writeReducedPopulation(scenario, 0.004, sharedSvn.resolve(String.format(outputPopulation, 0)).toString());
    }

    private static void writeReducedPopulation(Scenario scenario, double fractionOfOriginal, String outputPath) {

        var reducedPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        for (Person person : scenario.getPopulation().getPersons().values()) {

            if (random.nextDouble() <= fractionOfOriginal) {
                reducedPopulation.addPerson(person);
            }
        }

        new PopulationWriter(reducedPopulation).write(outputPath);

    }

    /**
     * Split activities into typical durations to improve value of travel time savings calculation.
     */
    private static void splitActivityTypesBasedOnDuration(Population population) {

        final double timeBinSize_s = 600.;

        // Calculate activity durations for the next step
        for (Person p : population.getPersons().values()) {
            for (Plan plan : p.getPlans()) {
                for (PlanElement el : plan.getPlanElements()) {

                    if (!(el instanceof Activity))
                        continue;

                    Activity act = (Activity) el;
                    double duration = act.getEndTime().orElse(24 * 3600)
                            - act.getStartTime().orElse(0);

                    int durationCategoryNr = (int) Math.round((duration / timeBinSize_s));

                    if (durationCategoryNr <= 0) {
                        durationCategoryNr = 1;
                    }

                    String newType = act.getType() + "_" + (durationCategoryNr * timeBinSize_s);
                    act.setType(newType);

                }

                mergeOvernightActivities(plan);
            }
        }
    }

    private static void mergeOvernightActivities(Plan plan) {

        if (plan.getPlanElements().size() > 1) {
            Activity firstActivity = (Activity) plan.getPlanElements().get(0);
            Activity lastActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1);

            String firstBaseActivity = firstActivity.getType().split("_")[0];
            String lastBaseActivity = lastActivity.getType().split("_")[0];

            if (firstBaseActivity.equals(lastBaseActivity)) {
                double mergedDuration = Double.parseDouble(firstActivity.getType().split("_")[1]) + Double.parseDouble(lastActivity.getType().split("_")[1]);


                firstActivity.setType(firstBaseActivity + "_" + mergedDuration);
                lastActivity.setType(lastBaseActivity + "_" + mergedDuration);
            }
        }  // skipping plans with just one activity

    }
}
