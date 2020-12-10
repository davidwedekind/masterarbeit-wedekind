import os
import pandas as pd
import geopandas as gpd
import click
import matsim
import logging
import h3
import psycopg2 as pg
from shapely import geometry
from shapely.geometry.multipolygon import MultiPolygon
from shapely.geometry.polygon import Polygon
from shapely.geometry.point import Point
from utils import load_df_to_database, load_db_parameters, drop_table_if_exists, run_sql_script
from sim_import import update_views

@click.group()
def cli():
    pass



@cli.command()
@click.option('--plans', type=str, default='', help='plans path')
@click.option('--db_parameter', type=str, default='', help='Directory of where db parameter are stored')
@click.pass_context
def import_home_loc(ctx, plans, db_parameter):
    """
    HOME LOCATIONS
    This is the function which can be executed for extracting the agents home locations from the plans file.

    ---------
    Execution:
    python ini_import import-home-loc
    --plans [plan file path]
    --db_parameter [path of db parameter json]
    ---------

    """

    # -- PRE-CALCULATIONS --
    logging.info("Read plans file...")
    plans = matsim.plan_reader(plans, selectedPlansOnly=True)

    home_activity_prefix = 'home'
    agents = list()

    logging.info("Extract relevant information...")
    for person, plan in plans:
        home_activity = next(
            e for e in plan if (e.tag == 'activity' and e.attrib['type'].startswith(home_activity_prefix)))
        agents.append({'person_id': person.attrib['id'], 'home_x': home_activity.attrib['x'],
                       'home_y': home_activity.attrib['y']})

    df_agents = pd.DataFrame(agents)
    gdf_agents = gpd.GeoDataFrame(df_agents.drop(columns=['home_x', 'home_y']),
                                  geometry=gpd.points_from_xy(df_agents.home_x, df_agents.home_y))
    gdf_agents = gdf_agents.set_crs(epsg=25832)

    # -- IMPORT --
    table_name = 'agent_home_locations'
    table_schema = 'matsim_input'
    db_parameter = load_db_parameters(db_parameter)
    drop_table_if_exists(db_parameter, table_name, table_schema)

    DATA_METADATA = {
        'title': 'Agent Home Locations',
        'description': 'Table of agents home locations',
        'source_name': 'Senozon Input',
        'source_url': 'Nan',
        'source_year': '2020',
        'source_download_date': 'Nan',
    }

    logging.info("Load data to database...")
    load_df_to_database(
        df=gdf_agents,
        update_mode='replace',
        db_parameter=db_parameter,
        schema=table_schema,
        table_name=table_name,
        meta_data=DATA_METADATA,
        geom_cols={'geometry': 'POINT'})

    logging.info("Home location import successful!")


@cli.command()
@click.option('--gem', type=str, default='', help='path to vg250 data [.shp]')
@click.option('--regiosta', type=str, default='', help='path to regioSta data [.xlsx]')
@click.option('--db_parameter', type=str, default='', help='path to db_parameter [.json]')
@click.pass_context
def import_gem(ctx, gem, regiosta, db_parameter):
    """
    COMMUNITY AREA WITH REGIOSTAR ASSIGNMENT
    This is the function which can be executed for uploading community data/ shapes with regiostar assignments to db

    ---------
    Execution:
    python ini_import import-gem
    --vg250 [shape file path]
    --db_parameter [path of db parameter json]
    ---------

    """

    # -- PRE-CALCULATIONS --
    # Step 1: vg250
    logging.info("Read-in community shape file...")
    gdf_gemeinden = gpd.read_file(gem)
    gdf_gemeinden['geometry'] = gdf_gemeinden.apply(lambda x:
                                                    x['geometry'] if x['geometry'].type == 'MultiPolygon' else
                                                    MultiPolygon([x['geometry']]),
                                                    axis=1)
    gdf_gemeinden = gdf_gemeinden.to_crs("epsg:25832")

    # Step 2: regiostar
    logging.info("Read-in regiostart data...")
    df_regiosta = pd.read_excel(regiosta, sheet_name='ReferenzGebietsstand2018')
    df_regiosta['gem'] = df_regiosta.apply(lambda x: str(x['gem']).zfill(8), axis=1)
    df_regiosta = df_regiosta[['gem', 'RegioStaR2', 'RegioStaR4', 'RegioStaR5', 'RegioStaR7']]
    df_regiosta.rename(columns={'gem': 'AGS'}, inplace=True)

    r2 = {1: 'Stadtregion',
          2: 'Ländliche Region'}

    r4 = {11: 'Metropolitane Stadtregion',
          12: 'Regiopolitane Stadtregion',
          21: 'Stadtregionsnahe ländliche Region',
          22: 'Periphere ländliche Region',
          }

    r5 = {51: 'Stadtregion - Metropole',
          52: 'Stadtregion - Regiopole und Großstadt',
          53: 'Stadtregion - Umland',
          54: 'Ländliche Region - Städte, städtischer Raum',
          55: 'Ländliche Region - Kleinstädtischer, dörflicher Raum',
          }

    r7 = {71: 'Stadtregion - Metropole',
          72: 'Stadtregion - Regiopole und Großstadt',
          73: 'Stadtregion - Mittelstadt, städtischer Raum',
          74: 'Stadtregion - Kleinstädtischer, dörflicher Raum',
          75: 'Ländliche Region - Zentrale Stadt ',
          76: 'Ländliche Region - Städtischer Raum',
          77: 'Ländliche Region - Kleinstädtischer, dörflicher Raum',
          }

    df_regiosta['RegioStaR2_bez'] = df_regiosta['RegioStaR2'].replace(r2)
    df_regiosta['RegioStaR4_bez'] = df_regiosta['RegioStaR4'].replace(r4)
    df_regiosta['RegioStaR5_bez'] = df_regiosta['RegioStaR5'].replace(r5)
    df_regiosta['RegioStaR7_bez'] = df_regiosta['RegioStaR7'].replace(r7)

    df_regiosta['RegioStaR2'] = df_regiosta['RegioStaR2'].astype('str')
    df_regiosta['RegioStaR4'] = df_regiosta['RegioStaR4'].astype('str')
    df_regiosta['RegioStaR5'] = df_regiosta['RegioStaR5'].astype('str')
    df_regiosta['RegioStaR7'] = df_regiosta['RegioStaR7'].astype('str')

    logging.info("Merge community and regiostar data...")
    gdf_gemeinden = gdf_gemeinden.merge(df_regiosta, how='left', on='AGS')
    gdf_gemeinden.columns = gdf_gemeinden.columns.map(str.lower)

    # -- IMPORT --
    table_name = 'gemeinden'
    table_schema = 'general'
    db_parameter = load_db_parameters(db_parameter)
    drop_table_if_exists(db_parameter, table_name, table_schema)

    DATA_METADATA = {
        'title': 'Gemeinden (VG250) mit ReioSta Zuordnung',
        'description': 'Verwaltungsgebiete 1:250000 (Ebenen), Stand 01.01.',
        'source_name': 'Bundesamt für Kartographie und Geodäsie',
        'source_url': 'https://gdz.bkg.bund.de/index.php/default/verwaltungsgebiete-1-250-000-ebenen-stand-01-01-vg250-ebenen-01-01.html',
        'source_year': '2020',
        'source_download_date': '2020-11-17',
    }

    logging.info("Load data to database...")
    load_df_to_database(
        df=gdf_gemeinden,
        update_mode='replace',
        db_parameter=db_parameter,
        schema=table_schema,
        table_name=table_name,
        meta_data=DATA_METADATA,
        geom_cols={'geometry': 'MULTIPOLYGON'})

    logging.info("Import of community data successful!")


@cli.command()
@click.option('--kreise', type=str, default='', help='path to vg250 data [.shp]')
@click.option('--db_parameter', type=str, default='', help='path to db_parameter [.json]')
@click.pass_context
def import_kreise(ctx, kreise, db_parameter):
    """
    COMMUNITY AREA WITH REGIOSTAR ASSIGNMENT
    This is the function which can be executed for uploading county data/ shapes to db

    ---------
    Execution:
    python ini_import import-kreise
    --vg250 [shape file path]
    --db_parameter [path of db parameter json]
    ---------

    """

    # -- PRE-CALCULATIONS --
    logging.info("Read-in county shape file...")
    gdf_kreise = gpd.read_file(kreise)
    gdf_kreise['geometry'] = gdf_kreise.apply(lambda x:
                                              x['geometry'] if x['geometry'].type == 'MultiPolygon' else
                                              MultiPolygon([x['geometry']]),
                                              axis=1)
    gdf_kreise = gdf_kreise.to_crs(epsg=25832)
    gdf_kreise.columns = gdf_kreise.columns.map(str.lower)

    # -- IMPORT --
    table_name = 'kreise'
    table_schema = 'general'
    db_parameter = load_db_parameters(db_parameter)
    drop_table_if_exists(db_parameter, table_name, table_schema)

    DATA_METADATA = {
        'title': 'Kreise (VG250)',
        'description': 'Verwaltungsgebiete 1:250000 (Ebenen), Stand 01.01.',
        'source_name': 'Bundesamt für Kartographie und Geodäsie',
        'source_url': 'https://gdz.bkg.bund.de/index.php/default/verwaltungsgebiete-1-250-000-ebenen-stand-01-01-vg250-ebenen-01-01.html',
        'source_year': '2020',
        'source_download_date': '2020-11-17',
    }

    logging.info("Load data to database...")
    load_df_to_database(
        df=gdf_kreise,
        update_mode='replace',
        db_parameter=db_parameter,
        schema=table_schema,
        table_name=table_name,
        meta_data=DATA_METADATA,
        geom_cols={'geometry': 'MULTIPOLYGON'})

    logging.info("Import of county data successful!")


@cli.command()
@click.option('--calib', type=str, default='', help='path to calib data [.xlsx]')
@click.option('--db_parameter', type=str, default='', help='path to db_parameter [.json]')
@click.pass_context
def import_calib(ctx, calib, db_parameter):
    """
    CALIBRATION DATA
    This is the function which can be executed for uploading community data/ shapes with regiostar assignments to db

    ---------
    Execution:
    python ini_import import-calib
    --calib [xlsx file path]
    --db_parameter [path of db parameter json]
    ---------

    """

    # -- PRE-CALCULATIONS --
    logging.info("Read-in excel file...")
    tables = dict()
    tables['calib_distanzklassen'] = pd.read_excel(calib, sheet_name='01_Distanzklassen_Tidy')
    tables['calib_wege'] = pd.read_excel(calib, sheet_name='02_Wege_Tidy')
    tables['calib_modal_split'] = pd.read_excel(calib, sheet_name='03_ModalSplit_Tidy')
    tables['calib_nutzersegmente'] = pd.read_excel(calib, sheet_name='04_Nutzersegmente', skipfooter=7)
    tables['calib_oev_segmente'] = pd.read_excel(calib, sheet_name='05_ÖVSegmente', skipfooter=8)

    for df in tables.values():
        df.columns = df.columns.map(str.lower)

    # -- META DATA --
    DATA_METADATA = {'calib_distanzklassen': {
        'title': 'Distanzklassen',
        'description': 'Tabelle A W12 Wegelänge - Stadt Stuttgart',
        'source_name': 'Tabellarische Grundausertung Stadt Stuttgart. MID 2017',
        'source_url': 'https://vm.baden-wuerttemberg.de/fileadmin/redaktion/m-mvi/intern/Dateien/PDF/MID2017_Stadt_Stuttgart.pdf',
        'source_year': '2018',
        'source_download_date': '2020-11-20'
    }, 'calib_wege': {
        'title': 'Wege',
        'description': 'Allgemeine Kennwerte und Verkehrsaufkommen nach regionalstatistischem Raumtyp (RegioSta R7)',
        'source_name': 'infas. MID 2017',
        'source_url': 'http://gecms.region-stuttgart.org/Download.aspx?id=104816',
        'source_year': '2019',
        'source_download_date': '2020-11-20'
    }, 'calib_modal_split': {
        'title': 'Modal Split',
        'description': 'Allgemeine Kennwerte und Verkehrsaufkommen nach regionalstatistischem Raumtyp (RegioSta R7)',
        'source_name': 'infas. MID 2017',
        'source_url': 'http://gecms.region-stuttgart.org/Download.aspx?id=104816',
        'source_year': '2019',
        'source_download_date': '2020-11-20'
    }, 'calib_nutzersegmente': {
        'title': 'Nutzersegmente',
        'description': 'Allgemeine Kennwerte und Verkehrsaufkommen nach regionalstatistischem Raumtyp (RegioSta R7)',
        'source_name': 'infas. MID 2017',
        'source_url': 'http://gecms.region-stuttgart.org/Download.aspx?id=104816',
        'source_year': '2019',
        'source_download_date': '2020-11-20'
    }, 'calib_oev_segmente': {
        'title': 'Fahrtenanteile je Verkehrsmittel',
        'description': 'Fahrtenanteile je Verkehrsmittel bis zum Ums',
        'source_name': 'VVS',
        'source_url': 'https://www.vvs.de/download/Zahlen-Daten-Fakten-2019.pdf',
        'source_year': '2020',
        'source_download_date': '2020-11-20'
    }
    }

    # -- IMPORT --
    db_parameter = load_db_parameters(db_parameter)

    for key in tables:
        table_schema = 'general'
        drop_table_if_exists(db_parameter, key, table_schema)
        logging.info("Load data to database: " + key)
        load_df_to_database(
            df=tables[key],
            update_mode='replace',
            db_parameter=db_parameter,
            schema=table_schema,
            table_name=key,
            meta_data=DATA_METADATA[key])

    logging.info("Import of calibration data successful!")


@cli.command()
@click.option('--sim_area', type=str, default='', help='sim area shape file')
@click.option('--reg_stuttgart', type=str, default='', help='sim area shape file')
@click.option('--vvs_area', type=str, default='', help='vvs area shape file')
@click.option('--db_parameter', type=str, default='', help='Directory of where db parameter are stored')
@click.pass_context
def import_areas(ctx, sim_area, reg_stuttgart, vvs_area, db_parameter):
    """
    SIMULATION AREA
    This is the function which can be executed for uploading the simulation area and region stuttgart shape file

    ---------
    Execution:
    python ini_import import-sim-area
    --sim_area [shp]
    --reg_stuttgart [shp]
    --db_parameter [path of db parameter json]
    ---------

    """

    # -- PRE-CALCULATIONS --
    logging.info("Read shape files...")
    gdf_sim_area = gpd.read_file(sim_area)
    gdf_sim_area['geometry'] = gdf_sim_area['geometry'].apply(lambda x: MultiPolygon([x]))

    gdf_reg_stuttgart = gpd.read_file(reg_stuttgart)
    gdf = gdf_sim_area.append(gdf_reg_stuttgart)

    gdf_vvs_area = gpd.read_file(vvs_area)
    gdf = gdf.append(gdf_vvs_area)

    gdf = gdf.set_crs(epsg=25832)

    # -- IMPORT --
    table_name = 'areas'
    table_schema = 'general'
    db_parameter = load_db_parameters(db_parameter)
    drop_table_if_exists(db_parameter, table_name, table_schema)

    DATA_METADATA = {
        'title': 'Areas',
        'description': 'Important areas',
        'source_name': 'Nan',
        'source_url': 'Nan',
        'source_year': 'Nan',
        'source_download_date': 'Nan',
    }

    logging.info("Load data to database...")
    load_df_to_database(
        df=gdf,
        update_mode='replace',
        db_parameter=db_parameter,
        schema=table_schema,
        table_name=table_name,
        meta_data=DATA_METADATA,
        geom_cols={'geometry': 'MULTIPOLYGON'})

    logging.info("Area import successful!")


@cli.command()
@click.option('--shape', type=str, default='', help='path to Germany shapefile [.shp]')
@click.option('--db_parameter', type=str, default='', help='path to db_parameter [.json]')
@click.pass_context
def create_h3_tables(ctx, shape, db_parameter):
    """
    H§ HEXAGONS (DIFFERENT LEVELS)
    This is the function which can be executed for creating tables of h3 hexagons spread over Germany

    ---------
    Execution:
    python create-h3-tables
    -- de_shape [path to Germany shapefile shp]
    -- db_parameter [path of db parameter json]
    ---------

    """

    # -- PRE-CALCULATIONS --
    logging.info("Create h3 hexagon tables...")
    resolutions = [6, 7, 8]

    gdf_de = gpd.read_file(filename=shape)
    gdf_de = gdf_de.set_crs(epsg=4326)
    gdf_de["geom_geojson"] = gdf_de["geometry"].apply(lambda x: geometry.mapping(x))
    db_parameter = load_db_parameters(db_parameter)

    for res in resolutions:
        logging.info("Create resolution " + str(res))
        gdf_h3 = generate_hexagon_df(gdf_shape=gdf_de, res=res)

        # -- IMPORT --
        table_name = 'h3_res_' + str(res)
        table_schema = 'general'
        drop_table_if_exists(db_parameter, table_name, table_schema)

        DATA_METADATA = {
            'title': 'H3 Hexagons - Level ' + str(res),
            'description': 'H3 Hexagons spread over Germany of level ' + str(res),
            'source_name': 'Nan',
            'source_url': 'Nan',
            'source_year': 'Nan',
            'source_download_date': 'Nan',
        }

        logging.info("Load data to database...")
        load_df_to_database(
            df=gdf_h3,
            update_mode='replace',
            db_parameter=db_parameter,
            schema=table_schema,
            table_name=table_name,
            meta_data=DATA_METADATA,
            geom_cols={'center': 'POINT', 'geometry': 'POLYGON'})


@cli.command()
@click.option('--db_parameter', type=str, default='', help='Directory of where db parameter are stored')
@click.pass_context
def create_mviews(ctx, db_parameter):
    sql_dir = os.path.abspath(os.path.join('__file__', "../../../sql"))
    logging.info('sql directory: ' + sql_dir)

    db_parameter = load_db_parameters(db_parameter)

    # -- 1st ORDER MVIEWS --
    logging.info('Start with agent home locations mview:')
    query = sql_dir + '/matsim_input/create_home_locations_mview.sql'
    run_sql_script(SQL_FILE_PATH=query, db_parameter=db_parameter)

    query = f'''
            REFRESH MATERIALIZED VIEW matsim_input."agents_homes_with_raumdata";
            '''
    with pg.connect(**db_parameter) as con:
        cursor = con.cursor()
        cursor.execute(query)
        con.commit()

    # -- 2nd ORDER MVIEWS (dependend on home locations) --
    logging.info('Create more mviews...')
    sql_dir = sql_dir + '/matsim_output'
    queries = os.listdir(sql_dir)
    queries = [sql_dir + '/' + query for query in queries]

    queries_2nd_order = [query for query in queries if not query.endswith('_3o.sql')]
    queries_3rd_order = [query for query in queries if query.endswith('_3o.sql')]

    for query in queries_2nd_order:
        run_sql_script(SQL_FILE_PATH=query, db_parameter=db_parameter)

    # -- 3rd ORDER MVIEWS (dependend on home locations and other 2nd order mviews) --
    for query in queries_3rd_order:
        run_sql_script(SQL_FILE_PATH=query, db_parameter=db_parameter)

    logging.info('Update all mviews...')
    update_views(db_parameter=db_parameter)


def generate_hexagon_df(gdf_shape, res):
    gdf = gdf_shape.copy()
    logging.info("Spread out hexagons over geometries in shape file...")
    gdf['h3_id'] = gdf['geom_geojson'].apply(lambda x: fill_hex(x, res))

    logging.info("Collect hexagons and create geodataframe...")
    gdf = pd.DataFrame(gdf['h3_id'].explode().dropna())

    gdf['geometry'] = gdf['h3_id'].apply(lambda x: Polygon(h3.h3_to_geo_boundary(h=x)))
    gdf['center'] = gdf['h3_id'].apply(lambda x: Point(h3.h3_to_geo(h=x)))
    gdf = gpd.GeoDataFrame(gdf.drop(columns=['geometry']),
                             geometry=gdf['geometry'])
    gdf = gdf.set_crs(epsg=4326)
    gdf = gdf.to_crs(epsg=25832)
    return gdf


def fill_hex(geom_geojson, res):
    set_hexagons = h3.polyfill(geojson=geom_geojson,
                               res=res,
                               geo_json_conformant=False)
    return list(set_hexagons)


if __name__ == '__main__':
    cli(obj={})
