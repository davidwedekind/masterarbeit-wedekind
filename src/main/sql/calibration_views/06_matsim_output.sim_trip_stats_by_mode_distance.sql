SELECT	
	run_name,
	area,
	distance_group,
	distance_group_no,
	matsim_cal_main_mode matsim_main_mode,
	COUNT(trip_id) sim_trips_abs,
	COUNT(trip_id)*{**sfactor**} sim_trips_abs_scaled_100,
	(COUNT(trip_id) / SUM(COUNT(trip_id)) OVER (partition by run_name, area, matsim_cal_main_mode)) AS sim_mode_share_gb_mode,
	(COUNT(trip_id) / SUM(COUNT(trip_id)) OVER (partition by run_name, area, distance_group)) AS sim_mode_share_gb_dgroup
FROM matsim_output.sim_trips_enriched
GROUP BY run_name, area, distance_group, distance_group_no, matsim_cal_main_mode
ORDER BY run_name, area, distance_group, distance_group_no, matsim_cal_main_mode