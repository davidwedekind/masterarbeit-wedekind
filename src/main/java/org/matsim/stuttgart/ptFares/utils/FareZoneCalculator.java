package org.matsim.stuttgart.ptFares.utils;
import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.stuttgart.ptFares.PtFaresConfigGroup;

import java.util.*;
import java.util.stream.Collectors;

public class FareZoneCalculator {
    private static final Logger log = Logger.getLogger(FareZoneCalculator.class);

    Scenario scenario;
    PtFaresConfigGroup ptFaresConfigGroup;
    Map<String, Set<String>> hybridZones;
    Set<String> baseZones;
    String outOfZoneTag;

    @Inject
    public FareZoneCalculator(Scenario scenario, PtFaresConfigGroup ptFaresConfigGroup){
        this.scenario = scenario;
        this.ptFaresConfigGroup = ptFaresConfigGroup;
        this.hybridZones = ptFaresConfigGroup.getZonesGroup().getAllHybridZones();
        this.baseZones = ptFaresConfigGroup.getZonesGroup().getAllBaseZones();
        this.outOfZoneTag = ptFaresConfigGroup.getZonesGroup().getOutOfZoneTag();

        fareZoneConsistencyChecker();
    }

    private void fareZoneConsistencyChecker() {

        boolean checkFailed = false;

        log.info("Start fare zone consistency checker...");

        // Step 1: Check that all zones in transit schedule are defined in config group
        Set<String> fareZonesInSchedule = scenario.getTransitSchedule().getFacilities().values().stream()
                .map(transitStopFacility -> (String) transitStopFacility.getAttributes().getAttribute(ptFaresConfigGroup.getPtFareZoneAttributeName()))
                .collect(Collectors.toSet());
        Set<String> fareZonesInConfig = ptFaresConfigGroup.getZonesGroup().getAllZones().stream()
                .map(PtFaresConfigGroup.ZonesGroup.Zone::getZoneName)
                .collect(Collectors.toSet());
        for (var fareZone:fareZonesInSchedule){
            if (! fareZonesInConfig.contains(fareZone) && ! fareZone.equals(ptFaresConfigGroup.getZonesGroup().getOutOfZoneTag())){
                log.error(String.format("Fare zone '%s' in transit schedule is not defined in fare zone config.", fareZone));
                checkFailed = true;
            }
        }


        // Step 2: Check that base zones are castable to integer
        // Stuttgart specific implementation
        try{
            for (String baseZone:ptFaresConfigGroup.getZonesGroup().getAllBaseZones()){
                Integer.parseInt(baseZone);
            }

        } catch (Exception e) {
            log.error("Fare zone names have to be castable to integer!");
            checkFailed = true;
        }


        // Step 3: Check if the correct number of fares is defined

        int numberOfZones = ptFaresConfigGroup.getZonesGroup().getAllBaseZones().size();
        for (int i = 0; i < numberOfZones; i++){
            if (! ptFaresConfigGroup.getFaresGroup().getAllFares().containsKey(i+1)){
                log.error(String.format("Fare with number of zones [ %s ] has to be defined.", i + 1));
                checkFailed = true;
            }
        }

        if (checkFailed){
            throw new AssertionError("Fare zone consistency checker has failed. See log for details!");
        } else {
            log.info("Successfully passed fare zone consistency checker...");
        }

    }

    public boolean isCompletelyInTariffArea(List<TransitRider.TransitTrip> trips ){

        String outOfZoneTag = ptFaresConfigGroup.getZonesGroup().getOutOfZoneTag();
        return ! determineDistinctZonesFromTrips(trips).contains(outOfZoneTag);

    }

    public int calculateMinNoZones(List<TransitRider.TransitTrip> trips){

        List<String> zones = determineDistinctZonesFromTrips(trips);
        // find hybrid zones in list
        List<Integer> hybridZoneIndices = zones.stream()
                .filter(hybridZones::containsKey)
                .map(zones::indexOf)
                .collect(Collectors.toList());

        if (hybridZoneIndices.isEmpty()){
            List<Integer> intZones = zones.stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            return (Collections.max(intZones) - Collections.min(intZones)) + 1;

        } else {

            // generate Combinations
            List<List<String>> zoneCombinations = new ArrayList<>();
            zoneCombinations.add(zones);
            int combinations = 2;
            for (var id: hybridZoneIndices){

                List<String> zoneAssignments = new ArrayList<>(hybridZones.get(zones.get(id)));
                for (int i = 0; i < combinations/2; i++){
                    zoneCombinations.add(new ArrayList<>(zones));
                    zoneCombinations.get(i).set(id, zoneAssignments.get(0));
                    zoneCombinations.get(i + (combinations/2)).set(id, zoneAssignments.get(1));
                }
                combinations *= 2;
            }

            // determine zoneRange for each combination
            List<Integer> zoneRanges = zoneCombinations.stream()
                    .map(list -> list.stream()
                        .map(Integer::parseInt)
                        .collect(Collectors.toList()))
                    .map(list -> Collections.max(list) - Collections.min(list))
                    .collect(Collectors.toList());

            return Collections.min(zoneRanges) + 1;
        }

    }


    public List<String> determineDistinctZonesFromTrips(List<TransitRider.TransitTrip> trips){

        TransitSchedule schedule = scenario.getTransitSchedule();
        String attributeName = ptFaresConfigGroup.getPtFareZoneAttributeName();


        return trips.stream()
                .flatMap(trip -> trip.getStopSequence().stream())
                .map(facilityId -> schedule.getFacilities().get(facilityId).getAttributes().getAttribute(attributeName).toString())
                .distinct()
                .collect(Collectors.toList());
    }





}
