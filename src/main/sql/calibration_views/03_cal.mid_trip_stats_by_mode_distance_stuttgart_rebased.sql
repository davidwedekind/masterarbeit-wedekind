SELECT
	sim.run_name,
	cal.area,
	cal.distance_group,
	cal.distance_group_no,
	cal.matsim_main_mode,
	cal.mid_mode_share_gb_dgroup,
	cal.mid_trips_abs,
	((cal.mid_mode_share_gb_dgroup*sim.sim_trips_abs)/100)*100 mid_trips_abs_rebased
FROM matsim_output.sim_trip_stats_by_distance sim
INNER JOIN cal.mid_trip_stats_by_mode_distance_stuttgart cal
ON cal.distance_group_no = sim.distance_group_no
AND cal.area = sim.area
ORDER BY sim.run_name, cal.area, cal.distance_group_no, cal.matsim_main_mode