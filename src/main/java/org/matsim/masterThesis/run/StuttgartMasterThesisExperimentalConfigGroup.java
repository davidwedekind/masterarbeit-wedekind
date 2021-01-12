package org.matsim.masterThesis.run;

import graphql.AssertException;
import org.apache.commons.lang3.EnumUtils;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author dwedekind
 */

public class StuttgartMasterThesisExperimentalConfigGroup extends ReflectiveConfigGroup {
    private static final String GROUP_NAME = "stuttgartMasterThesisExperimental";

    private static final String PARKING_ZONE_SHAPE_FILE = "parkingZoneShapeFile";
    private static final String FARE_ZONE_SHAPE_FILE = "fareZoneShapeFile";
    private static final String REDUCED_CAR_INFRASTRUCTURE_ZONES_SHAPE_FILE = "reducedCarInfrastructureZonesShapeFile";
    private static final String REDUCED_CAR_INFRASTRUCTURE = "reducedCarInfrastructure";
    private static final String REDUCED_CAR_INFRASTRUCTURE_FREESPEED_THRESHOLD = "reducedCarInfrastructureFreespeedThreshold";
    private static final String REDUCED_CAR_INFRASTRUCTURE_CAPACITY_THRESHOLD = "reducedCarInfrastructureCapacityThreshold";
    private static final String PT_NETWORK_EXTENSIONS = "ptNetworkExtensions";


    private String parkingZoneShapeFile = "parkingZones.shp";
    private String fareZoneShapeFile = "fareZones.shp";
    private String reducedCarInfrastructureShapeFile = "reducedCarInfrastructureZones.shp";
    private boolean reducedCarInfrastructure = false;
    private double reducedCarInfrastructureFreespeedThreshold = 10.;
    private double reducedCarInfrastructureCapacityThreshold = 650.;
    private final Set<PtExtension> ptNetworkExtensions = new HashSet<>();

    public StuttgartMasterThesisExperimentalConfigGroup(){
        super(GROUP_NAME);
    }

    @StringGetter(PARKING_ZONE_SHAPE_FILE)
    public String getParkingZoneShapeFile() {
        return parkingZoneShapeFile;
    }

    @StringSetter(PARKING_ZONE_SHAPE_FILE)
    public void setParkingZoneShapeFile(String parkingZoneShapeFile) {
        this.parkingZoneShapeFile = parkingZoneShapeFile;
    }

    @StringGetter(FARE_ZONE_SHAPE_FILE)
    public String getFareZoneShapeFile() {
        return fareZoneShapeFile;
    }

    @StringSetter(FARE_ZONE_SHAPE_FILE)
    public void setFareZoneShapeFile(String fareZoneShapeFile) {
        this.fareZoneShapeFile = fareZoneShapeFile;
    }

    @StringGetter(REDUCED_CAR_INFRASTRUCTURE_ZONES_SHAPE_FILE)
    public String getReducedCarInfrastructureShapeFile() {
        return reducedCarInfrastructureShapeFile;
    }

    @StringSetter(REDUCED_CAR_INFRASTRUCTURE_ZONES_SHAPE_FILE)
    public void setReducedCarInfrastructureShapeFile(String reducedCarInfrastructureShapeFile) {
        this.reducedCarInfrastructureShapeFile = reducedCarInfrastructureShapeFile;
    }

    @StringGetter(REDUCED_CAR_INFRASTRUCTURE)
    public boolean getReducedCarInfrastructure() {
        return reducedCarInfrastructure;
    }

    @StringSetter(REDUCED_CAR_INFRASTRUCTURE)
    public void setReducedCarInfrastructure(boolean reducedCarInfrastructure) {
        this.reducedCarInfrastructure = reducedCarInfrastructure;
    }

    @StringGetter(REDUCED_CAR_INFRASTRUCTURE_FREESPEED_THRESHOLD)
    public double getReducedCarInfrastructureFreespeedThreshold() {
        return reducedCarInfrastructureFreespeedThreshold;
    }

    @StringSetter(REDUCED_CAR_INFRASTRUCTURE_FREESPEED_THRESHOLD)
    public void setReducedCarInfrastructureFreespeedThreshold(double reducedCarInfrastructureFreespeedThreshold) {
        this.reducedCarInfrastructureFreespeedThreshold = reducedCarInfrastructureFreespeedThreshold;
    }

    @StringGetter(REDUCED_CAR_INFRASTRUCTURE_CAPACITY_THRESHOLD)
    public double getReducedCarInfrastructureCapacityThreshold() {
        return reducedCarInfrastructureCapacityThreshold;
    }

    @StringSetter(REDUCED_CAR_INFRASTRUCTURE_CAPACITY_THRESHOLD)
    public void setReducedCarInfrastructureCapacityThreshold(double reducedCarInfrastructureCapacityThreshold) {
        this.reducedCarInfrastructureCapacityThreshold = reducedCarInfrastructureCapacityThreshold;
    }

    @StringGetter(PT_NETWORK_EXTENSIONS)
    public String getDeterministicPtNetworkExtensions() {
        return this.ptNetworkExtensions.stream().map(Enum::toString).collect(Collectors.joining(","));
    }

    public Set<PtExtension> getPtNetworkExtensions(){
        return this.ptNetworkExtensions;
    }

    @StringSetter(PT_NETWORK_EXTENSIONS)
    public void setPtNetworkExtensions(String ptNetworkExtensions) {
        Set<String> ptNetworkExtensionsSet = CollectionUtils.stringToSet(ptNetworkExtensions);
        Set<PtExtension> newPtNetworkExtensionsSet = ptNetworkExtensionsSet.stream()
                .map(extension -> {
                    if (EnumUtils.isValidEnum(PtExtension.class, extension)) {
                        return PtExtension.valueOf(extension);
                    } else {
                        throw new AssertException("There is no pt network extension with name: " + extension);
                    }
                })
                .collect(Collectors.toSet());
    }

    public void setPtNetworkExtensions(Set<PtExtension> ptNetworkExtensions){
        this.ptNetworkExtensions.clear();
        this.ptNetworkExtensions.addAll(ptNetworkExtensions);
    }


    enum PtExtension {
        U5,
        U6,
        S60
    }



}
