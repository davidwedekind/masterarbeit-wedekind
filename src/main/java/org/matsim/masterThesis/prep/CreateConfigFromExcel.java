package org.matsim.masterThesis.prep;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.masterThesis.run.StuttgartMasterThesisExperimentalConfigGroup;
import org.matsim.stuttgart.ptFares.PtFaresConfigGroup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.stream.IntStream;

public class CreateConfigFromExcel {

    public static void main(String[] args) throws IOException {
        final int columnNumber = 4;

        CreateConfigFromExcel.Input input = new CreateConfigFromExcel.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);

        // ---- Read-In Excel Data ----
        FileInputStream file = new FileInputStream(new File(input.excelFile));
        final Workbook workbook = new XSSFWorkbook(file);
        final Sheet sheet = workbook.getSheet("scenario_template");


        Config config = ConfigUtils.loadConfig(input.configTemplate);

        // RUN ID
        final int runIdRow = 3;
        final String runId = sheet.getRow(runIdRow - 1).getCell(columnNumber).getStringCellValue();
        config.controler().setRunId(runId);

        // NO ITERATIONS
        final int noIterationsRow = 4;
        config.controler().setLastIteration((int) sheet.getRow(noIterationsRow - 1).getCell(columnNumber).getNumericCellValue());

        // PLANS FILE
        final int plansFileRow = 5;
        config.plans().setInputFile(sheet.getRow(plansFileRow - 1).getCell(columnNumber).getStringCellValue());

        // BIKE TELEPORTED MODE SPEED
        final int bikeModeSpeedRow = 8;
        ModeRoutingParams bikeParams = config.plansCalcRoute().getModeRoutingParams().get(TransportMode.bike);
        bikeParams.setTeleportedModeSpeed(sheet.getRow(bikeModeSpeedRow - 1).getCell(columnNumber).getNumericCellValue());

        // COMPENSATION PER INTERMODAL TRIP
        final int compPerIntermodalTripRow = 9;
        IntermodalTripFareCompensatorsConfigGroup compensatorsCfg = ConfigUtils.addOrGetModule(config,
                IntermodalTripFareCompensatorsConfigGroup.class);

        IntermodalTripFareCompensatorConfigGroup compensatorCfg = new IntermodalTripFareCompensatorConfigGroup();
        compensatorCfg.setCompensationCondition(IntermodalTripFareCompensatorConfigGroup.CompensationCondition.PtModeUsedInSameTrip);
        compensatorCfg.setDrtModesAsString(TransportMode.bike);
        compensatorCfg.setPtModesAsString("pt");
        compensatorCfg.setCompensationMoneyPerTrip(sheet.getRow(compPerIntermodalTripRow - 1).getCell(columnNumber).getNumericCellValue());
        compensatorsCfg.addParameterSet(compensatorCfg);

        // CHAIN BASED MODES
        final int chainBasedModesRow = 10;
        String[] modes = sheet.getRow(compPerIntermodalTripRow - 1).getCell(columnNumber).getStringCellValue().split(", ");
        config.subtourModeChoice().setChainBasedModes(modes);

        // FARES
        final int fareZonesStartRow = 13;
        StuttgartMasterThesisExperimentalConfigGroup thesisExpConfigGroup = ConfigUtils.addOrGetModule(config,
                StuttgartMasterThesisExperimentalConfigGroup.class);
        thesisExpConfigGroup.setFareZoneShapeFile(sheet.getRow(fareZonesStartRow - 1).getCell(columnNumber).getStringCellValue());

        if (! sheet.getRow( fareZonesStartRow ).getCell(columnNumber).getStringCellValue().equals("-")){
            PtFaresConfigGroup configFares = ConfigUtils.addOrGetModule(config,
                    PtFaresConfigGroup.class);

            PtFaresConfigGroup.FaresGroup faresGroup = new PtFaresConfigGroup.FaresGroup();
            faresGroup.setOutOfZonePrice(10.);

            // ToDo: Map to Map<Integer, Double> und dann add fares
            IntStream.range(fareZonesStartRow, fareZonesStartRow + 8)
                    .mapToDouble(row -> sheet.getRow( row ).getCell(columnNumber).getNumericCellValue())
                    .forEach(value -> faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(1, value)));
            configFares.setFaresGroup(faresGroup);

            PtFaresConfigGroup.ZonesGroup zonesGroup = new PtFaresConfigGroup.ZonesGroup();
            zonesGroup.setOutOfZoneTag("out");
            zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("1", false));
            zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("1/2", true, Set.of("1", "2")));
            zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("2", false));
            zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("2/3", true, Set.of("2", "3")));
            zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("3", false));
            zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("3/4", true, Set.of("3", "4")));
            zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("4", false));
            zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("4/5", true, Set.of("4", "5")));
            zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("5", false));
            zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("5/6", true, Set.of("5", "6")));
            zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("6", false));
            zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("6/7", true, Set.of("6", "7")));
            zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("7", false));
            zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("7/8", true, Set.of("7", "8")));
            zonesGroup.addZone(new PtFaresConfigGroup.ZonesGroup.Zone("8", false));
            configFares.setZonesGroup(zonesGroup);

        }



    }

    private static class Input {

        @Parameter(names = "-excelFile")
        private String excelFile;

        @Parameter(names = "-configTemplate")
        private String configTemplate;

        @Parameter(names = "-output")
        private String output;

    }


}
