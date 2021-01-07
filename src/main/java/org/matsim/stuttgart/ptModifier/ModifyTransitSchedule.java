package org.matsim.stuttgart.ptModifier;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.util.List;
import java.util.stream.Collectors;

public class ModifyTransitSchedule {
    private final Scenario scenario;
    private final TransitSchedule transitSchedule;
    private final Network network;


    public ModifyTransitSchedule(Scenario scenario){
        this.scenario = scenario;
        this.transitSchedule = scenario.getTransitSchedule();
        this.network = scenario.getNetwork();

    }


    public void removeLine(String transitLineName){
        TransitLine line = transitSchedule.getTransitLines().get(Id.create(transitLineName, TransitLine.class));
        transitSchedule.removeTransitLine(line);

    }


    public void shortenLine(String transitLineName, List<String> stopsToCancel){
        TransitLine line = transitSchedule.getTransitLines().get(Id.create(transitLineName, TransitLine.class));

        List<TransitRoute> routesToRemove = line.getRoutes().values().stream()
                .filter(route -> route.getStops().stream().anyMatch(stop -> {
                    String stopId = stop.getStopFacility().getId().toString();
                    return stopsToCancel.contains(stopId);
                }))
                .collect(Collectors.toList());

        List<TransitRoute> routesToAdd = routesToRemove.stream()
                .map(route -> copyAndAdjustRoute(route, stopsToCancel))
                .collect(Collectors.toList());

        for(TransitRoute route: routesToRemove) {
            line.removeRoute(route);
        }

        for(TransitRoute route: routesToAdd) {
            line.addRoute(route);

        }

    }


    public void extendLine(String transitLineName, List<String> stopsToAdd, List<String> linksToAdd, List<Integer> travelTimesToAdd){

    }


    public TransitRoute copyAndAdjustRoute(TransitRoute route, List<String> stopsToCancel) {

        List<Id<Link>> links = route.getRoute().getLinkIds().stream()
                .filter(linkId -> {
                    Link link = network.getLinks().get(linkId);
                    return ! stopsToCancel.contains(link.getToNode().getId().toString());
                })
                .collect(Collectors.toList());

        List<TransitRouteStop> stops = route.getStops().stream()
                .filter(transitRouteStop -> {
                    TransitStopFacility stopFacility = transitRouteStop.getStopFacility();
                    return ! stopsToCancel.contains(stopFacility.getId().toString());
                })
                .collect(Collectors.toList());

        TransitRoute newRoute = scenario.getTransitSchedule().getFactory().createTransitRoute(
                route.getId(),
                RouteUtils.createNetworkRoute(links),
                stops,
                route.getTransportMode());

        for (Departure departure: route.getDepartures().values()){
            newRoute.addDeparture(departure);
        }

        return newRoute;

    }
}
