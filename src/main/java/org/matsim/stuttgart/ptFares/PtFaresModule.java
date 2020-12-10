/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.stuttgart.ptFares;

import org.matsim.core.controler.AbstractModule;
import org.matsim.stuttgart.ptFares.utils.FareZoneCalculator;

/**
 * @author dwedekind, gleich
 */

public class PtFaresModule extends AbstractModule {

    @Override
    public void install() {

        // Specifically bind to this one instance
        // as pt fares handler is events handler and controler listener at the same time
        PtFaresHandler ptHandler = new PtFaresHandler();

        addEventHandlerBinding().toInstance(ptHandler);
        addControlerListenerBinding().toInstance(ptHandler);
        this.bind(FareZoneCalculator.class);


    }
}
