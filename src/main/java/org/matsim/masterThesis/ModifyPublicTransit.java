package org.matsim.masterThesis;

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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author dwedekind
 */

public class ModifyPublicTransit {
    private final Scenario scenario;
    private final TransitSchedule tS;
    private final Network network;


    public ModifyPublicTransit(Scenario scenario){
        this.scenario = scenario;
        this.tS = scenario.getTransitSchedule();
        this.network = scenario.getNetwork();

    }

    public void createS60Extension(){

        // Create two links (Rohrer Kurve)
        Node stopGoldberg = network.getNodes().get(Id.createNodeId("tr8005201"));
        Node stopLeinfelden = network.getNodes().get(Id.createNodeId("tr8003622"));

        Link linkTrNew0001 = createLink("trNew0001", stopGoldberg, stopLeinfelden);
        Link linkTrNew0002 = createLink("trNew0002", stopLeinfelden, stopGoldberg);

        // Create additional stop facilities
        TransitStopFacility stopFacilityTr8003622_1 = tS.getFacilities().get(Id.create("tr8003622.1", TransitStopFacility.class));
        TransitStopFacility stopFacilityTr8003622_2 = createStopFacility("tr8003622.2", stopFacilityTr8003622_1.getCoord(), linkTrNew0001.getId(), stopFacilityTr8003622_1.getName(), stopFacilityTr8003622_1.getStopAreaId());

        TransitStopFacility stopFacilityTr8005201_0 = tS.getFacilities().get(Id.create("tr8005201", TransitStopFacility.class));
        TransitStopFacility stopFacilityTr8005201_2 = createStopFacility("tr8005201.2", stopFacilityTr8005201_0.getCoord(), linkTrNew0001.getId(), stopFacilityTr8005201_0.getName(), stopFacilityTr8005201_0.getStopAreaId());

        // Extend S60
        // Prolong all Böblingen Arrivals and let all Böblingen departures already start at Filderstadt

        // Links
        List<Id<Link>> linksToAddDirFilderstadt = (Arrays.asList("tr672168", "trNew0001", "tr673031", "tr673032", "tr673033")).stream()
                .map((Function<String, Id<Link>>) Id::createLinkId).collect(Collectors.toList());
        List<Id<Link>> linksToAddDirBoeblingen = (Arrays.asList("tr672997", "tr672998", "tr672999", "tr673000", "trNew0002", "tr672157")).stream()
                .map((Function<String, Id<Link>>) Id::createLinkId).collect(Collectors.toList());

        // Transit Route Stops
        List<TransitRouteStop> transitRouteStopsDirB = Arrays.asList(
                tS.getFactory().createTransitRouteStop()
        )

        TransitLine lineS60 = tS.getTransitLines().get("S 60 - 1");
        for (TransitRoute oldRoute: lineS60.getRoutes().values()){


            TransitRoute newRoute = tS.getFactory().createTransitRoute(
                    oldRoute.getId(),
                    extendNetworkRoute(oldRoute.getRoute().getLinkIds(), linksToAddDirBoeblingen, true),

            )
        }
    }

    private NetworkRoute extendNetworkRoute(List<Id<Link>> routeLinks, List<Id<Link>> linksToAddDirBoeblingen, boolean atTheEnd) {
        if (atTheEnd){
            routeLinks.addAll(linksToAddDirBoeblingen);
            return RouteUtils.createNetworkRoute(routeLinks);

        }else{
            linksToAddDirBoeblingen.addAll(routeLinks);
            return RouteUtils.createNetworkRoute(linksToAddDirBoeblingen);

        }
    }

    private


    public void createU5Extension(){

    }

    public void createU6Extension(){

    }

    private Link createLink(String id, Node from, Node to) {
        Link link = network.getFactory().createLink(Id.createLinkId(id), from, to);
        link.setAllowedModes(new HashSet<>(Collections.singletonList(TransportMode.pt)));
        link.setCapacity(999999);
        link.setLength(NetworkUtils.getEuclideanDistance(link.getFromNode().getCoord(), link.getToNode().getCoord()));
        link.setFreespeed(28.);
        return link;

    }

    private TransitStopFacility createStopFacility(String id, Coord coord, Id<Link> linkId, String name, Id<TransitStopArea> stopAreaId){
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
