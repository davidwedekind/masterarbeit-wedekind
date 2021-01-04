SELECT
	cal.run_name,
	cal.area,
	cal.matsim_main_mode,
	cal.distance_group_no,
	cal.distance_group,
	COALESCE(sim.sim_mode_share_gb_dgroup,0) sim_mode_share_gb_dgroup,
	cal.mid_mode_share_gb_dgroup/100 mid_mode_share_gb_dgroup,
	COALESCE(sim.sim_trips_abs_scaled_100,0) sim_trips_abs_scaled_100,
	cal.mid_trips_abs,
	cal.mid_trips_abs_rebased
FROM cal.mid_trip_stats_by_mode_distance_stuttgart_rebased cal
LEFT JOIN matsim_output.sim_trip_stats_by_mode_distance sim
ON sim.run_name = cal.run_name
AND sim.area = cal.area
AND sim.matsim_main_mode = cal.matsim_main_mode
AND sim.distance_group_no = cal.distance_group_no
ORDER BY cal.run_name, cal.area, cal.distance_group_no, cal.matsim_main_mode