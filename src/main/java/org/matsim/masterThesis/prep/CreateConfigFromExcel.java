package org.matsim.masterThesis.prep;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.masterThesis.run.StuttgartMasterThesisExperimentalConfigGroup;
import org.matsim.stuttgart.ptFares.PtFaresConfigGroup;
import java.io.*;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author dwedekind
 */

public class CreateConfigFromExcel {
    final Logger log = Logger.getLogger(CreateConfigFromExcel.class);
    private Sheet sheet = null;

    public CreateConfigFromExcel(String excelPath, String sheetName)  {

        try{
            FileInputStream excelFile = new FileInputStream(new File(excelPath));
            final Workbook workbook = new XSSFWorkbook(excelFile);
            this.sheet = workbook.getSheet(sheetName);

        }catch(Exception e) {
            log.error(e);

        }

    }


    public static void main(String[] args) {
        final Logger log = Logger.getLogger(CreateConfigFromExcel.class);

        CreateConfigFromExcel.Input input = new CreateConfigFromExcel.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);

        log.info("Get creation settings from: " + input.excelFile);
        log.info("Look at worksheet: " + input.sheetName);

        CreateConfigFromExcel creator = new CreateConfigFromExcel(input.excelFile, input.sheetName);

        Set<Integer> columnNumbers = CollectionUtils.stringToSet(input.columnNumbers).stream()
                .map(Integer::parseInt)
                .map(integer -> integer - 1)
                .collect(Collectors.toSet());

        // for each column defined write config and bash
        for (Integer columnNumber: columnNumbers){
            String configFileInput = creator.getConfigFileInput(columnNumber);
            log.info("Work on setting of column number: " + String.valueOf(columnNumber + 1));
            log.info("Config input file: " + configFileInput);

            Config config = ConfigUtils.loadConfig(configFileInput);

            creator.modifyConfigByValuesFromExcelColumn(config, columnNumber);
            String sampleSize = creator.getSampleSize(columnNumber);
            String runId = config.controler().getRunId();
            log.info("Run Id: " + configFileInput);

            String configOutputPathString = input.outputDir + "/stuttgart-v1.0-" + sampleSize + ".config_" + runId + ".xml";
            new ConfigWriter(config).write(configOutputPathString);

            String bashFileInput = creator.getBashFileInput(columnNumber);
            log.info("Bash input file: " + bashFileInput);

            if (! bashFileInput.equals("-")){
                File bashScript = new File(bashFileInput);
                try {
                    String bashContents = FileUtils.readFileToString(bashScript);
                    bashContents = bashContents.replace("XXX", runId);
                    String bashOutputPathString = input.outputDir + "/stuttgart-v1.0-" + sampleSize + "_" + runId + ".sh";
                    File bashOutputFile = new File(bashOutputPathString);
                    FileUtils.writeStringToFile(bashOutputFile, bashContents);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            log.info("Done.....");


        }


    }

    private String getSampleSize(int columnNumber){
        final int sampleSizeRow = 8;
        return sheet.getRow(sampleSizeRow - 1).getCell(columnNumber).getStringCellValue();

    }

    private String getConfigFileInput(int columnNumber){
        final int configFileRow = 6;
        return sheet.getRow(configFileRow - 1).getCell(columnNumber).getStringCellValue();
    }

    private String getBashFileInput(int columnNumber){
        final int bashFileRow = 7;
        return sheet.getRow(bashFileRow - 1).getCell(columnNumber).getStringCellValue();
    }


    private void modifyConfigByValuesFromExcelColumn(Config config, int columnNumber){
        log.info("Start modifying config for scenario of column number: " + columnNumber + " ...");

        // RUN ID
        final int runIdRow = 3;
        final String runId = sheet.getRow(runIdRow - 1).getCell(columnNumber).getStringCellValue();
        log.info("Set RunId to: " + runId);
        config.controler().setRunId(runId);

        // NO ITERATIONS
        final int noIterationsRow = 4;
        final int noIterations = (int) sheet.getRow(noIterationsRow - 1).getCell(columnNumber).getNumericCellValue();
        log.info("Set number of iterations to: " + noIterations);
        config.controler().setLastIteration(noIterations);

        // PLANS FILE
        final int plansFileRow = 5;
        final String plansFile = sheet.getRow(plansFileRow - 1).getCell(columnNumber).getStringCellValue();
        log.info("Set plans file input to: " + plansFile);
        config.plans().setInputFile(plansFile);

        // BIKE TELEPORTED MODE SPEED
        final int bikeModeSpeedRow = 11;
        final double bikeModeSpeed = sheet.getRow(bikeModeSpeedRow - 1).getCell(columnNumber).getNumericCellValue();
        log.info("Set bike teleported mode speed to: " + plansFile);
        ModeRoutingParams bikeParams = config.plansCalcRoute().getModeRoutingParams().get(TransportMode.bike);
        bikeParams.setTeleportedModeSpeed(bikeModeSpeed);

        // COMPENSATION PER INTERMODAL TRIP
        final int compPerIntermodalTripRow = 12;
        final double compPerIntermodalTrip = sheet.getRow(compPerIntermodalTripRow - 1).getCell(columnNumber).getNumericCellValue();
        log.info("Set compensation per intermodal trip to: " + compPerIntermodalTrip);

        IntermodalTripFareCompensatorsConfigGroup compensatorsCfg = ConfigUtils.addOrGetModule(config,
                IntermodalTripFareCompensatorsConfigGroup.class);

        IntermodalTripFareCompensatorConfigGroup compensatorCfg = new IntermodalTripFareCompensatorConfigGroup();
        compensatorCfg.setCompensationCondition(IntermodalTripFareCompensatorConfigGroup.CompensationCondition.PtModeUsedInSameTrip);
        compensatorCfg.setDrtModesAsString(TransportMode.bike);
        compensatorCfg.setPtModesAsString("pt");
        compensatorCfg.setCompensationMoneyPerTrip(compPerIntermodalTrip);
        compensatorsCfg.addParameterSet(compensatorCfg);

        // CHAIN BASED MODES
        final int chainBasedModesRow = 13;
        final String chainBasedModes = sheet.getRow(chainBasedModesRow - 1).getCell(columnNumber).getStringCellValue();
        log.info("Set compensation chain bases modes to: " + chainBasedModes);
        String[] modes = chainBasedModes.split(", ");
        config.subtourModeChoice().setChainBasedModes(modes);

        // FARES
        final int fareZonesStartRow = 16;
        StuttgartMasterThesisExperimentalConfigGroup thesisExpConfigGroup = ConfigUtils.addOrGetModule(config,
                StuttgartMasterThesisExperimentalConfigGroup.class);
        final String fareZoneShapeFile = sheet.getRow(fareZonesStartRow - 1).getCell(columnNumber).getStringCellValue();
        log.info("Set fare zone shape file path to: " + fareZoneShapeFile);
        thesisExpConfigGroup.setFareZoneShapeFile(fareZoneShapeFile);

        DataFormatter dataFormatter = new DataFormatter();

        if (! dataFormatter.formatCellValue(sheet.getRow( fareZonesStartRow ).getCell(columnNumber)).equals("-")){
            PtFaresConfigGroup configFares = ConfigUtils.addOrGetModule(config,
                    PtFaresConfigGroup.class);

            PtFaresConfigGroup.FaresGroup faresGroup = new PtFaresConfigGroup.FaresGroup();
            faresGroup.setOutOfZonePrice(8.);

            for (int i = 0; i < 8; i++){
                double value = sheet.getRow( fareZonesStartRow + i ).getCell(columnNumber).getNumericCellValue();
                log.info("Create fare with [zones, fare]: [" + (i + 1) + ", " + value + "]");
                faresGroup.addFare(new PtFaresConfigGroup.FaresGroup.Fare(i + 1, value));

            }

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

        } else {
            log.info("No extra fare values specified!");

        }

        // PARKING
        final int parkingShapeFileRow = 27;
        final String parkingShapeFile = sheet.getRow(parkingShapeFileRow - 1).getCell(columnNumber).getStringCellValue();
        log.info("Set parking shape file path to: " + parkingShapeFile);
        thesisExpConfigGroup.setParkingZoneShapeFile(parkingShapeFile);

        // REDUCED STREET NETWORK
        final int reducedStreetNetworkShpFileRow = 30;
        final String reducedStreetNetworkShpFile = sheet.getRow(reducedStreetNetworkShpFileRow - 1).getCell(columnNumber).getStringCellValue();

        if (! reducedStreetNetworkShpFile.equals("-")) {
            log.info("Set reduced street network shape file path to: " + reducedStreetNetworkShpFile);
            thesisExpConfigGroup.setReducedCarInfrastructureShapeFile(reducedStreetNetworkShpFile);

        } else {
            log.info("No reduced street network shape file specified!");

        }

        // PT EXTENSION
        final int ptExtensionStartRow = 33;
        final boolean s60Extension = sheet.getRow(ptExtensionStartRow - 1).getCell(columnNumber).getBooleanCellValue();
        log.info("Tag s60 extension: " + s60Extension);
        thesisExpConfigGroup.setS60Extension(s60Extension);

        final boolean supershuttleExtension = sheet.getRow(ptExtensionStartRow).getCell(columnNumber).getBooleanCellValue();
        log.info("Tag super shuttle extension: " + supershuttleExtension);
        thesisExpConfigGroup.setSupershuttleExtension(supershuttleExtension);

        final boolean taktAlignment = sheet.getRow(ptExtensionStartRow + 1).getCell(columnNumber).getBooleanCellValue();
        log.info("Tag takt alignment extension: " + taktAlignment);
        thesisExpConfigGroup.setTaktAlignment(taktAlignment);

        final boolean connectionImprovement = sheet.getRow(ptExtensionStartRow + 2).getCell(columnNumber).getBooleanCellValue();
        log.info("Tag connection improvement extension: " + connectionImprovement);
        thesisExpConfigGroup.setConnectionImprovement(connectionImprovement);


    }


    private static class Input {
        @Parameter(names = "-excelFile")
        private String excelFile;

        @Parameter(names = "-sheetName")
        private String sheetName;

        @Parameter(names = "-columnNumbers")
        private String columnNumbers;

        @Parameter(names = "-outputDir")
        private String outputDir;

    }


}
