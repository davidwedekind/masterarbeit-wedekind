/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.stuttgart.prepare;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;
import org.matsim.stuttgart.Utils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author ikaddoura
 */

public class CleanFacilities {

    private static final Logger log = Logger.getLogger(CleanFacilities.class);
    private static final String inputFacilities = "projects\\matsim-stuttgart\\stuttgart-v0.0-snz-original\\optimizedFacilities.xml.gz";
    private static final String outputFacilities = "projects\\matsim-stuttgart\\stuttgart-v2.0\\input\\facilities-stuttgart.xml.gz";

    public static void main(String[] args) {

        var arguments = Utils.parseSharedSvn(args);
        clean(Paths.get(arguments.getSharedSvn()));
    }

    public static void clean(Path sharedSvn) {


        Config config = ConfigUtils.createConfig();
        config.facilities().setInputFile(sharedSvn.resolve(inputFacilities).toString());
        Scenario scInput = ScenarioUtils.loadScenario(config);

        var scOutput = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        ActivityFacilities facilities = scOutput.getActivityFacilities();

        ActivityFacilitiesFactoryImpl fact = new ActivityFacilitiesFactoryImpl();

        for (ActivityFacility fac : scInput.getActivityFacilities().getFacilities().values()) {

            ActivityFacility newFacility = fact.createActivityFacility(fac.getId(), fac.getCoord());

            for (ActivityOption option : fac.getActivityOptions().values()) {
                newFacility.addActivityOption(option);
            }
            facilities.addActivityFacility(newFacility);
        }

        log.info("Writing...");
        new FacilitiesWriter(facilities).write(sharedSvn.resolve(outputFacilities).toString());
        log.info("Writing... Done.");
    }
}