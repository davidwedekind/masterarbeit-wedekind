package org.matsim.masterThesis.run;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author dwedekind
 */

public class StuttgartMasterThesisExperimentalConfigGroup extends ReflectiveConfigGroup {
    private static final String GROUP_NAME = "stuttgartMasterThesisExperimental";
    private static final String PARKING_ZONE_SHAPE_FILE = "parkingZoneShapeFile";
    private static final String FARE_ZONE_SHAPE_FILE = "fareZoneShapeFile";
    private static final String REDUCED_CAR_INFRASTRUCTURE_ZONES_SHAPE_FILE = "reducedCarInfrastructureZonesShapeFile";
    private static final String U6_EXTENSION_SHAPEFILE = "u6ExtensionShapeFile";
    private static final String S60_EXTENSION = "s60Extension";
    private static final String SUPERSHUTTLE_EXTENSION = "supershuttleExtension";
    private static final String FLUGHAFEN_CONNECTION_ALIGNMENT = "flughafenConnectionAlignment";

    private String parkingZoneShapeFile = "parkingZones_bc.shp";
    private String fareZoneShapeFile = "fareZones_bc.shp";
    private String reducedCarInfrastructureShapeFile = null;
    private String u6ExtensionShapeFile = null;
    private boolean s60Extension = false;
    private boolean supershuttleExtension = false;
    private boolean flughafenConnectionAlignment = false;


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

    @StringGetter(U6_EXTENSION_SHAPEFILE)
    public String getU6ExtensionShapeFile() {
        return u6ExtensionShapeFile;
    }

    @StringSetter(U6_EXTENSION_SHAPEFILE)
    public void setU6ExtensionShapeFile(String u6ExtensionShapeFile) {
        this.u6ExtensionShapeFile = u6ExtensionShapeFile;
    }

    @StringGetter(S60_EXTENSION)
    public String getS60ExtensionAsString() {
        return String.valueOf(getS60Extension());
    }

    public boolean getS60Extension() {
        return s60Extension;
    }

    @StringSetter(S60_EXTENSION)
    public void setS60Extension(String s60Extension) {
        setS60Extension(Boolean.parseBoolean(s60Extension));
    }

    public void setS60Extension(boolean s60Extension) {
        this.s60Extension = s60Extension;
    }

    @StringGetter(SUPERSHUTTLE_EXTENSION)
    public String getSupershuttleExtensionAsString() {
        return String.valueOf(getSupershuttleExtension());
    }

    public boolean getSupershuttleExtension() {
        return supershuttleExtension;
    }

    @StringSetter(SUPERSHUTTLE_EXTENSION)
    public void setSupershuttleExtension(String supershuttleExtension) {
        setSupershuttleExtension(Boolean.parseBoolean(supershuttleExtension));
    }

    public void setSupershuttleExtension(boolean supershuttleExtension) {
        this.supershuttleExtension = supershuttleExtension;
    }

    @StringGetter(FLUGHAFEN_CONNECTION_ALIGNMENT)
    public String getFlughafenConnectionAlignmentAsString() {
        return String.valueOf(getFlughafenConnectionAlignment());
    }

    public boolean getFlughafenConnectionAlignment() {
        return flughafenConnectionAlignment;
    }

    @StringSetter(FLUGHAFEN_CONNECTION_ALIGNMENT)
    public void setFlughafenConnectionAlignment(String flughafenConnectionAlignment) {
        setFlughafenConnectionAlignment(Boolean.parseBoolean(flughafenConnectionAlignment));
    }

    public void setFlughafenConnectionAlignment(boolean flughafenConnectionAlignment) {
        this.flughafenConnectionAlignment = flughafenConnectionAlignment;
    }

}
