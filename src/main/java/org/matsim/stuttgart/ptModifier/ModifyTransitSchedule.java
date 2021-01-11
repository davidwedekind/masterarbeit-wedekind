package org.matsim.stuttgart.ptModifier;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.transitSchedule.api.*;
import playground.vsp.andreas.utils.pt.TransitLineRemover;
import playground.vsp.andreas.utils.pt.TransitScheduleCleaner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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


    private TransitRoute copyAndAdjustRoute(TransitRoute oldRoute, List<String> stopsToCancel) {

        // Remove stops2Cancel from Route only -> more sophisticated would be also to adjust also network route, stops etc...
        // Filter stops: Do not keep keep stops to be canceled
        List<TransitRouteStop> stops2Keep = oldRoute.getStops().stream()
                .filter(transitRouteStop -> {
                    TransitStopFacility stopFacility = transitRouteStop.getStopFacility();
                    return ! stopsToCancel.contains(stopFacility.getId().toString());
                })
                .collect(Collectors.toList());

        TransitRoute newRoute = scenario.getTransitSchedule().getFactory().createTransitRoute(
                oldRoute.getId(),
                oldRoute.getRoute(),
                stops2Keep,
                oldRoute.getTransportMode());

        for (Departure departure: oldRoute.getDepartures().values()){
            newRoute.addDeparture(departure);
        }

        return newRoute;

    }


    public void extendLine(String transitLineName, List<Id<TransitStopFacility>> stops2Add, Map<Id<TransitStopFacility>, Double> departureOffsets, Map<Id<TransitStopFacility>, Double> arrivalOffsets) {
        TransitLine line = transitSchedule.getTransitLines().get(Id.create(transitLineName, TransitLine.class));
        // network extension

        TransitScheduleCleaner cleaner = new TransitScheduleCleaner();

        //transitSchedule.getFactory().createTransitStopFacility();



        // transit schedule extension
        for (TransitRoute oldRoute : line.getRoutes().values()) {
            List<TransitRouteStop> newStops;

            if (oldRoute.getStops().get(oldRoute.getStops().size() - 1).getStopFacility().getId().equals(stops2Add.get(0))) {
                // Extension at the end of oldRoute


                newStops = oldRoute.getStops();




                for (Id<TransitStopFacility> stopFacilityId: stops2Add){
                    //network.addNode(network.getFactory().createNode());
                    //network.addLink(network.getFactory().createLink());
                }

            } else if (oldRoute.getStops().get(0).getStopFacility().getId().equals(stops2Add.get(stops2Add.size() - 1))) {
                // Extension at the beginning of oldRoute

                newStops = oldRoute.getStops();

            } else {
                // Route does not connect with extension
                break;

            }


            TransitRoute newRoute = scenario.getTransitSchedule().getFactory().createTransitRoute(
                    oldRoute.getId(),
                    oldRoute.getRoute(),
                    newStops,
                    oldRoute.getTransportMode());

            line.removeRoute(oldRoute);
            line.addRoute(newRoute);

        }
    }

/*        var network = NetworkUtils.createNetwork();
        var inputNetwork = input.inputNetwork;
        var outputNetwork = NetworkUtils.createNetwork().getLinks().values().stream()
                .map(link -> {
                    var newLink = network.getFactory().createLink(link.getId(), link.getFromNode(), link.getToNode());
                    newLink.setFreespeed(link.getFreespeed());
                    return newLink;
                })
                .collect(NetworkUtils.getCollector());*/
}
