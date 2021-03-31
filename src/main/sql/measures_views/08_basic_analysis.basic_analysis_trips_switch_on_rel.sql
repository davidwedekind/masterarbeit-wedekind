SELECT
	run_name,
	from_bc_mode,
	to_m_mode,
	COUNT(trip_id) switches_abs,
	COUNT(trip_id) / sum(count(trip_id)) OVER (PARTITION BY run_name) AS switches_rel
FROM basic_analysis.basic_analysis_trips_switch cmp
WHERE (start_kreis_ags='08115' AND end_kreis_ags='08116') OR (start_kreis_ags='08116' AND end_kreis_ags='08115')
GROUP BY
	run_name,
	from_bc_mode,
	to_m_mode