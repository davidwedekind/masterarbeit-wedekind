CREATE MATERIALIZED VIEW matsim_output."sim_ms_by_dist_stuttgart" AS (

	SELECT
		t.run_name,
		'LH Stuttgart'::text area,
		t.distance_group_no,
		t.distance_group,
		COUNT(t.trip_id) as sim_wege_abs
	FROM matsim_output.trips_enriched t
	WHERE t.calib_group = 'LH Stuttgart'
	GROUP BY t.run_name, t.distance_group, t.distance_group_no
	ORDER BY t.run_name, t.distance_group_no
	
);