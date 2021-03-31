SELECT
	run_name,
	area,
	from_bc_mode,
	to_m_mode,
	COUNT(trip_id) switches_abs,
	COUNT(trip_id) / sum(count(trip_id)) OVER (PARTITION BY run_name, area) AS switches_rel
FROM basic_analysis.basic_analysis_trips_switch cmp
GROUP BY
	run_name,
	area,
	from_bc_mode,
	to_m_mode