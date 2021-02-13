WITH sums AS (
	SELECT
		run_name,
		area,
		SUM(sim_trips_abs_scaled_100) trip_sum_scaled_100
	FROM matsim_output.sim_trip_stats
	WHERE area = 'Region Stuttgart ohne LH Stuttgart'
	GROUP BY run_name, area
)

SELECT
	sim.run_name,
	cal.area,
	cal.matsim_main_mode,
	sim.sim_trips_abs,
	sim.sim_trips_abs_scaled_100,
	sim_trips_abs / SUM(sim_trips_abs) OVER (partition by sim.run_name) AS sim_mode_share,
	cal.mid_mode_share,
	(cal.mid_mode_share*sums.trip_sum_scaled_100)/100 mid_trips_abs
FROM matsim_output.sim_trip_stats sim
JOIN cal.mid_trip_stats_multiple_level cal ON (sim.area = cal.area
AND sim.matsim_main_mode=cal.matsim_main_mode)
JOIN sums ON (sim.area = sums.area
AND sim.run_name = sums.run_name)
ORDER BY sim.run_name, sim.matsim_main_mode