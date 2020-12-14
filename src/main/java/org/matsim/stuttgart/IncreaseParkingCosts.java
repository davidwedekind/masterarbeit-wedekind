package org.matsim.stuttgart;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.stuttgart.prepare.AddAdditionalNetworkAttributes;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;
import java.util.Map;

/**
 * @author dwedekind, gleich
 */

public class IncreaseParkingCosts {
    private static final Logger log = Logger.getLogger(IncreaseParkingCosts.class );

    public static void main(String[] args) {
        IncreaseParkingCosts.Input input = new IncreaseParkingCosts.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Input network file: " + input.networkFile);

        // Read-in network
        IncreaseParkingCosts networkModifier = new IncreaseParkingCosts();
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Network network = scenario.getNetwork();
        new MatsimNetworkReader(network).readFile(input.networkFile);

        // Do manipulations
        ZoneModifier modifier = new ZoneModifier();
        networkModifier.run(scenario, modifier);

        // Write network output
        new NetworkWriter(scenario.getNetwork()).write(input.outputFile);
    }


    private void run(Scenario scenario, ZoneModifier modifier) {

        Network network = scenario.getNetwork();

        log.info("Start modifying network attributes...");
        modifyNetworkAttributes(network, modifier);
        log.info("Attributes modified successfully!");
    }


    private void modifyNetworkAttributes(Network network, ZoneModifier modifier) {

        for (var link : network.getLinks().values()){

        }
    }


    public static class ZoneModifier {
        private Map<String, ModifierParamGroup> zoneGroupString2ModifierParams;

        public void setModifierParams(ModifierParamGroup paramGroup) {
            zoneGroupString2ModifierParams.put(paramGroup.getZoneGroup(), paramGroup);
        }

        public Map<String, ModifierParamGroup> getAllModifierParams() {
            return zoneGroupString2ModifierParams;
        }

        public ModifierParamGroup getModifierParams(String zoneGroup) {
            return zoneGroupString2ModifierParams.get(zoneGroup);
        }

        public static class ModifierParamGroup {
            private String zoneGroup;
            private double oneHourPCostScalingFactor;
            private double extraHourPCostScalingFactor;
            private double maxDailyPCostScalingFactor;
            private double maxPTimeScalingFactor;
            private double pFineScalingFactor;
            private double resPCostsScalingFactor;

            ModifierParamGroup(String zoneGroup){
                this.zoneGroup = zoneGroup;
            }
        }

    }




    private static class Input {

        @Parameter(names = "-networkFile")
        private String networkFile;

        @Parameter(names = "-output")
        private String outputFile;

    }
}
