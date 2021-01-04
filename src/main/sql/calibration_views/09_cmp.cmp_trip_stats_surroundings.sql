SELECT
	sim.run_name,
	cal.area,
	cal.matsim_main_mode,
	sim.sim_mode_share,
	cal.mid_mode_share/100 mid_mode_share,
	sim.sim_trips_abs_scaled_100,
	cal.mid_trips_abs
FROM matsim_output.sim_trip_stats sim
INNER JOIN cal.mid_trip_stats_multiple_level cal
ON sim.area = cal.area
AND sim.matsim_main_mode = cal.matsim_main_mode
WHERE cal.area = 'Region Stuttgart ohne LH Stuttgart'