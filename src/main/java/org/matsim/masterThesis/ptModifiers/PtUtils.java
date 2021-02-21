package org.matsim.masterThesis.ptModifiers;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.transitSchedule.api.*;
import playground.vsp.andreas.utils.pt.TransitScheduleCleaner;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author dwedekind
 */


public class PtUtils {
    private static final Logger log = Logger.getLogger(PtUtils.class);
    private final TransitSchedule tS;
    private final Network network;


    public PtUtils(Scenario scenario){
        this.tS = scenario.getTransitSchedule();
        this.network = scenario.getNetwork();

    }


    public void extendTransitLine(String lineIdAsString, List<TransitRouteStop> stopSequenceA, List<TransitRouteStop> stopSequenceB){
        TransitLine line = tS.getTransitLines().get(Id.create(lineIdAsString, TransitLine.class));
        List<TransitRoute> routesToAdd = new ArrayList<>();
        List<TransitRoute> routesToRemove = new ArrayList<>();

        List<Id<Link>> linkSequenceA = stopSequenceA.stream()
                .map(transitRouteStop -> transitRouteStop.getStopFacility().getLinkId())
                .collect(Collectors.toList());

        List<Id<Link>> linkSequenceB = stopSequenceB.stream()
                .map(transitRouteStop -> transitRouteStop.getStopFacility().getLinkId())
                .collect(Collectors.toList());

        for (TransitRoute oldRoute: line.getRoutes().values()){
            TransitRouteStop fstStop = oldRoute.getStops().get(0);
            TransitRouteStop lstStop = oldRoute.getStops().get(oldRoute.getStops().size() - 1);

            NetworkRoute newNetworkRoute;
            List<TransitRouteStop> newRouteStopSequence;
            List<Departure> departures;

            if (determineBaseStop(lstStop).equals(determineBaseStop(stopSequenceA.get(0)))) {
                // CASE 1 - Existing Route is prolonged in the end with stopSequenceA

                newNetworkRoute = extendNetworkRoute(
                        getAllLinkIds(oldRoute).subList(0, getAllLinkIds(oldRoute).size() - 1),
                        linkSequenceA
                );
                newRouteStopSequence = extendRouteStopList(
                        oldRoute.getStops().subList(0, oldRoute.getStops().size() - 1),
                        stopSequenceA
                );
                departures = copyDepartures(oldRoute.getDepartures(), 0.);


            } else if (determineBaseStop(lstStop).equals(determineBaseStop(stopSequenceB.get(0)))){
                // CASE 2 - Existing Route is prolonged in the end with stopSequenceB

                newNetworkRoute = extendNetworkRoute(
                        getAllLinkIds(oldRoute).subList(0, getAllLinkIds(oldRoute).size() - 1),
                        linkSequenceB
                );
                newRouteStopSequence = extendRouteStopList(
                        oldRoute.getStops().subList(0, oldRoute.getStops().size() - 1),
                        stopSequenceB
                );
                departures = copyDepartures(oldRoute.getDepartures(), 0.);


            } else if (determineBaseStop(fstStop).equals(determineBaseStop(stopSequenceA.get(stopSequenceA.size() - 1)))){
                // CASE 3 - Existing Route is prolonged in the beginning with stopSequenceA

                newNetworkRoute = extendNetworkRoute(
                        linkSequenceA,
                        getAllLinkIds(oldRoute).subList(1, getAllLinkIds(oldRoute).size())
                );
                newRouteStopSequence = extendRouteStopList(
                        stopSequenceA,
                        oldRoute.getStops().subList(1, oldRoute.getStops().size())
                );
                departures = copyDepartures(oldRoute.getDepartures(), - (stopSequenceA.get(stopSequenceA.size() - 1).getDepartureOffset().seconds()) );


            } else if (determineBaseStop(fstStop).equals(determineBaseStop(stopSequenceB.get(stopSequenceB.size() - 1)))) {
                // CASE 4 - Existing Route is prolonged in the beginning with stopSequenceB

                newNetworkRoute = extendNetworkRoute(
                        linkSequenceB,
                        getAllLinkIds(oldRoute).subList(1, getAllLinkIds(oldRoute).size())
                );
                newRouteStopSequence = extendRouteStopList(
                        stopSequenceB,
                        oldRoute.getStops().subList(1, oldRoute.getStops().size())
                );
                departures = copyDepartures(oldRoute.getDepartures(), - (stopSequenceA.get(stopSequenceB.size() - 1).getDepartureOffset().seconds()) );


            } else {
                continue;

            }

            TransitRoute newRoute = tS.getFactory().createTransitRoute(
                    oldRoute.getId(),
                    newNetworkRoute,
                    newRouteStopSequence,
                    oldRoute.getTransportMode());

            for (Departure departure: departures){
                newRoute.addDeparture(departure);

            }

            routesToRemove.add(oldRoute);
            routesToAdd.add(newRoute);

        }

        for (TransitRoute oldRoute: routesToRemove){
            line.removeRoute(oldRoute);
        }

        for (TransitRoute newRoute: routesToAdd){
            line.addRoute(newRoute);
        }

    }


    private List<Id<Link>> getAllLinkIds(TransitRoute route){
        List<Id<Link>> allLinkIds = new ArrayList<>();
        allLinkIds.add(route.getRoute().getStartLinkId());
        allLinkIds.addAll(route.getRoute().getLinkIds());
        allLinkIds.add(route.getRoute().getEndLinkId());
        return allLinkIds;

    }


    private List<Departure> copyDepartures(Map<Id<Departure>, Departure> oldDepartures, double offSet) {
        List<Departure> newDepartures = new ArrayList<>();

        for (Departure oldDeparture: oldDepartures.values()){
            if ((oldDeparture.getDepartureTime() + offSet) >= 0){
                Departure newDeparture = tS.getFactory().createDeparture(
                        oldDeparture.getId(),
                        oldDeparture.getDepartureTime() + offSet
                );
                newDeparture.setVehicleId(oldDeparture.getVehicleId());
                newDepartures.add(newDeparture);
            }
        }

        return newDepartures;

    }


    private String determineBaseStop(TransitRouteStop stop){
        String stopIdAsString = stop.getStopFacility().getId().toString();
        if (stopIdAsString.contains(".")){
            return stopIdAsString.split("\\.")[0];

        } else {
            return stopIdAsString;

        }

    }


    private NetworkRoute extendNetworkRoute(List<Id<Link>> linksPt1, List<Id<Link>> linksPt2) {
        List<Id<Link>> newRouteLinks = new ArrayList<>(linksPt1);
        newRouteLinks.addAll(linksPt2);
        return RouteUtils.createNetworkRoute(newRouteLinks);

    }


    private List<TransitRouteStop> extendRouteStopList(List<TransitRouteStop> transitRouteStopsPt1, List<TransitRouteStop> transitRouteStopsPt2){
        List<TransitRouteStop> newRouteStopSequence = new ArrayList<>(transitRouteStopsPt1);
        final double offSet = transitRouteStopsPt1.get(transitRouteStopsPt1.size() - 1).getDepartureOffset().seconds()
                + transitRouteStopsPt2.get(0).getArrivalOffset().seconds();
        

        List<TransitRouteStop> adjRouteStopsPt2 = transitRouteStopsPt2.stream()
                .map(transitRouteStop -> tS.getFactory().createTransitRouteStop(
                        transitRouteStop.getStopFacility(),
                        getV0OrV1(transitRouteStop.getArrivalOffset(), transitRouteStop.getDepartureOffset()) + offSet,
                        getV0OrV1(transitRouteStop.getDepartureOffset(), transitRouteStop.getArrivalOffset()) + offSet
                ))
                .collect(Collectors.toList());

        newRouteStopSequence.addAll(adjRouteStopsPt2);

        for (TransitRouteStop routeStop: newRouteStopSequence){
            routeStop.setAwaitDepartureTime(true);
        }

        return newRouteStopSequence;

    }


    private double getV0OrV1(OptionalTime v0, OptionalTime v1){
        if (v0.isDefined()){
            return v0.seconds();
        } else {
            return v1.seconds();
        }
    }


    public Link createLink(Id<Link> id, Node from, Node to, String ptMode) {
        Link link = network.getFactory().createLink(id, from, to);
        link.setAllowedModes(new HashSet<>(Collections.singletonList(ptMode)));
        link.setCapacity(999999);
        link.setLength(NetworkUtils.getEuclideanDistance(link.getFromNode().getCoord(), link.getToNode().getCoord()));
        link.setFreespeed(28.);
        return link;

    }


    public TransitStopFacility createStopFacility(Id<TransitStopFacility> id, Coord coord, Id<Link> linkId, String name, Id<TransitStopArea> stopAreaId){
        TransitStopFacility stopFacility = tS.getFactory().createTransitStopFacility(
                id,
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


    private TransitRoute copyAndAdjustRoute(TransitRoute oldRoute, List<String> stopsToCancel) {

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

}
