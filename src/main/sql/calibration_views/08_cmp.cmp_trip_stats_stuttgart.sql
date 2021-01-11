SELECT
	cal.run_name,
	cal.area,
	cal.matsim_main_mode,
	sim.sim_trips_abs_scaled_100,
	cal.mid_trips_abs,
	cal.mid_trips_abs_rebased,
	sim.sim_mode_share,
	cal.mid_mode_share,
	cal.mid_mode_share_rebased	
FROM matsim_output.sim_trip_stats sim
INNER JOIN cal.mid_trip_stats_stuttgart_rebased cal
ON sim.run_name = cal.run_name
AND sim.area = cal.area
AND sim.matsim_main_mode = cal.matsim_main_mode