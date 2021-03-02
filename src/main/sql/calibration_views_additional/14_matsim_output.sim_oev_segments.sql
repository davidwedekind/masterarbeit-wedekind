WITH legs_within_vvs AS(
	SELECT
		legs.*
	FROM matsim_output.sim_legs_raw legs
	INNER JOIN raw.areas vvs
	ON ST_Within(legs.geometry, vvs.geometry)
	WHERE vvs.subpop = 'vvs_area'
),

pt_group_per_trip AS(
	SELECT
		run_name,
		trip_id,
		pt_group,
		COUNT(index) counter
	FROM legs_within_vvs
	WHERE mode = 'pt'
	GROUP BY run_name, trip_id, pt_group)

SELECT
	run_name,
	pt_group,
	COUNT(trip_id) sgmt_abs,
	COUNT(trip_id) / SUM(COUNT(trip_id)) OVER (partition by run_name) AS sgmt_rel
FROM pt_group_per_trip
GROUP BY run_name, pt_group