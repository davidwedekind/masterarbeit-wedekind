package org.matsim.stuttgart.ptFares;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.stuttgart.ptFares.utils.FareZoneCalculator;
import org.matsim.stuttgart.ptFares.utils.TransitRider;
import org.matsim.stuttgart.ptFares.utils.TransitVehicle;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.stream.Collectors;


public class PtFaresHandler implements TransitDriverStartsEventHandler, PersonLeavesVehicleEventHandler, VehicleArrivesAtFacilityEventHandler, PersonEntersVehicleEventHandler, AfterMobsimListener, ActivityStartEventHandler {
    private static final Logger log = Logger.getLogger( PtFaresHandler.class );
    private double compensationTime = Double.NaN;
    private final Set<Id<Person>> ptDrivers = new HashSet<>();
    private final Map<Id<Vehicle>, TransitVehicle> transitVehicles = new HashMap<>();
    private final Map<Id<Person>, TransitRider> transitRiders = new HashMap<>();


    @Inject
    private EventsManager events;

    @Inject
    private Scenario scenario;

    @Inject
    private QSimConfigGroup qSimConfigGroup;

    @Inject
    private PtFaresConfigGroup ptFaresConfigGroup;

    @Inject
    private FareZoneCalculator fareZoneCalculator;


    @Override
    public void reset(int iteration) {
        this.transitVehicles.clear();
        this.transitRiders.clear();
        this.ptDrivers.clear();
    }


    @Override
    public void handleEvent(TransitDriverStartsEvent event) {

        ptDrivers.add(event.getDriverId());
    }


    @Override
    public void handleEvent(VehicleArrivesAtFacilityEvent event) {
        if (scenario.getTransitVehicles().getVehicles().containsKey(event.getVehicleId())) {

            // case: vehicles first arrival of the day
            transitVehicles.computeIfAbsent(event.getVehicleId(), vehicleId ->
                    new TransitVehicle(event.getVehicleId()));

            // update vehicle position
            transitVehicles.get(event.getVehicleId()).
                    updateLastTransitStopFacility(event.getFacilityId());

            // update persons trip stop sequence
            for (var riderId:transitVehicles.get(event.getVehicleId()).getPersonsOnboard()){
                    transitRiders.get(riderId).updateTrip(event.getFacilityId());
            }
        }
    }


    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {

        // transit riders only
        if (! ptDrivers.contains(event.getPersonId()) &
                scenario.getTransitVehicles().getVehicles().containsKey(event.getVehicleId())){

            // case: first trip of the day
            transitRiders.computeIfAbsent(event.getPersonId(), TransitRider::new);

            // update persons onboard vehicle
            transitVehicles.get(event.getVehicleId()).personEntersVehicle(event.getPersonId());

            // update persons trip stop sequence
            if (transitRiders.get(event.getPersonId()).isOnTransit()){
                transitRiders.get(event.getPersonId()).updateTrip(
                        transitVehicles.get(event.getVehicleId()).getLastTransitStopFacility()
                );
            } else {
                transitRiders.get(event.getPersonId()).startTrip(
                        transitVehicles.get(event.getVehicleId()).getLastTransitStopFacility()
                );
            }

        }

    }


    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {

        // transit riders only
        if (! ptDrivers.contains(event.getPersonId()) &
                scenario.getTransitVehicles().getVehicles().containsKey(event.getVehicleId())){

            // update persons onboard vehicle
            transitVehicles.get(event.getVehicleId()).personLeavesVehicle(event.getPersonId());

        }

    }


    @Override
    public void handleEvent(ActivityStartEvent event) {

        if (transitRiders.containsKey(event.getPersonId())){

            if (transitRiders.get(event.getPersonId()).isOnTransit() &
                     (! event.getActType().startsWith(ptFaresConfigGroup.getPtInteractionPrefix()))){
                transitRiders.get(event.getPersonId()).closeTrip();
            }
        }

    }


    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        log.info("Start collecting transit fares...");

        Map<Id<Person>, List<TransitRider.TransitTrip>> riders2TransitTrips = transitRiders.entrySet().parallelStream()
                .collect(Collectors.toMap(Map.Entry::getKey, map -> map.getValue().getAllTrips()));

        // Trips completely in tariff zone
        riders2TransitTrips.entrySet().parallelStream()
                .filter(map -> fareZoneCalculator.isCompletelyInTariffArea(map.getValue()))
                .forEach(map -> {
                    Integer zones = fareZoneCalculator.calculateMinNoZones(map.getValue());
                    Double fare = ptFaresConfigGroup.getFaresGroup().getFare(zones);
                    fare *= -1;
                    events.processEvent(new PersonMoneyEvent(getOrCalcCompensationTime(),
                            map.getKey(),
                            fare,
                            "ptFares",
                            "ptAuthority"
                    ));
                });

        // Trips at least partly out of tariff zone
        riders2TransitTrips.entrySet().parallelStream()
                .filter(map -> (! fareZoneCalculator.isCompletelyInTariffArea(map.getValue())))
                .forEach(map -> {
                    double fare = ptFaresConfigGroup.getFaresGroup().getOutOfZonePrice();
                    fare *= -1;
                    events.processEvent(new PersonMoneyEvent(getOrCalcCompensationTime(),
                            map.getKey(),
                            fare,
                            "ptFares",
                            "ptAuthority"
                    ));
                });

        log.info("Transit fares successfully collected...");
    }


    private double getOrCalcCompensationTime() {
        if (Double.isNaN(this.compensationTime)) {
            this.compensationTime = (Double.isFinite(qSimConfigGroup.getEndTime().seconds()) && qSimConfigGroup.getEndTime().seconds() > 0)
                    ? qSimConfigGroup.getEndTime().seconds()
                    : Double.MAX_VALUE;
        }

        return this.compensationTime;
    }






}
