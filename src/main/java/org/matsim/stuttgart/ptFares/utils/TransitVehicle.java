package org.matsim.stuttgart.ptFares.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.HashSet;
import java.util.Set;

public class TransitVehicle{
    private Id<Vehicle> vehicleId;
    private final Set<Id<Person>> personsOnboard = new HashSet<>();
    private Id<TransitStopFacility> lastTransitStopFacility;

    public TransitVehicle(Id<Vehicle> vehicleId){
        this.setId(vehicleId);
    }

    public Id<Vehicle> getId() {
        return vehicleId;
    }

    public void setId(Id<Vehicle> vehicleId) {
        this.vehicleId = vehicleId;
    }

    public void personEntersVehicle(Id<Person> personId){
        personsOnboard.add(personId);
    }

    public void personLeavesVehicle(Id<Person> personId){
        personsOnboard.remove(personId);
    }

    public Set<Id<Person>> getPersonsOnboard(){
        return personsOnboard;
    }

    public void updateLastTransitStopFacility(Id<TransitStopFacility> lastTransitStopFacility){
        this.lastTransitStopFacility = lastTransitStopFacility;
    }

    public Id<TransitStopFacility> getLastTransitStopFacility(){
        return lastTransitStopFacility;
    }

}
