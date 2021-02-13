import os
import click
import logging
from analysis.utils import load_db_parameters

import analysis.sim_import

sql_dir = os.path.abspath(os.path.join('__file__', "../../../sql/measures_views"))

@click.group()
def cli():
    pass


@cli.command()
@click.option('--parent_dir', type=str, default='', help='parent directory to run directory [dir]')
@click.option('--db_parameter', type=str, default='', help='path to db_parameter [.json]')
@click.option('--str_filter', type=str, default=None, help='string filter')
@click.pass_context
def import_run_data(ctx, parent_dir, db_parameter, str_filter):
    """
    This is the function which can be executed for importing all relevant data
    of the output of all runs (all runs starting with a specific filter str)
    >> For each run selected:
    >> Import trips output
    >> Import legs output
    >> Import mode config settings
    >> Update materialized analyzer views

    ---------
    Execution:
    python sim_import update-run-tables
    --parent_dir [parent to output directory]
    --filter [string filter]
    --db_parameter [path of db parameter json]
    ---------

    """

    logging.info('Search for folders in: ' + parent_dir)
    if str_filter is not None:
        logging.info('Filter - Folders start with string: ' + str_filter)

    db_parameter = load_db_parameters(db_parameter)

    dir_contents = os.listdir(parent_dir)
    dir_contents = [run_dir.replace("output-", "") for run_dir in dir_contents]
    if str_filter is not None:
        dir_contents = [run_dir for run_dir in dir_contents if run_dir.startswith(str_filter)]

    # -- DATA IMPORTS --
    logging.info('The following run(s) will be imported: ' + '[' + ", ".join(dir_contents) + ']')

    for run_dir in dir_contents:
        import_run(parent_dir + "/output-" + run_dir, db_parameter)

    # -- VIEW UPDATES --
    analysis.sim_import.update_views(db_parameter, sql_dir)


def import_run(run_dir, db_parameter):
    """
    This is the function which can be executed for importing all relevant data
    of one run output to a postgres database.
    """

    # -- PRE-CALCULATIONS --
    run_name = run_dir.rsplit("/", 1)[1].replace("output-", "")
    logging.info("Start importing run: " + run_name)

    # -- TRIPS IMPORT --
    trips = run_dir + "/" + run_name + ".output_trips.csv.gz"
    analysis.sim_import.import_trips(trips, db_parameter, run_name)

    # -- LEGS IMPORT --
    legs = run_dir + "/" + run_name + ".output_legs.csv.gz"
    analysis.sim_import.import_legs(legs, db_parameter, run_name)

    logging.info("All data successfully imported: " + run_name)
    logging.info("-------------------------------------------")
    logging.info("")


if __name__ == '__main__':
    cli(obj={})
