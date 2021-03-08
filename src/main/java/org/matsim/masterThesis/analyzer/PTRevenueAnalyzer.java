package org.matsim.masterThesis.analyzer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.stuttgart.ptFares.PtFaresConfigGroup;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dwedekind
 *
 */

public class PTRevenueAnalyzer implements PersonMoneyEventHandler {
    private static final Logger log = Logger.getLogger(PTRevenueAnalyzer.class);

    Map<Id<Person>,Integer> personId2NumberOfZones = new HashMap<>();
    Map<Id<Person>,Double> personId2FareAmount = new HashMap<>();

    private final String separator;
    private final String[] HEADER = {"personId", "outOfZones", "noZones", "fareAmount"};

    public PTRevenueAnalyzer(Scenario sc){
        this.separator = sc.getConfig().global().getDefaultDelimiter();
    }

    @Override
    public void reset(int iteration) {
        personId2NumberOfZones.clear();
        personId2FareAmount.clear();
    }

    @Override
    public void handleEvent(PersonMoneyEvent event) {
        if (event.getPurpose().startsWith("ptFare")){

            // Zones travelled through is stored in purpose string
            // Not so robust solution, but there is no other field in the personMoneyEvent where such parameter could be stored
            if (! event.getPurpose().equals("ptFare - outOfZone")){
                int numberOfZones = Integer.parseInt(event.getPurpose().replace("ptFare - ", "").replace(" zone(s)", ""));
                personId2NumberOfZones.put(event.getPersonId(), numberOfZones);
            }
            personId2FareAmount.put(event.getPersonId(), event.getAmount());
        }

    }

    public void printResults(String path) {
        String fileName = path + "person2Fare.csv.gz";

        try {

            CSVPrinter tripsCSVPrinter = new CSVPrinter(IOUtils.getBufferedWriter(fileName),
                    CSVFormat.DEFAULT.withDelimiter(separator.charAt(0)).withHeader(HEADER));

            for (var personId: personId2FareAmount.keySet()){
                List<String> oneLine = new ArrayList<>();

                oneLine.add(personId.toString());

                String outOfZoneString;
                String noZonesString;
                if (personId2NumberOfZones.containsKey(personId)){
                    outOfZoneString = "0";
                    noZonesString = personId2NumberOfZones.get(personId).toString();
                } else {
                    outOfZoneString = "1";
                    noZonesString = "";
                }
                oneLine.add(outOfZoneString);
                oneLine.add(noZonesString);
                oneLine.add(personId2FareAmount.get(personId).toString());

                tripsCSVPrinter.printRecord(oneLine);
            }

            tripsCSVPrinter.close();
            log.info("person2Fare written to: " + fileName);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
