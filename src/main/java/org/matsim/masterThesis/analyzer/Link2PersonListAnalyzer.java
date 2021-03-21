package org.matsim.masterThesis.analyzer;


import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author dwedekind
 *
 */
public class Link2PersonListAnalyzer implements TransitDriverStartsEventHandler, LinkLeaveEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
    private static final Logger log = Logger.getLogger(Link2PersonListAnalyzer.class);

    private final double timeBinSize = 3600.;
    private int index = 0;
    private final Scenario sc;

    private final List<Id<Person>> ptDrivers = new ArrayList<>();
    private final Map<Id<Vehicle>,List<Id<Person>>> vehicleId2PersonIds = new HashMap<>();

    private final SortedMap<Integer, Double> index2Time = new TreeMap<>();
    private final SortedMap<Integer, Id<Person>> index2PersonId = new TreeMap<>();
    private final SortedMap<Integer, Id<Link>> index2LinkId = new TreeMap<>();
    private final SortedMap<Integer, Id<Vehicle>> index2VehicleId = new TreeMap<>();

    private final String separator;
    private final String[] HEADER = {"id", "linkId", "vehicleId", "personId",
            "time", "timeBin", "mode", "ptSubmode", "ptLine",
            "ptRoute", "ptDep"};


    public Link2PersonListAnalyzer(Scenario sc){
        this.sc = sc;
        this.separator = sc.getConfig().global().getDefaultDelimiter();
    }

    @Override
    public void reset(int iteration) {
        index = 0;
        vehicleId2PersonIds.clear();
        index2Time.clear();
        index2PersonId.clear();
        index2LinkId.clear();
        index2VehicleId.clear();
    }

    @Override
    public void handleEvent(TransitDriverStartsEvent event) {
        ptDrivers.add(event.getDriverId());
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        if (sc.getTransitVehicles().getVehicles().containsKey(event.getVehicleId())){
            vehicleId2PersonIds.computeIfAbsent(event.getVehicleId(), vehicleId -> new ArrayList<>());
            for (var personId: vehicleId2PersonIds.get(event.getVehicleId())){
                index2Time.put(index, event.getTime());
                index2PersonId.put(index, personId);
                index2LinkId.put(index, event.getLinkId());
                index2VehicleId.put(index, event.getVehicleId());
                index = index + 1;
            }
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (sc.getTransitVehicles().getVehicles().containsKey(event.getVehicleId())){
            if (!ptDrivers.contains(event.getPersonId())){
                vehicleId2PersonIds.computeIfAbsent(event.getVehicleId(), vehicleId -> new ArrayList<>());
                vehicleId2PersonIds.get(event.getVehicleId()).add(event.getPersonId());
            }
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        if (sc.getTransitVehicles().getVehicles().containsKey(event.getVehicleId())){
            if (!ptDrivers.contains(event.getPersonId())){
                vehicleId2PersonIds.get(event.getVehicleId()).remove(event.getPersonId());
            }
        }
    }

    public void printResults(String path) {
        String fileName = path + "person2PtLinkList.csv.gz";

            try {

                CSVPrinter csvPrinter = new CSVPrinter(IOUtils.getBufferedWriter(fileName),
                        CSVFormat.DEFAULT.withDelimiter(separator.charAt(0)).withHeader(HEADER));

                for (int i = 0; i < index; i++){
                    csvPrinter.printRecord(getCSVRecords(i));
                    }
                csvPrinter.close();

                log.info("Output written to: " + fileName);

            } catch (IOException e) {
                e.printStackTrace();
            }

    }

    private List<String> getCSVRecords(int index){
        Id<Vehicle> vehicleId = index2VehicleId.get(index);
        Id<TransitLine> transitLineId = findTransitLine(vehicleId);
        Id<TransitRoute> transitRouteId = findTransitRoute(transitLineId, vehicleId);

        List<String> oneLine = new ArrayList<>();
        oneLine.add(String.valueOf(index));
        oneLine.add(index2LinkId.get(index).toString());
        oneLine.add(index2VehicleId.get(index).toString());
        oneLine.add(index2PersonId.get(index).toString());
        oneLine.add(Time.writeTime(index2Time.get(index), Time.TIMEFORMAT_HHMMSS));
        oneLine.add(Time.writeTime((Math.floor(index2Time.get(index) / this.timeBinSize) + 1) * this.timeBinSize, Time.TIMEFORMAT_HHMMSS));
        oneLine.add("pt");
        oneLine.add(findPtSubmode(transitLineId));
        oneLine.add(transitLineId.toString());
        oneLine.add(transitRouteId.toString());
        oneLine.add(findDepartureId(transitLineId, transitRouteId, vehicleId).toString());

        return oneLine;
    }

    private Id<TransitLine> findTransitLine(Id<Vehicle> vehicleId) {
        Id<TransitLine> transitLineId = null;

        for (var transitLine: sc.getTransitSchedule().getTransitLines().values()){
            for (var transitRoute: transitLine.getRoutes().values()){
                for (var dep: transitRoute.getDepartures().values()){
                    if (dep.getVehicleId().equals(vehicleId)){
                        transitLineId = transitLine.getId();
                        break;
                    }
                }
            }
        }

        return transitLineId;
    }

    private Id<TransitRoute> findTransitRoute(Id<TransitLine> transitLineId, Id<Vehicle> vehicleId){
        Id<TransitRoute> transitRouteId = null;

        for (var transitRoute: sc.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().values()) {
            for (var dep : transitRoute.getDepartures().values()) {
                if (dep.getVehicleId().equals(vehicleId)) {
                    transitRouteId = transitRoute.getId();
                    break;
                }
            }
        }

        return transitRouteId;
    }

    private Id<Departure> findDepartureId(Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId, Id<Vehicle> vehicleId){
        Id<Departure> transitDepartureId = null;
        for (var dep : sc.getTransitSchedule().getTransitLines().get(transitLineId).getRoutes().get(transitRouteId).getDepartures().values()) {
            if (dep.getVehicleId().equals(vehicleId)) {
                transitDepartureId = dep.getId();
                break;
            }
        }

        return transitDepartureId;
    }

    private String findPtSubmode(Id<TransitLine> transitLineId){
        if (transitLineId.toString().startsWith("Bus")){
            return "bus";
        } else if (transitLineId.toString().startsWith("STB")){
            return "stb";
        } else if (transitLineId.toString().startsWith("SL ")){
            return "hyperloop";
        } else if (transitLineId.toString().startsWith("S ")){
            return "sbahn";
        } else {
            return "dbregio";
        }
    }


}


