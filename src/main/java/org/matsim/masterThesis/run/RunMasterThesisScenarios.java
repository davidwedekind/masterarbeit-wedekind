package org.matsim.masterThesis.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.masterThesis.BanCarsFromSmallerStreets;
import org.matsim.masterThesis.ModifyPublicTransit;
import org.matsim.stuttgart.run.StuttgartMasterThesisRunner;

/**
 * @author dwedekind
 */

public class RunMasterThesisScenarios {
    private static final Logger log = Logger.getLogger(RunMasterThesisScenarios.class);

    public RunMasterThesisScenarios(){

    }

    public static void main(String[] args) {

        for (String arg : args) {
            log.info( arg );
        }

        Config config = StuttgartMasterThesisRunner.prepareConfig(args, new StuttgartMasterThesisExperimentalConfigGroup()) ;

        // Adjust intermodal trip fare compensation
        IntermodalTripFareCompensatorsConfigGroup tripFareCompensatorsConfigGroup =
                (IntermodalTripFareCompensatorsConfigGroup) config.getModules().get("intermodalTripFareCompensators");
        IntermodalTripFareCompensatorConfigGroup tripFareCompensatorConfigGroup =
                (IntermodalTripFareCompensatorConfigGroup) tripFareCompensatorsConfigGroup.getParameterSets().get("intermodalTripFareCompensator");

        StuttgartMasterThesisExperimentalConfigGroup thesisExpConfigGroup =
                (StuttgartMasterThesisExperimentalConfigGroup) config.getModules().get("stuttgartMasterThesisExperimental");

        // Provide FareZoneShapeFile and ParkingZoneShapeFile to the scenario accordingly via the thesisExperimentalConfigGroup
        Scenario scenario = StuttgartMasterThesisRunner.prepareScenario(
                config,
                thesisExpConfigGroup.getFareZoneShapeFile(),
                thesisExpConfigGroup.getParkingZoneShapeFile());

        // If measure 'reducedCarInfrastructure' is switched on, then 'BanCarsFromSmallerStreets'
        if (thesisExpConfigGroup.getReducedCarInfrastructure()){
            new BanCarsFromSmallerStreets(scenario.getNetwork()).run(
                    thesisExpConfigGroup.getReducedCarInfrastructureShapeFile(),
                    thesisExpConfigGroup.getReducedCarInfrastructureFreespeedThreshold(),
                    thesisExpConfigGroup.getReducedCarInfrastructureCapacityThreshold());
        }

        // If atleast one 'ptExtension' is switched on, then modify pt (network, transit schedule)
        if (thesisExpConfigGroup.getPtNetworkExtensions() != null){
            ModifyPublicTransit modifier = new ModifyPublicTransit(scenario);

            if (thesisExpConfigGroup.getPtNetworkExtensions().contains(
                    StuttgartMasterThesisExperimentalConfigGroup.PtExtension.S60)){
                modifier.createS60Extension();
            }

            if (thesisExpConfigGroup.getPtNetworkExtensions().contains(
                    StuttgartMasterThesisExperimentalConfigGroup.PtExtension.U5)){
                modifier.createU5Extension();
            }

            if (thesisExpConfigGroup.getPtNetworkExtensions().contains(
                    StuttgartMasterThesisExperimentalConfigGroup.PtExtension.U6)){
                modifier.createU6Extension();
            }

            // Here possibly connection alignment Stuttgart Flughafen/ Messe as last measure
        }



        Controler controler = StuttgartMasterThesisRunner.prepareControler( scenario ) ;
        controler.run() ;

    }

}
