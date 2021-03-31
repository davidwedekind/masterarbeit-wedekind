import gzip
import os

import click
import pandas as pd
import geopandas as gpd
import logging
from analysis.utils import load_df_to_database, load_db_parameters
import xml.etree.ElementTree as ET
from shapely.geometry.linestring import LineString
import psycopg2 as pg

sql_dir = os.path.abspath(os.path.join('__file__', "../../../sql/calibration_views"))

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
    update_views(db_parameter, sql_dir)


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
    import_trips(trips, db_parameter, run_name)

    # -- LEGS IMPORT --
    legs = run_dir + "/" + run_name + ".output_legs.csv.gz"
    import_legs(legs, db_parameter, run_name)

    # -- CONFIG IMPORT --
    config_param = run_dir + "/" + run_name + ".output_config.xml"
    import_config_param(config_param, db_parameter, run_name)

    logging.info("All data successfully imported: " + run_name)
    logging.info("-------------------------------------------")
    logging.info("")


def import_trips(trips, db_parameter, run_name):
    """
    Trip import based off .output_trips.csv.gz
    """

    logging.info("Append to trips table: " + run_name)

    # -- PRE-CALCULATIONS --
    gdf_trips = parse_trips_file(trips)
    gdf_trips['run_name'] = run_name

    # -- IMPORT --
    table_name = 'sim_trips_raw'
    table_schema = 'matsim_output'

    DATA_METADATA = {
        'title': 'Trips',
        'description': 'Trip table',
        'source_name': 'Senozon Input',
        'source_url': 'Nan',
        'source_year': '2020',
        'source_download_date': 'Nan',
    }

    logging.info("Load data to database...")
    load_df_to_database(
        df=gdf_trips,
        update_mode='append',
        db_parameter=db_parameter,
        schema=table_schema,
        table_name=table_name,
        meta_data=DATA_METADATA,
        geom_cols={'geometry': 'LINESTRING'})

    logging.info("Trip table update successful!")


def parse_trips_file(trips):
    """
    Function for parsing trip file and manipulating trip data
    """

    # -- PARSING --
    types = {'person': str,
             'trip_number': int,
             'trip_id': str,
             'start_link': str,
             'end_link': str,
             'first_pt_boarding_stop': str,
             'last_pt_egress_stop': str
             }

    logging.info("Parse trips file...")
    try:
        with gzip.open(trips) as f:
            df_trips = pd.read_csv(f, sep=";", dtype=types)
    except OSError as e:
        raise Exception(e.strerror)

    # -- BUILDING GEOMETRIES --
    logging.info("Build trip geometries...")
    df_trips['geometry'] = df_trips.apply(
        lambda x: LineString([(x['start_x'], x['start_y']), (x['end_x'], x['end_y'])]), axis=1)
    gdf_trips = gpd.GeoDataFrame(df_trips.drop(columns=['geometry']), geometry=df_trips['geometry'])
    gdf_trips = gdf_trips.set_crs(epsg=25832)

    # -- FURTHER DATA MANIPULATIONS --
    logging.info("Further data manipulations...")
    gdf_trips.rename(columns={'main_mode': 'matsim_raw_main_mode'}, inplace=True)
    gdf_trips['matsim_cal_main_mode'] = gdf_trips.apply(lambda x: identify_calib_mode(x['matsim_raw_main_mode'], x['modes']), axis=1)
    gdf_trips['dep_time'] = gdf_trips['dep_time'].apply(convert_time)
    gdf_trips['trav_time'] = gdf_trips['trav_time'].apply(convert_time)
    gdf_trips['wait_time'] = gdf_trips['wait_time'].apply(convert_time)

    # Checked on 30/03/2021: arr_time = trav_time + dep_time (without wait time)
    gdf_trips['arr_time'] = gdf_trips['dep_time'] + gdf_trips['trav_time']
    gdf_trips['trip_speed'] = gdf_trips.apply(lambda x:
                                              calculate_speed(x['traveled_distance'], x['trav_time'] + x['wait_time']),
                                              axis=1
                                              )
    gdf_trips['beeline_speed'] = gdf_trips.apply(lambda x:
                                                 calculate_speed(x['euclidean_distance'],
                                                                 x['trav_time'] + x['wait_time']),
                                                 axis=1
                                                 )
    logging.info("Trip table manipulation finished...")
    return gdf_trips


def import_legs(legs, db_parameter, run_name):
    """
    Legs import based off .output_legs.csv.gz
    """

    logging.info("Append to legs table: " + run_name)

    # -- PRE-CALCULATIONS --
    gdf_legs = parse_legs_file(legs)
    gdf_legs['run_name'] = run_name

    # -- IMPORT --
    table_name = 'sim_legs_raw'
    table_schema = 'matsim_output'

    DATA_METADATA = {
        'title': 'Legs',
        'description': 'Leg table',
        'source_name': 'Senozon Input',
        'source_url': 'Nan',
        'source_year': '2020',
        'source_download_date': 'Nan',
    }

    gdf_legs.to_csv("C:/Users/david/Desktop/m5_test.csv")

    load_df_to_database(
        df=gdf_legs,
        update_mode='append',
        db_parameter=db_parameter,
        schema=table_schema,
        table_name=table_name,
        meta_data=DATA_METADATA,
        geom_cols={'geometry': 'LINESTRING'})


def parse_legs_file(legs):
    """
    Function for parsing legs file and manipulating legs data
    """

    # -- PARSING --
    types = {'person': str,
             'trip_id': str,
             'start_link': str,
             'end_link': str,
             'access_stop_id': str,
             'egress_stop_id': str
             }

    logging.info("Parse legs file...")
    try:
        with gzip.open(legs) as f:
            df_legs = pd.read_csv(f, sep=";", dtype=types)
    except OSError as e:
        raise Exception(e.strerror)

    # -- BUILDING GEOMETRIES --
    df_legs['geometry'] = df_legs.apply(
        lambda x: LineString([(x['start_x'], x['start_y']), (x['end_x'], x['end_y'])]), axis=1)
    gdf_legs = gpd.GeoDataFrame(df_legs.drop(columns=['geometry']), geometry=df_legs['geometry'])
    gdf_legs = gdf_legs.set_crs(epsg=25832)

    # -- FURTHER DATA MANIPULATIONS --
    logging.info("Further data manipulations...")
    gdf_legs['dep_time'] = gdf_legs['dep_time'].apply(convert_time)
    gdf_legs['trav_time'] = gdf_legs['trav_time'].apply(convert_time)
    gdf_legs['wait_time'] = gdf_legs['wait_time'].apply(convert_time)
    gdf_legs['arr_time'] = gdf_legs['dep_time'] + gdf_legs['trav_time'] + gdf_legs['wait_time']
    gdf_legs['leg_speed'] = gdf_legs.apply(lambda x:
                                           calculate_speed(x['distance'], x['trav_time'] + x['wait_time']),
                                           axis=1
                                           )
    gdf_legs['pt_line'] = gdf_legs['transit_line'].apply(get_pt_line)
    gdf_legs['pt_group'] = gdf_legs['pt_line'].apply(get_pt_group)

    logging.info("Legs table manipulation finished...")
    return gdf_legs


def update_views(db_parameter, sql_dir):
    """
    Function for executing sql scripts that create/ update trip output materialized views
    """

    logging.info('sql directory: ' + sql_dir)
    queries = os.listdir(sql_dir)
    queries.sort()

    logging.info("Update materialized views: " + str(len(queries)))

    for query in queries:
        query_name = query.split(sep="_", maxsplit=1)[1].replace(".sql", "")
        logging.info("Refresh view: " + query_name)
        query = f'''
                REFRESH MATERIALIZED VIEW {query_name} ;
                '''

        logging.info(query)

        with pg.connect(**db_parameter) as con:
            cursor = con.cursor()
            cursor.execute(query)
            con.commit()


def import_config_param(config_param, db_parameter, run_name):
    """
    Function importing config parameter namely score parameters for modes
    """

    logging.info("Append to mode param table: " + run_name)

    # -- PRE-CALCULATIONS --
    tree = ET.parse(config_param)
    root = tree.getroot()

    df_modeParameters = list()
    for modeParameters in root.findall("./module[@name='planCalcScore']/parameterset[@type='scoringParameters']/parameterset[@type='modeParams']"):
        row = dict()
        for node in modeParameters:
            row[node.attrib.get("name")] = node.attrib.get("value")
        df_modeParameters.append(row)
    df_modeParameters = pd.DataFrame(df_modeParameters)
    df_modeParameters['run_name'] = run_name

    # -- IMPORT --
    table_name = 'sim_mode_params'
    table_schema = 'matsim_input'

    DATA_METADATA = {
        'title': 'Mode Parameter',
        'description': 'Mode parameter table',
        'source_name': 'Own Input',
        'source_url': 'Nan',
        'source_year': '2020',
        'source_download_date': 'Nan',
    }

    logging.info("Load data to database...")
    load_df_to_database(
        df=df_modeParameters,
        update_mode='append',
        db_parameter=db_parameter,
        schema=table_schema,
        table_name=table_name,
        meta_data=DATA_METADATA)

    logging.info("Mode param import successful!")


def identify_calib_mode(m_main_mode, modes):
    if isinstance(m_main_mode, float):
        if 'pt' in modes:
            return 'pt'
        elif 'car' in modes:
            return 'car'
        elif 'ride' in modes:
            return 'ride'
        elif 'bike' in modes:
            return 'bike'
        else:
            return 'walk'

    else:
        if 'pt_with_bike_used' in m_main_mode:
            return 'pt'
        else:
            return m_main_mode


def convert_time(time_string):
    time = time_string.split(":")
    time = list(map(int, time))
    return time[0]*3600+time[1]*60+time[2]


def calculate_speed(distance, time):
    if time == 0:
        return 0
    else:
        return (distance/time)*3.6


def get_pt_line(transit_line):
    if isinstance(transit_line, float):
        return transit_line
    else:
        if transit_line.startswith('addedFrom'):
            return transit_line.split('_', 2)[1]
        else:
            return transit_line


def get_pt_group(pt_line):
    if isinstance(pt_line, float):
        return pt_line
    else:
        if pt_line.startswith('Bus'):
            return 'bus'
        elif pt_line.startswith('STB'):
            return 'stb'
        elif pt_line.startswith('SL '):
            return 'hyperloop'
        elif pt_line.startswith('S '):
            return 'sbahn'
        else:
            return 'dbregio'


if __name__ == '__main__':
    cli(obj={})
