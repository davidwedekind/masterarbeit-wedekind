import geopandas as gpd
import pandas as pd
import numpy as np
import psycopg2 as pg
from geoalchemy2 import Geometry, WKTElement
import logging
import json

logging.basicConfig(format='%(asctime)s - %(levelname)s: %(message)s', level=logging.DEBUG)

DATA_METADATA_KEYS = [
    'title',
    'description',
    'source_name',
    'source_url',
    'source_year',
    'source_download_date'
]

GEOM_DATA_TYPES = ['POINT', 'LINESTRING', 'POLYGON', 'MULTIPOLYGON', 'MULTILINESTRING']


def load_df_to_database(df, update_mode, db_parameter, schema, table_name, meta_data, geom_cols=None):
    if update_mode not in ['replace', 'append']:
        raise Exception("Update mode is: [" + update_mode + "] but can only be [replace] or [append]")

    df_import = df.copy()
    logging.info('Import data to database...')
    db_engine = "postgresql+psycopg2://" \
                f"{db_parameter['user']}:{db_parameter['password']}@{db_parameter['host']}:{db_parameter['port']}/{db_parameter['database']}"

    check_meta_data(meta_data)

    if isinstance(df_import, gpd.geodataframe.GeoDataFrame):
        logging.info('Found GeoDataFrame')

        logging.info('Checking for EPSG 25832')
        if not df_import.crs.srs == 'epsg:25832':
            raise Exception('Transform data to EPSG 25832')

        logging.info('Writing wkts of geometries')
        geom_data_types = {}
        for geom_column, dtype in geom_cols.items():
            df_import[geom_column] = df_import[geom_column].apply(lambda x: WKTElement(x.wkt, srid=25832))

            if not dtype in GEOM_DATA_TYPES:
                raise Exception(f'Wrong geom column dtype. Valid are {GEOM_DATA_TYPES}')

            else:
                geom_data_types = {geom_column: Geometry(dtype, srid=25832)}
        logging.info('Uploading to database')
        if big_df_size(df_import):
            import_data_chunks(df_import, update_mode, table_name, db_engine, schema, geom_data_types=geom_data_types)
        else:
            df_import.to_sql(table_name, db_engine, schema=schema, index=False, if_exists=update_mode, method='multi',
                             dtype=geom_data_types)

    else:
        logging.info('Uploading to database')
        if big_df_size(df_import):
            import_data_chunks(df_import, table_name, db_engine, schema, geom_data_types=None)
        else:
            df_import.to_sql(table_name, db_engine, schema=schema, index=False, if_exists=update_mode, method='multi')

    write_meta_data(db_parameter, meta_data, schema, table_name)
    logging.info('Table import done!')


def big_df_size(df):
    if df.shape[0] > 100000:
        return True
    return False


def import_data_chunks(df, update_mode, table_name, db_engine, schema, geom_data_types=None):
    logging.info('Table size too big --> Importing chunks')
    df = df.reset_index()
    chunks = np.array_split(list(df.index), 100)

    if isinstance(df, gpd.geodataframe.GeoDataFrame):
        for chunk in chunks:
            df.loc[chunk].to_sql(table_name, db_engine, schema=schema, index=False, if_exists=update_mode,
                                 method='multi', dtype=geom_data_types)
            update_mode = 'append'
    else:
        for chunk in chunks:
            df.loc[chunk].to_sql(table_name, db_engine, schema=schema, index=False, if_exists=update_mode,
                                 method='multi')
            update_mode = 'append'


def check_meta_data(meta_data):
    logging.info('Checking meta data inputs')
    for key in DATA_METADATA_KEYS:
        if not key in meta_data.keys():
            raise Exception(f'{key} -> Missing in your Metadata')
    for key, value in meta_data.items():
        if not (isinstance(value, str) and len(value) > 1):
            raise Exception(f'{key} -> Missing in your Metadata')


def write_meta_data(db_parameter, meta_data, schema, table_name):
    comment_sql = f"""
        COMMENT ON TABLE {schema}.{table_name} IS
        '
        ####################
        # Meta information #
        ####################
            title: {meta_data["title"]},
            description: {meta_data["description"]},
            source_name: {meta_data["source_name"]},
            source_url: {meta_data["source_url"]},
            source_year: {meta_data["source_year"]},
            source_download_date: {meta_data["source_download_date"]}'
        """
    logging.info(f'Table metadata: {comment_sql}')
    logging.info('Writing metadata to table comments')
    with pg.connect(**db_parameter) as con:
        cursor = con.cursor()
        cursor.execute(comment_sql)


def load_db_parameters(path):
    logging.info("Fetch db_parameter...")
    with open(path) as json_file:
        data = json.load(json_file)
        return data['param']


def drop_table_if_exists(db_parameter, table_name, table_schema):
    logging.info("Drop table if exists...")
    with pg.connect(**db_parameter) as con:
        sql = f'''DROP TABLE IF EXISTS {table_schema}.{table_name} CASCADE;'''
        cursor = con.cursor()
        cursor.execute(sql)
        con.commit()


def run_sql_script(SQL_FILE_PATH, db_parameter, param=None):
    logging.info("Run sql script: " + SQL_FILE_PATH)
    sql_file = open(SQL_FILE_PATH, 'r')
    sql = sql_file.read()

    if not (param is None):
        sql = sql.format(**param)

    logging.info("Execute sql query...")
    logging.info(sql)

    with pg.connect(**db_parameter) as con:
        cursor = con.cursor()
        cursor.execute(sql)
        con.commit()

    logging.info("Successfully finished running!")
