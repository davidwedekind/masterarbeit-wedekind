CREATE MATERIALIZED VIEW matsim_output."sim_ms_metropolregion" AS (

	SELECT
		run_name,
		calib_group area,
		c_main_mode sim_main_mode,
		COUNT(trip_id) as sim_wege_abs
	FROM matsim_output.trips_enriched
	GROUP BY run_name, area, sim_main_mode
	ORDER BY run_name, area, sim_main_mode

);