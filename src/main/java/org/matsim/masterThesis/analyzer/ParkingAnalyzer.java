package org.matsim.masterThesis.analyzer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import playground.vsp.simpleParkingCostHandler.ParkingCostConfigGroup;

import java.io.IOException;
import java.util.*;

/**
 * @author dwedekind
 *
 */

public class ParkingAnalyzer implements TransitDriverStartsEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
    private static final Logger log = Logger.getLogger(ParkingAnalyzer.class);

    private final Map<Id<Person>, Double> personId2lastLeaveVehicleTime = new HashMap<>();
    private final Map<Id<Person>, String> personId2previousActivity = new HashMap<>();
    private final Map<Id<Person>, Id<Link>> personId2relevantModeLinkId = new HashMap<>();

    private final Set<Id<Person>> ptDrivers = new HashSet<>();
    private final Set<Id<Person>> hasAlreadyPaidDailyResidentialParkingCosts = new HashSet<>();

    private final List<Parking> parkings = new ArrayList<>();

    private final ParkingCostConfigGroup parkingCostConfigGroup;
    private final Scenario scenario;


    private final String separator;
    private final String[] HEADER = {"parkingId", "vehicleId", "personId", "linkId",
            "activityType", "parkingType", "firstTimeResidentialParking", "overnight", "ruleViolation",
            "parkingStartTime", "parkingEndTime", "parkingDuration", "parkingFee"};


    public ParkingAnalyzer(Scenario scenario){
        this.scenario = scenario;
        this.parkingCostConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), ParkingCostConfigGroup.class);
        this.separator = scenario.getConfig().global().getDefaultDelimiter();
    }

    @Override
    public void reset(int iteration) {
        this.personId2lastLeaveVehicleTime.clear();
        this.personId2previousActivity.clear();
        this.personId2relevantModeLinkId.clear();
        this.ptDrivers.clear();
    }


    @Override
    public void handleEvent(TransitDriverStartsEvent event) {
        ptDrivers.add(event.getDriverId());
    }


    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (ptDrivers.contains(event.getPersonId())) {
            // skip pt drivers
        } else {
            if (!(StageActivityTypeIdentifier.isStageActivity(event.getActType()))) {

                personId2previousActivity.put(event.getPersonId(), event.getActType());

                if (personId2relevantModeLinkId.get(event.getPersonId()) != null) {
                    personId2relevantModeLinkId.remove(event.getPersonId());
                }
            }
        }
    }


    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (! ptDrivers.contains(event.getPersonId())) {
            // There might be several departures during a single trip.
            if (event.getLegMode().equals(parkingCostConfigGroup.getMode())) {
                personId2relevantModeLinkId.put(event.getPersonId(), event.getLinkId());
            }
        }
    }


    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (! ptDrivers.contains(event.getPersonId())) {
            if (personId2relevantModeLinkId.get(event.getPersonId()) != null) {
                createParkingAndAddAtrributes(
                        event.getPersonId(),
                        event.getVehicleId(),
                        event.getTime()
                );

            }
        }

    }


    private void createParkingAndAddAtrributes(Id<Person> personId, Id<Vehicle> vehicleId, double depTime) {
        Link link = scenario.getNetwork().getLinks().get(personId2relevantModeLinkId.get(personId));
        Parking parking = new Parking(
                Id.create(parkings.size(), Parking.class),
                vehicleId,
                personId,
                link.getId()
        );
        parking.setActivityDuringParking(personId2previousActivity.get(personId));

        double parkingStartTime = 0.;
        if (personId2lastLeaveVehicleTime.get(personId) != null) {
            parkingStartTime = personId2lastLeaveVehicleTime.get(personId);
        }

        parking.setParkingStartTime(parkingStartTime);
        parking.setParkingEndTime(depTime);

        if (parkingCostConfigGroup.getActivityPrefixesToBeExcludedFromParkingCost().stream()
                .noneMatch(s -> personId2previousActivity.get(personId).startsWith(s))){

            parking.setFirstTimeResidentialParking(false);
            if (personId2previousActivity.get(personId).startsWith(parkingCostConfigGroup.getActivityPrefixForDailyParkingCosts())) {
                // daily residential parking costs
                parking.setParkingType(ParkingType.RESIDENTIAL);


                if (! hasAlreadyPaidDailyResidentialParkingCosts.contains(personId)){
                    hasAlreadyPaidDailyResidentialParkingCosts.add(personId);
                    parking.setFirstTimeResidentialParking(true);

                    double residentialParkingFeePerDay = 0.;
                    if (link.getAttributes().getAttribute(parkingCostConfigGroup.getResidentialParkingFeeAttributeName()) != null) {
                        residentialParkingFeePerDay = (double) link.getAttributes().getAttribute(parkingCostConfigGroup.getResidentialParkingFeeAttributeName());
                    }

                    if (residentialParkingFeePerDay > 0.) {
                        residentialParkingFeePerDay = -1. * residentialParkingFeePerDay;
                    }

                    parking.setParkingFee(residentialParkingFeePerDay);
                }

            } else {
                // other parking cost types
                parking.setParkingType(ParkingType.OPPORTUNITY);

                if (personId2lastLeaveVehicleTime.get(personId) != null) {
                    parkingStartTime = personId2lastLeaveVehicleTime.get(personId);
                }
                int parkingDurationHrs = (int) Math.ceil((depTime - parkingStartTime) / 3600.);

                double extraHourParkingCosts = 0.;
                if (link.getAttributes().getAttribute(parkingCostConfigGroup.getExtraHourParkingCostLinkAttributeName()) != null) {
                    extraHourParkingCosts = (double) link.getAttributes().getAttribute(parkingCostConfigGroup.getExtraHourParkingCostLinkAttributeName());
                }

                double firstHourParkingCosts = 0.;
                if (link.getAttributes().getAttribute(parkingCostConfigGroup.getFirstHourParkingCostLinkAttributeName()) != null) {
                    firstHourParkingCosts = (double) link.getAttributes().getAttribute(parkingCostConfigGroup.getFirstHourParkingCostLinkAttributeName());
                }

                double dailyParkingCosts = firstHourParkingCosts + 29 * extraHourParkingCosts;
                if (link.getAttributes().getAttribute(parkingCostConfigGroup.getDailyParkingCostLinkAttributeName()) != null) {
                    dailyParkingCosts = (double) link.getAttributes().getAttribute(parkingCostConfigGroup.getDailyParkingCostLinkAttributeName());
                }

                double maxDailyParkingCosts = dailyParkingCosts;
                if (link.getAttributes().getAttribute(parkingCostConfigGroup.getMaxDailyParkingCostLinkAttributeName()) != null) {
                    maxDailyParkingCosts = (double) link.getAttributes().getAttribute(parkingCostConfigGroup.getMaxDailyParkingCostLinkAttributeName());
                }

                double maxParkingDurationHrs = 30;
                if (link.getAttributes().getAttribute(parkingCostConfigGroup.getMaxParkingDurationAttributeName()) != null) {
                    maxParkingDurationHrs = (double) link.getAttributes().getAttribute(parkingCostConfigGroup.getMaxParkingDurationAttributeName());
                }

                double parkingPenalty = 0.;
                if (link.getAttributes().getAttribute(parkingCostConfigGroup.getParkingPenaltyAttributeName()) != null) {
                    parkingPenalty = (double) link.getAttributes().getAttribute(parkingCostConfigGroup.getParkingPenaltyAttributeName());
                }

                double costs = 0.;
                if (parkingDurationHrs > 0) {
                    costs += firstHourParkingCosts;
                    costs += (parkingDurationHrs - 1) * extraHourParkingCosts;
                }
                if (costs > dailyParkingCosts) {
                    costs = dailyParkingCosts;
                }
                if (costs > maxDailyParkingCosts) {
                    costs = maxDailyParkingCosts;
                }

                parking.setRuleViolation(false);
                if ((parkingDurationHrs > maxParkingDurationHrs)) {
                    parking.setRuleViolation(true);
                    if (costs < parkingPenalty){
                        costs = parkingPenalty;
                    }
                }

                if (costs > 0.) {
                    double amount = -1. * costs;
                    parking.setParkingFee(amount);
                }

            }

        }

        parkings.add(parking);
        personId2lastLeaveVehicleTime.remove(personId);

    }


    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        if (! ptDrivers.contains(event.getPersonId())) {
            personId2lastLeaveVehicleTime.put(event.getPersonId(), event.getTime());
        }
    }


    public void printResults(String path) {
        String fileName = path + "parkings.csv.gz";

        // Here, actually last parking procedure of each person would have to be closed and added to list
        // This is a bit an issue because when does last parking end? At midnight/ at simulation end time?
        // Currently, last parkings also do not have any cost effect to scoring (as the handler neglects them)
        // Hence, don't count last parkings in the analysis


        try {

            CSVPrinter csvPrinter = new CSVPrinter(IOUtils.getBufferedWriter(fileName),
                    CSVFormat.DEFAULT.withDelimiter(separator.charAt(0)).withHeader(HEADER));

            for (var parking: parkings){
                List<String> oneLine = getCSVRecords(parking);
                csvPrinter.printRecord(oneLine);
            }

            csvPrinter.close();
            log.info("person2Fare written to: " + fileName);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private List<String> getCSVRecords(Parking parking){
        List<String> oneLine = new ArrayList<>();
        oneLine.add(parking.getParkingId().toString());
        oneLine.add(parking.getVehicleId().toString());
        oneLine.add(parking.getPersonId().toString());
        oneLine.add(parking.getLinkId().toString());
        oneLine.add(parking.getActivityDuringParking());
        oneLine.add(parking.getParkingType().toString());
        oneLine.add(String.valueOf(parking.getFirstTimeResidentialParking()));
        oneLine.add(String.valueOf(parking.getOvernight()));
        oneLine.add(String.valueOf(parking.getRuleViolation()));
        oneLine.add(String.valueOf(parking.getParkingStartTime()));
        oneLine.add(String.valueOf(parking.getParkingEndTime()));
        oneLine.add(String.valueOf(parking.getParkingDuration()));
        oneLine.add(String.valueOf(parking.getParkingFee()));
        return oneLine;

    }





    private static class Parking{
        private final Id<Parking> parkingId;
        private Id<Person> personId;
        private Id<Vehicle> vehicleId;
        private Id<Link> linkId;
        private String activityDuringParking = null;
        private ParkingType parkingType = ParkingType.RESIDENTIAL;
        private boolean firstTimeResidentialParking = true;
        private boolean overnight = false;
        private boolean ruleViolation = false;
        private double parkingStartTime = 0.;
        private double parkingEndTime = 0.;
        private double parkingFee = 0.;

        Parking(Id<Parking> parkingId, Id<Vehicle> vehicleId, Id<Person> personId, Id<Link> linkId){
            this.parkingId = parkingId;
            this.personId = personId;
            this.vehicleId = vehicleId;
            this.linkId = linkId;
        }

        public Id<Parking> getParkingId(){
            return parkingId;
        }

        public Id<Person> getPersonId(){
            return personId;
        }

        public Id<Vehicle> getVehicleId(){
            return vehicleId;
        }

        public void setPersonId(Id<Person> personId){
            this.personId = personId;
        }

        public void setVehicleId(Id<Vehicle> vehicleId){
            this.vehicleId = vehicleId;
        }

        public Id<Vehicle> setVehicleId(){
            return vehicleId;
        }

        public void setLinkId(Id<Link> linkId){
            this.linkId = linkId;
        }

        public Id<Link> getLinkId(){
            return this.linkId;
        }

        public void setActivityDuringParking(String activityDuringParking){
            this.activityDuringParking = activityDuringParking;
        }

        public String getActivityDuringParking(){
            return this.activityDuringParking;
        }

        public void setParkingType(ParkingType parkingType){
            this.parkingType = parkingType;
        }

        public ParkingType getParkingType(){
            return this.parkingType;
        }

        public void setFirstTimeResidentialParking(boolean firstTimeResidentialParking){
            this.firstTimeResidentialParking = firstTimeResidentialParking;
        }

        public boolean getFirstTimeResidentialParking(){
            return this.firstTimeResidentialParking;
        }

        public void setOvernight(boolean overnight){
            this.overnight = overnight;
        }

        public boolean getOvernight(){
            return this.overnight;
        }

        public void setRuleViolation(boolean ruleViolation){
            this.ruleViolation = ruleViolation;
        }

        public boolean getRuleViolation(){
            return this.ruleViolation;
        }

        public void setParkingStartTime(double parkingStartTime){
            this.parkingStartTime = parkingStartTime;
        }

        public double getParkingStartTime(){
            return this.parkingStartTime;
        }

        public void setParkingEndTime(double parkingEndTime){
            if (parkingEndTime < this.parkingStartTime){
                log.error(String.format("Parking of id %s has parkingEndTime < parkingStartTime.", parkingId.toString()));
            }
            this.parkingEndTime = parkingEndTime;
        }

        public double getParkingEndTime(){
            return this.parkingEndTime;
        }

        public double getParkingDuration(){
            return this.parkingEndTime - this.parkingStartTime;
        }

        public void setParkingFee(double parkingFee){
            this.parkingFee = parkingFee;
        }

        public double getParkingFee(){
            return this.parkingFee;
        }

    }

    public enum ParkingType {
        RESIDENTIAL,
        OPPORTUNITY
    }

}
