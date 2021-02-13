WITH tmp_1 AS (
	SELECT
		*,
		CASE
			WHEN matsim_main_mode='car' THEN 'Miv'
			WHEN matsim_main_mode='ride' THEN 'Miv'
			WHEN matsim_main_mode='pt' THEN 'Oepnv'
			WHEN matsim_main_mode='bike' THEN 'Rad'
			WHEN matsim_main_mode='walk' THEN 'Fuß'
		END As mid_main_mode
	FROM matsim_output.sim_trip_stats_boeb_essl
),

tmp_2 AS (
	SELECT
		run_name,
		mid_main_mode,
		SUM(sim_trips_abs_scaled_100) sim_trips_abs_scaled_100,
		SUM(sim_mode_share) sim_mode_share
	FROM tmp_1
	GROUP BY run_name, mid_main_mode
	ORDER BY run_name, mid_main_mode
)

SELECT
	*,
	CASE
		WHEN mid_main_mode='Miv' THEN 0.863326
		WHEN mid_main_mode='Oepnv' THEN 0.117583
		WHEN mid_main_mode='Rad' THEN 0.019091
		WHEN mid_main_mode='Fuß' THEN 0.0
	END As bvwp_mode_share,
		CASE
		WHEN mid_main_mode='Miv' THEN 0.863326*(SUM(sim_trips_abs_scaled_100) OVER (partition by run_name))
		WHEN mid_main_mode='Oepnv' THEN 0.117583*(SUM(sim_trips_abs_scaled_100) OVER (partition by run_name))
		WHEN mid_main_mode='Rad' THEN 0.019091*(SUM(sim_trips_abs_scaled_100) OVER (partition by run_name))
		WHEN mid_main_mode='Fuß' THEN 0.0*(SUM(sim_trips_abs_scaled_100) OVER (partition by run_name))
	END As bvwp_trips_abs
FROM tmp_2