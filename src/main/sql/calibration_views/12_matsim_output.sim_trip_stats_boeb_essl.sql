SELECT
	run_name,
	matsim_cal_main_mode matsim_main_mode,
	COUNT(trip_id) as sim_trips_abs,
	COUNT(trip_id)*{**sfactor**} sim_trips_abs_scaled_100,
	(COUNT(trip_id) / SUM(COUNT(trip_id)) OVER (partition by run_name)) AS sim_mode_share
FROM matsim_output.sim_trips_on_boeb_essl_rel
GROUP BY run_name, matsim_cal_main_mode
ORDER BY run_name, matsim_cal_main_mode