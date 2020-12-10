CREATE MATERIALIZED VIEW matsim_output."modal_split" AS (

	WITH stuttgart_trips AS (
		SELECT t.*, h.regiostar7, h.calib_group
		FROM matsim_input.agents_homes_with_raumdata h
		INNER JOIN matsim_output.trips t
		ON h.person_id = t.person
		WHERE h.subpop = 'region_stuttgart'
		),

	trip_counts AS (
		SELECT
			run_name,
			calib_group,
			c_main_mode,
			COUNT(trip_number) as no_trips
		FROM stuttgart_trips
		GROUP BY run_name, calib_group, c_main_mode
		)

	SELECT
		*,  ROUND((no_trips / SUM(no_trips) OVER (partition by run_name, calib_group))* 100, 1) AS modal_split_sim
	FROM
		trip_counts
	ORDER BY run_name, calib_group, c_main_mode

);