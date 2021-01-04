SELECT
	run_name,
	area,
	distance_group_no,
	distance_group,
	COUNT(trip_id) as sim_trips_abs,
	(COUNT(trip_id) / SUM(COUNT(trip_id)) OVER (partition by run_name, area)) AS sim_trips_rel	
FROM matsim_output.sim_trips_enriched
GROUP BY run_name, area, distance_group, distance_group_no
ORDER BY run_name, area, distance_group_no