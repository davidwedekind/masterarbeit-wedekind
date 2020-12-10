CREATE MATERIALIZED VIEW matsim_output."wege_eigenschaften" AS (

	WITH stuttgart_trips AS (
			SELECT t.*, h.calib_group
			FROM matsim_input.agents_homes_with_raumdata h
			INNER JOIN matsim_output.trips t
			ON h.person_id = t.person
			WHERE h.subpop = 'region_stuttgart'
	),

	trav_time_per_person AS (
		SELECT
		run_name,
		calib_group,
		person,
		SUM(trav_time) as trav_time,
		SUM(traveled_distance) as trav_dist,
		COUNT(trip_id) as trips
		FROM stuttgart_trips
		GROUP BY run_name, calib_group, person
	)

	(
	SELECT
		run_name
		calib_group,
		'Wege_pro_Tag' as "metric",
		AVG(trips) as "value"
	FROM trav_time_per_person
	GROUP BY run_name, calib_group

	UNION

		SELECT
			run_name
			calib_group,
			'Tagesstrecke_pro_Tag' as "metric",
			AVG(trav_dist) as "value"
		FROM trav_time_per_person
		GROUP BY run_name, calib_group

	UNION

		SELECT
			run_name
			calib_group,
			'Unterwegszeit' as "metric",
			AVG(trav_time) as "value"
		FROM trav_time_per_person
		GROUP BY run_name, calib_group
		
	UNION

		SELECT
			run_name
			calib_group,
			'Weggeschwindigkeit' as "metric",
			(AVG(trav_dist)/AVG(trav_time))*3.6 as "value"
		FROM trav_time_per_person
		GROUP BY run_name, calib_group
		
	) ORDER BY calib_group

);