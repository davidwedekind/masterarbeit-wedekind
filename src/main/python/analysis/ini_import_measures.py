import os
import click
import logging
import analysis.ini_import

sql_dir = os.path.abspath(os.path.join('__file__', "../../../sql/measures_views"))

@click.group()
def cli():
    pass


@cli.command()
@click.option('--plans', type=str, default='', help='plans path')
@click.option('--db_parameter', type=str, default='', help='Directory of where db parameter are stored')
@click.pass_context
def import_agents(ctx, plans, db_parameter):
    analysis.ini_import.import_agents(ctx, plans, db_parameter)


@cli.command()
@click.option('--gem', type=str, default='', help='path to vg250 data [.shp]')
@click.option('--regiosta', type=str, default='', help='path to regioSta data [.xlsx]')
@click.option('--db_parameter', type=str, default='', help='path to db_parameter [.json]')
@click.pass_context
def import_gem(ctx, gem, regiosta, db_parameter):
    analysis.ini_import.import_gem(ctx, gem, regiosta, db_parameter)


@cli.command()
@click.option('--kreise', type=str, default='', help='path to vg250 data [.shp]')
@click.option('--db_parameter', type=str, default='', help='path to db_parameter [.json]')
@click.pass_context
def import_kreise(ctx, kreise, db_parameter):
    analysis.ini_import.import_kreise(ctx, kreise, db_parameter)


@cli.command()
@click.option('--sim_area', type=str, default='', help='sim area shape file')
@click.option('--reg_stuttgart', type=str, default='', help='sim area shape file')
@click.option('--vvs_area', type=str, default='', help='vvs area shape file')
@click.option('--db_parameter', type=str, default='', help='Directory of where db parameter are stored')
@click.pass_context
def import_areas(ctx, sim_area, reg_stuttgart, vvs_area, db_parameter):
    analysis.ini_import.import_areas(ctx, sim_area, reg_stuttgart, vvs_area, db_parameter)


@cli.command()
@click.option('--shape', type=str, default='', help='path to Germany shapefile [.shp]')
@click.option('--db_parameter', type=str, default='', help='path to db_parameter [.json]')
@click.pass_context
def create_h3_tables(ctx, shape, db_parameter):
    analysis.ini_import.create_h3_tables(ctx, shape, db_parameter)


@cli.command()
@click.option('--sfactor', type=str, default='', help='Scaling factor in percent')
@click.option('--db_parameter', type=str, default='', help='Directory of where db parameter are stored')
@click.pass_context
def create_mviews(ctx, sfactor, db_parameter):
    logging.info('scaling factor: ' + sfactor)
    logging.info('sql directory: ' + sql_dir)
    analysis.ini_import.create_mviews_func(sfactor, sql_dir, db_parameter)


@cli.command()
@click.option('--db_parameter', type=str, default='', help='Directory of where db parameter are stored')
@click.pass_context
def remove_mviews(ctx, db_parameter):
    logging.info('sql directory: ' + sql_dir)
    analysis.ini_import.remove_mviews_func(sql_dir, db_parameter)


if __name__ == '__main__':
    cli(obj={})
