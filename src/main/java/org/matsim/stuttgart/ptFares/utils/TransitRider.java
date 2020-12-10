package org.matsim.stuttgart.ptFares.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.List;

public class TransitRider{
    private Id<Person> personId;
    private final List<TransitTrip> trips = new ArrayList<>();
    private boolean onTransit = false;

    public TransitRider(Id<Person> personId){
        setId(personId);
    }

    public Id<Person> getId() {
        return personId;
    }

    public void setId(Id<Person> personId) {
        this.personId = personId;
    }

    public void startTrip(Id<TransitStopFacility> currentFacility){
        onTransit = true;
        trips.add(new TransitTrip());
        updateTrip(currentFacility);
    }

    public void closeTrip(){
        trips.get(trips.size() - 1).markTripAsFinished();
        onTransit = false;
    }

    public void updateTrip(Id<TransitStopFacility> currentFacility){
        trips.get(trips.size() - 1).updateStopSequence(currentFacility);
    }

    public boolean isOnTransit() {
        return onTransit;
    }

    public List<TransitTrip> getAllTrips(){
        return trips;
    }


    public static class TransitTrip{
        private final List<Id<TransitStopFacility>> stopSequence = new ArrayList<>();
        private boolean tripClosed = false;

        public void updateStopSequence(Id<TransitStopFacility> facilityId){
            if (tripClosed){
                throw new AssertionError("Trip of a transit rider is to be updated although already finished.");
            } else {
                stopSequence.add(facilityId);
            }
        }

        public void markTripAsFinished(){
            tripClosed = true;
        }

        public List<Id<TransitStopFacility>> getStopSequence(){
            return stopSequence;
        }

    }
}
