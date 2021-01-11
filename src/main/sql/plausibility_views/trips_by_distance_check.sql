WITH mid_trips AS (
	SELECT
		area,
		distance_group_no,
		distance_group,
	SUM(mid_trips_abs) mid_trips_abs
	FROM cal.mid_trip_stats_by_mode_distance_stuttgart_rebased
	GROUP BY area, distance_group_no, distance_group
)

SELECT
	mid.area,
	mid.distance_group_no,
	mid.distance_group,
	sim.sim_trips_abs,
	sim.sim_trips_abs*4 sim_trips_abs_scaled_100,
	mid.mid_trips_abs,
	(((sim.sim_trips_abs*4)::numeric)/ (mid.mid_trips_abs::numeric))*100 sim_mid_share
FROM mid_trips mid
LEFT JOIN matsim_output.sim_trip_stats_by_distance sim
ON mid.area = sim.area
AND mid.distance_group_no = sim.distance_group_no