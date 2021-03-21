package org.matsim.masterThesis.ptModifiers;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.MatsimVehicleReader;
import playground.vsp.andreas.utils.pt.TransitScheduleCleaner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TransitScheduleReducer {

    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);

        final String inputTransitSchedule = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/02_stuttgart-v1.0_test/input/optimizedSchedule.xml.gz";
        final String transitVehicles = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/02_stuttgart-v1.0_test/input/optimizedVehicles.xml.gz";
        final String networkPath = "C:/Users/david/OneDrive/02_Uni/02_Master/05_Masterarbeit/03_MATSim/02_runs/stuttgart-v1.0/02_stuttgart-v1.0_test/input/optimizedNetwork.xml.gz";
        final String outputTransitSchedule = "C:/Users/david/Desktop/reducedSchedule.xml";

        // read in existing files
        new TransitScheduleReader(scenario).readFile(inputTransitSchedule);
        new MatsimVehicleReader.VehicleReader(scenario.getTransitVehicles()).readFile(transitVehicles);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath);

        TransitSchedule tS = scenario.getTransitSchedule();
        List<String> lineNamesOfLinesToKeep = Arrays.asList("S 60 - 1", "S 2 - 12", "S 1 - 13");

        List<TransitLine> linesToRemove =  tS.getTransitLines().values().stream()
                .filter(transitLine -> !lineNamesOfLinesToKeep.contains(transitLine.getId().toString()))
                .collect(Collectors.toList());

        for (TransitLine line: linesToRemove){
            tS.removeTransitLine(line);
        }

        TransitScheduleCleaner.removeStopsNotUsed(tS);

        new TransitScheduleWriter(tS).writeFile(outputTransitSchedule);


    }
}
