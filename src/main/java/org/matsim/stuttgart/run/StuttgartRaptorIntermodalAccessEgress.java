
package org.matsim.stuttgart.run;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ch.sbb.matsim.routing.pt.raptor.RaptorStopFinder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.misc.OptionalTime;

import com.google.inject.Inject;

import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;

/**
 * A default implementation of {@link RaptorIntermodalAccessEgress} returning a new RIntermodalAccessEgress,
 * which contains a list of legs (same as in the input), the associated travel time as well as the disutility.
 *
 * @author vsp-gleich / ikaddoura
 */

public class StuttgartRaptorIntermodalAccessEgress implements RaptorIntermodalAccessEgress {

    Config config;


    // for randomization per person, per mode, per direction (but same random value for one combination of this per routing request)
    Id<Person> lastPersonId = Id.createPersonId("");
    RaptorStopFinder.Direction lastDirection = RaptorStopFinder.Direction.EGRESS;
    Map<String, Double> lastModes2Randomization = new HashMap<>();

    Random random = MatsimRandom.getLocalInstance();

    @Inject
    StuttgartRaptorIntermodalAccessEgress(Config config) {
        this.config = config;

    }

    @Override
    public RIntermodalAccessEgress calcIntermodalAccessEgress( final List<? extends PlanElement> legs, RaptorParameters params, Person person,
                                                               RaptorStopFinder.Direction direction) {
        // maybe nicer using raptor parameters per person ?
        String subpopulationName = null;
        if (person.getAttributes() != null) {
            Object attr = person.getAttributes().getAttribute("subpopulation") ;
            subpopulationName = attr == null ? null : attr.toString();
        }

        ScoringParameterSet scoringParams = config.planCalcScore().getScoringParameters(subpopulationName);

        double utility = 0.0;
        double tTime = 0.0;
        for (PlanElement pe : legs) {
            if (pe instanceof Leg) {
                String mode = ((Leg) pe).getMode();
                OptionalTime travelTime = ((Leg) pe).getTravelTime();

                // overrides individual parameters per person; use default scoring parameters
                if (travelTime.isDefined()) {
                    tTime += travelTime.seconds();
                    utility += travelTime.seconds() * (scoringParams.getModes().get(mode).getMarginalUtilityOfTraveling() + (-1) * scoringParams.getPerforming_utils_hr()) / 3600;
                }
                Double distance = ((Leg) pe).getRoute().getDistance();
                if (distance != null && distance != 0.) {
                    utility += distance * scoringParams.getModes().get(mode).getMarginalUtilityOfDistance();
                    utility += distance * scoringParams.getModes().get(mode).getMonetaryDistanceRate() * scoringParams.getMarginalUtilityOfMoney();
                }
                utility += scoringParams.getModes().get(mode).getConstant();

                }




            }

        return new RIntermodalAccessEgress(legs, -utility, tTime, direction);

    }
}