import os
import pandas as pd
import click
import logging
import xml.etree.ElementTree as ET

sql_dir = os.path.abspath(os.path.join('__file__', "../../../sql/calibration_views"))


@click.group()
def cli():
    pass


@cli.command()
@click.option('--excel', type=str, default='', help='excel path')
@click.option('--sheet', type=str, default='', help='sheet with data')
@click.option('--config_template', type=str, default='', help='path to config template')
@click.option('--bash_template', type=str, default='', help='path to bash template')
@click.option('--run_directory', type=str, default='', help='run directory')
@click.pass_context
def create_configs_and_bash_scripts(ctx, excel, sheet, config_template, bash_template, run_directory):
    """
    CREATE CONFIGS AND BASH SCRIPTS FROM EXCEL
    This is the function which can be executed for creating the run input consisting of config files and bash scripts.

    ---------
    Execution:
    python prepare_run_import create-configs-and-bash-scripts
    --excel [excel file path]
    --sheet [sheet name]
    --config_template [config template path]
    --bash_template [bash template]
    --run_directory [run directory]
    ---------

    """

    logging.info("Start creating run input...")

    tree = ET.parse(config_template)
    root = tree.getroot()

    params = pd.read_excel(excel, sheet_name=sheet)
    params = params.astype(str)

    for index, row in pd.read_excel(excel, sheet_name=sheet).iterrows():
        elements = root.findall(
            "./module[@name='planCalcScore']/parameterset[@type='scoringParameters']/parameterset[@type='modeParams']")
        for element in elements:
            if isinstance(element.find("./param[@value='car']"), ET.Element):
                element.find("./param[@name='constant']").set('value', str(row['car_constant']))
                element.find("./param[@name='marginalUtilityOfTraveling_util_hr']").set('value',
                                                                                        str(row['car_mUT_hr']))

            if isinstance(element.find("./param[@value='ride']"), ET.Element):
                element.find("./param[@name='constant']").set('value', str(row['ride_constant']))
                element.find("./param[@name='marginalUtilityOfTraveling_util_hr']").set('value',
                                                                                        str(row['ride_mUT_hr']))

            if isinstance(element.find("./param[@value='pt']"), ET.Element):
                element.find("./param[@name='constant']").set('value', str(row['pt_constant']))
                element.find("./param[@name='marginalUtilityOfTraveling_util_hr']").set('value', str(row['pt_mUT_hr']))

            if isinstance(element.find("./param[@value='bike']"), ET.Element):
                element.find("./param[@name='constant']").set('value', str(row['bike_constant']))
                element.find("./param[@name='marginalUtilityOfTraveling_util_hr']").set('value',
                                                                                        str(row['bike_mUT_hr']))

            if isinstance(element.find("./param[@value='walk']"), ET.Element):
                element.find("./param[@name='constant']").set('value', str(row['walk_constant']))
                element.find("./param[@name='marginalUtilityOfTraveling_util_hr']").set('value',
                                                                                        str(row['walk_mUT_hr']))

        run_no = str(index + 1).zfill(2)

        run_name = "cal_" + sheet.replace("batch_", "") + "_" + run_no
        config_file_name = config_template.rsplit("/", 1)[1].replace("xxx", run_name)
        tree.write(run_directory + "/" + config_file_name)

        bash = open(bash_template)
        script = bash.readlines()
        script[2] = script[2].replace("xxx", run_name)
        bash_file_name = bash_template.rsplit("/", 1)[1].replace("xxx", run_name)
        bash = open(run_directory + "/" + bash_file_name, "w")
        bash.writelines(script)
        bash.close()


if __name__ == '__main__':
    cli(obj={})
