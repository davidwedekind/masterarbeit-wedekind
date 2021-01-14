package org.matsim.masterThesis.ptModifiers;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.transitSchedule.api.*;
import playground.vsp.andreas.utils.pt.TransitScheduleCleaner;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author dwedekind
 */


public class PtUtils {
    private final TransitSchedule tS;
    private final Network network;


    public PtUtils(Scenario scenario){
        this.tS = scenario.getTransitSchedule();
        this.network = scenario.getNetwork();

    }


    public NetworkRoute extendNetworkRoute(List<Id<Link>> routeLinks, List<Id<Link>> linksToAdd, boolean atTheEnd) {
        List<Id<Link>> newRouteLinks = new ArrayList<>();

        if (atTheEnd) {
            newRouteLinks.addAll(routeLinks);
            newRouteLinks.addAll(linksToAdd);

        } else {
            List<Id<Link>> adjRouteLinks = new ArrayList<>(routeLinks);
            adjRouteLinks.remove(0);
            newRouteLinks.addAll(linksToAdd);
            newRouteLinks.addAll(adjRouteLinks);

        }

        return RouteUtils.createNetworkRoute(newRouteLinks);

    }


    public List<TransitRouteStop> extendRouteStopList(List<TransitRouteStop> transitRouteStops, List<TransitRouteStop> transitRouteStopsToAdd, boolean atTheEnd){
        final double offSet;
        List<TransitRouteStop> newTransitRouteStops = new ArrayList<>();

        if (atTheEnd) {
            offSet = transitRouteStops.get(transitRouteStops.size() - 1).getArrivalOffset().seconds();
        } else {
            offSet = 0.;
        }

        List<TransitRouteStop> newTransitRouteStopsToAdd = transitRouteStopsToAdd.stream()
                .map(transitRouteStop -> tS.getFactory().createTransitRouteStop(
                        transitRouteStop.getStopFacility(),
                        transitRouteStop.getArrivalOffset().seconds() + offSet,
                        transitRouteStop.getDepartureOffset().seconds() + offSet
                ))
                .collect(Collectors.toList());

        if (atTheEnd) {
            newTransitRouteStops.addAll(transitRouteStops);
            newTransitRouteStops.addAll(newTransitRouteStopsToAdd);
        } else {
            List<TransitRouteStop> adjTransitRouteStop = new ArrayList<>(transitRouteStops);
            adjTransitRouteStop.remove(0);
            newTransitRouteStops.addAll(newTransitRouteStopsToAdd);
            newTransitRouteStops.addAll(adjTransitRouteStop);

        }

        return newTransitRouteStops;
    }

    public Link createLink(String id, Node from, Node to) {
        Link link = network.getFactory().createLink(Id.createLinkId(id), from, to);
        link.setAllowedModes(new HashSet<>(Collections.singletonList(TransportMode.pt)));
        link.setCapacity(999999);
        link.setLength(NetworkUtils.getEuclideanDistance(link.getFromNode().getCoord(), link.getToNode().getCoord()));
        link.setFreespeed(28.);
        return link;

    }

    public TransitStopFacility createStopFacility(String id, Coord coord, Id<Link> linkId, String name, Id<TransitStopArea> stopAreaId){
        TransitStopFacility stopFacility = tS.getFactory().createTransitStopFacility(Id.create(id,
                TransitStopFacility.class),
                coord,
                false);
        stopFacility.setLinkId(linkId);
        stopFacility.setName(name);
        stopFacility.setStopAreaId(stopAreaId);
        return stopFacility;

    }

    public void removeLine(String transitLineName){
        TransitLine line = tS.getTransitLines().get(Id.create(transitLineName, TransitLine.class));
        tS.removeTransitLine(line);

    }

    public void shortenLine(String transitLineName, List<String> stopsToCancel){
        TransitLine line = tS.getTransitLines().get(Id.create(transitLineName, TransitLine.class));

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


    public TransitRoute copyAndAdjustRoute(TransitRoute oldRoute, List<String> stopsToCancel) {

        // Remove stops2Cancel from Route only -> more sophisticated would be also to adjust also network route, stops etc...
        // Filter stops: Do not keep keep stops to be canceled
        List<TransitRouteStop> stops2Keep = oldRoute.getStops().stream()
                .filter(transitRouteStop -> {
                    TransitStopFacility stopFacility = transitRouteStop.getStopFacility();
                    return ! stopsToCancel.contains(stopFacility.getId().toString());
                })
                .collect(Collectors.toList());

        TransitRoute newRoute = tS.getFactory().createTransitRoute(
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
        TransitLine line = tS.getTransitLines().get(Id.create(transitLineName, TransitLine.class));
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


            TransitRoute newRoute = tS.getFactory().createTransitRoute(
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
