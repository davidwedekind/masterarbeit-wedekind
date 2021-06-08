import gzip
import os
import click
import logging
import pandas as pd
import geopandas as gpd
from analysis.utils import load_db_parameters,load_df_to_database
import xml.etree.ElementTree as ET
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
    """
    for run_dir in dir_contents:
        import_run(parent_dir + "/output-" + run_dir, db_parameter)
    """


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
    
    # -- PERSON2PTLINKLIST --
    person_2_pt_link_list = run_dir + "/" + run_name + ".output_person2PtLinkList.csv.gz"
    import_person_2_pt_link_list(person_2_pt_link_list, db_parameter, run_name)

    # -- PARKINGS --
    parkings = run_dir + "/" + run_name + ".output_parkings.csv.gz"
    import_parkings(parkings, db_parameter, run_name)

    # -- PERSON2FARES --
    person_2_fares = run_dir + "/" + run_name + ".output_person2Fare.csv.gz"
    import_person_2_fares(person_2_fares, db_parameter, run_name)

    # -- PTCOMPARATOR --
    pt_comparator_results = run_dir + "/" + run_name + ".output_ptComparatorResults.csv.gz"
    import_pt_comparator_results(pt_comparator_results, db_parameter, run_name)

    # TEMP WORKAROUND !!!!!
    # BECAUSE NETWORK2SHAPEWRITER IS NOT WORKING ON MATH CLUSTER
    # -- NETWORK --
    network = "C:/Users/david/Desktop/tmp/network-shp/" + run_name + ".output_network.shp"
    import_network(network, db_parameter, run_name)

    # -- STOPFACILITIES --
    stop_facilities = run_dir + "/" + run_name + ".output_transitSchedule.xml.gz"
    import_stop_facilities(stop_facilities, db_parameter, run_name)

    logging.info("All data successfully imported: " + run_name)
    logging.info("-------------------------------------------")
    logging.info("")


def import_pt_comparator_results(pt_comparator_results, db_parameter, run_name):
    """
    Person2LinkList Import based off .output_person2PtLinkList.csv.gz
    """

    logging.info("Append to pt_comparator_results table: " + run_name)

    # -- PRE-CALCULATIONS --
    df_pt_comparator_results = parse_comparator_results(pt_comparator_results)
    df_pt_comparator_results['run_name'] = run_name

    # -- IMPORT --
    table_name = 'pt_comparator_results'
    table_schema = 'matsim_output'

    DATA_METADATA = {
        'title': 'PTComparatorResults',
        'description': 'PTComparatorResults',
        'source_name': 'Senozon Input',
        'source_url': 'Nan',
        'source_year': '2020',
        'source_download_date': 'Nan',
    }

    logging.info("Load data to database...")
    load_df_to_database(
        df=df_pt_comparator_results,
        update_mode='append',
        db_parameter=db_parameter,
        schema=table_schema,
        table_name=table_name,
        meta_data=DATA_METADATA)

    logging.info("Comparator table update successful!")


def parse_comparator_results(pt_comparator_results):
    """
    Function for comparator results list parsing
    """

    # -- PARSING --
    types = {'person_id': str,
             'trip_id': str,
             'routing_mode': str,
             'start_time': float,
             'end_time': float,
             'trav_time': float,
             'containsS60': bool,
             'time_on_s60': str,
             'is_bike_and_ride': bool,
             'is_walk': bool,
             'route_description': str,
             }

    logging.info("Parse comparator results file...")
    try:
        with gzip.open(pt_comparator_results) as f:
            df_pt_comparator_results = pd.read_csv(f, sep=";", dtype=types)
    except OSError as e:
        raise Exception(e.strerror)

    return df_pt_comparator_results[list(types.keys())]


def import_person_2_pt_link_list(person_2_pt_link_list, db_parameter, run_name):
    """
    Person2LinkList Import based off .output_person2PtLinkList.csv.gz
    """

    logging.info("Append to person2LinkList table: " + run_name)

    # -- PRE-CALCULATIONS --
    df_person_2_link_list = parse_link_list(person_2_pt_link_list)
    df_person_2_link_list['run_name'] = run_name

    # -- IMPORT --
    table_name = 'person_2_link_list'
    table_schema = 'matsim_output'

    DATA_METADATA = {
        'title': 'Person2LinkList',
        'description': 'Person2LinkList',
        'source_name': 'Senozon Input',
        'source_url': 'Nan',
        'source_year': '2020',
        'source_download_date': 'Nan',
    }

    logging.info("Load data to database...")
    load_df_to_database(
        df=df_person_2_link_list,
        update_mode='append',
        db_parameter=db_parameter,
        schema=table_schema,
        table_name=table_name,
        meta_data=DATA_METADATA)

    logging.info("Person 2 link list table update successful!")


def parse_link_list(person_2_pt_link_list):
    """
    Function for person 2 pt link list parsing
    """

    # -- PARSING --
    types = {'id': int,
             'linkId': str,
             'vehicleId': str,
             'personId': str,
             'time': str,
             'timeBin': str,
             'mode': str,
             'ptSubmode': str,
             'ptLine': str,
             'ptRoute': str,
             'ptDep': str,
             }

    logging.info("Parse person 2 pt link list file...")
    try:
        with gzip.open(person_2_pt_link_list) as f:
            df_person_2_pt_link_list = pd.read_csv(f, sep=";", dtype=types)
    except OSError as e:
        raise Exception(e.strerror)

    return df_person_2_pt_link_list[list(types.keys())]


def import_parkings(parkings, db_parameter, run_name):
    """
    Parkings Import based off .output_parkings.csv.gz
    """

    logging.info("Append to parkings table: " + run_name)

    # -- PRE-CALCULATIONS --
    df_parkings = parse_parkings(parkings)
    df_parkings['run_name'] = run_name

    # -- IMPORT --
    table_name = 'parkings'
    table_schema = 'matsim_output'

    DATA_METADATA = {
        'title': 'Parkings',
        'description': 'Parkings',
        'source_name': 'Senozon Input',
        'source_url': 'Nan',
        'source_year': '2020',
        'source_download_date': 'Nan',
    }

    logging.info("Load data to database...")
    load_df_to_database(
        df=df_parkings,
        update_mode='append',
        db_parameter=db_parameter,
        schema=table_schema,
        table_name=table_name,
        meta_data=DATA_METADATA)

    logging.info("Parkings table update successful!")


def parse_parkings(parkings):
    """
    Function for parkings parsing
    """

    # -- PARSING --
    types = {'parkingId': int,
             'vehicleId': str,
             'personId': str,
             'linkId': str,
             'activityType': str,
             'parkingType': str,
             'firstTimeResidentialParking': str,
             'overnight': str,
             'ruleViolation': str,
             'parkingStartTime': int,
             'parkingEndTime': int,
             'parkingDuration': int,
             'parkingFee': float,
             }

    logging.info("Parse person 2 pt link list file...")
    try:
        with gzip.open(parkings) as f:
            df_parkings = pd.read_csv(f, sep=";", dtype=types)
    except OSError as e:
        raise Exception(e.strerror)

    return df_parkings[list(types.keys())]


def import_person_2_fares(person_2_fares, db_parameter, run_name):
    """
    Person2Fares Import based off .output_person2Fare.csv.gz
    """

    logging.info("Append to person 2 fares table: " + run_name)

    # -- PRE-CALCULATIONS --
    df_person_2_fares = parse_person_2_fares(person_2_fares)
    df_person_2_fares['run_name'] = run_name

    # -- IMPORT --
    table_name = 'person_2_fares'
    table_schema = 'matsim_output'

    DATA_METADATA = {
        'title': 'Person2Fares',
        'description': 'Person2Fares',
        'source_name': 'Senozon Input',
        'source_url': 'Nan',
        'source_year': '2020',
        'source_download_date': 'Nan',
    }

    logging.info("Load data to database...")
    load_df_to_database(
        df=df_person_2_fares,
        update_mode='append',
        db_parameter=db_parameter,
        schema=table_schema,
        table_name=table_name,
        meta_data=DATA_METADATA)

    logging.info("Person2Fares table update successful!")


def parse_person_2_fares(person_2_fares):
    """
    Function for person 2 fares parsing
    """

    # -- PARSING --
    types = {'personId': str,
             'outOfZones': str,
             'noZones': float,
             'fareAmount': float
             }

    logging.info("Parse person 2 fares list file...")
    try:
        with gzip.open(person_2_fares) as f:
            df_person_2_fares = pd.read_csv(f, sep=";", dtype=types)
    except OSError as e:
        raise Exception(e.strerror)

    return df_person_2_fares[list(types.keys())]


def import_network(network, db_parameter, run_name):
    """
    Network Import based off .network.shp
    """

    # -- PRE-CALCULATIONS --
    logging.info("Read-in network shape file...")
    gdf_network = gpd.read_file(network)
    gdf_network['run_name'] = run_name

    # -- IMPORT --
    table_name = 'network'
    table_schema = 'matsim_output'

    DATA_METADATA = {
        'title': 'Network',
        'description': 'Network',
        'source_name': 'Senozon Input',
        'source_url': 'Nan',
        'source_year': '2020',
        'source_download_date': 'Nan',
    }

    logging.info("Load data to database...")
    load_df_to_database(
        df=gdf_network,
        update_mode='append',
        db_parameter=db_parameter,
        schema=table_schema,
        table_name=table_name,
        meta_data=DATA_METADATA,
        geom_cols={'geometry': 'LINESTRING'})

    logging.info("Network table update successful!")


def import_stop_facilities(transit_schedule, db_parameter, run_name):
    """
    Transit Stop Facilities Import based off .transitSchedule.xml.gz
    """

    # -- PRE-CALCULATIONS --
    logging.info("Read-in transit schedule file...")
    df_stops = parse_schedule_file(transit_schedule)
    gdf_stops = gpd.GeoDataFrame(df_stops, geometry=gpd.points_from_xy(df_stops.x, df_stops.y))
    gdf_stops['run_name'] = run_name
    gdf_stops.set_crs(crs="epsg:25832", inplace=True)

    # -- IMPORT --
    table_name = 'stop_facilities'
    table_schema = 'matsim_output'

    DATA_METADATA = {
        'title': 'Transit Stop Facilities',
        'description': 'Transit Stop Facilities',
        'source_name': 'Senozon Input',
        'source_url': 'Nan',
        'source_year': '2020',
        'source_download_date': 'Nan',
    }

    logging.info("Load data to database...")
    load_df_to_database(
        df=gdf_stops,
        update_mode='append',
        db_parameter=db_parameter,
        schema=table_schema,
        table_name=table_name,
        meta_data=DATA_METADATA,
        geom_cols={'geometry': 'POINT'})

    logging.info("Transit Stop Facilities table update successful!")


def parse_schedule_file(transit_schedule):
    transitStops = list()

    try:
        with gzip.open(transit_schedule) as f:
            tree = ET.parse(f)
            root = tree.getroot()

            for tSF in root.find('./transitStops'):
                transitStops.append({
                    "id": tSF.attrib['id'],
                    "x": tSF.attrib['x'],
                    "y": tSF.attrib['y'],
                    "link_ref_id": tSF.attrib['linkRefId'],
                    "name": tSF.attrib['name'],
                    "is_blocking": tSF.attrib['isBlocking'],
                    "vvs_bike_ride": tSF.find('./attributes/attribute[@name="VVSBikeAndRide"]').text
                })

    except OSError as e:
        raise Exception(e.strerror)

    return pd.DataFrame(transitStops)


if __name__ == '__main__':
    cli(obj={})
