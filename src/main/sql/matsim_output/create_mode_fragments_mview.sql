CREATE MATERIALIZED VIEW matsim_output."nutzersegmente" AS (

	WITH region_stuttgart_trips AS (
		SELECT 
			t.*,
			h.calib_group,
			CASE WHEN c_main_mode = 'bike' THEN 1 ELSE 0 END As contains_bike,
			CASE WHEN c_main_mode = 'pt' THEN 1 ELSE 0 END As contains_pt,
			CASE WHEN c_main_mode = 'ride' THEN 1 ELSE 0 END As contains_ride,
			CASE WHEN c_main_mode = 'car' THEN 1 ELSE 0 END As contains_car,
			CASE WHEN c_main_mode = 'walk' THEN 1 ELSE 0 END As contains_walk
		FROM matsim_input.agents_homes_with_raumdata h
		INNER JOIN matsim_output.trips t
		ON h.person_id = t.person
		WHERE h.subpop = 'region_stuttgart'
	),	

	mode_counts AS (
		SELECT
			run_name,
			calib_group,
			person,	
			SUM(contains_bike) as bike_s,
			SUM(contains_pt) as pt_s,
			SUM(contains_ride) as ride_s,
			SUM(contains_car) as car_s,
			SUM(contains_walk) as walk_s
		FROM region_stuttgart_trips
		GROUP BY run_name, calib_group, person
	),
		
	groups AS(

		SELECT
			run_name,
			calib_group,
			'walk' as modes_used,
			COUNT(person) as person
		FROM mode_counts
		WHERE
			bike_s = 0 AND
			pt_s = 0 AND
			(ride_s = 0 OR car_s = 0) AND
			walk_s > 0
		GROUP BY run_name, calib_group

		UNION

		SELECT
			run_name,
			calib_group,
			'bike' as modes_used,
			COUNT(person) as person
		FROM mode_counts
		WHERE
			(bike_s > 0 AND
			pt_s = 0 AND
			(ride_s = 0 OR car_s = 0) AND
			walk_s = 0)
		GROUP BY run_name, calib_group

		UNION

		SELECT
			run_name,
			calib_group,
			'pt' as modes_used,
			COUNT(person) as person
		FROM mode_counts
		WHERE
			(bike_s = 0 AND
			pt_s > 0 AND
			(ride_s = 0 OR car_s = 0) AND
			walk_s = 0)
		GROUP BY run_name, calib_group

		UNION

		SELECT
			run_name,
			calib_group,
			'car' as modes_used,
			COUNT(person) as person
		FROM mode_counts
		WHERE
			(bike_s = 0 AND
			pt_s > 0 AND
			(ride_s > 0 OR car_s > 0) AND
			walk_s = 0)
		GROUP BY run_name, calib_group

		UNION

		SELECT
			run_name,
			calib_group,
			'car_bike' as modes_used,
			COUNT(person) as person
		FROM mode_counts
		WHERE
			(bike_s > 0 AND
			pt_s = 0 AND
			(ride_s > 0 OR car_s > 0) AND
			walk_s = 0)
		GROUP BY run_name, calib_group

		UNION

		SELECT
			run_name,
			calib_group,
			'car_pt' as modes_used,
			COUNT(person) as person
		FROM mode_counts
		WHERE
			(bike_s = 0 AND
			pt_s > 0 AND
			(ride_s > 0 OR car_s > 0) AND
			walk_s = 0)
		GROUP BY run_name, calib_group

		UNION

		SELECT
			run_name,
			calib_group,
			'bike_pt' as modes_used,
			COUNT(person) as person
		FROM mode_counts
		WHERE
			(bike_s > 0 AND
			pt_s > 0 AND
			(ride_s = 0 OR car_s = 0) AND
			walk_s = 0)
		GROUP BY run_name, calib_group

		UNION

		SELECT
			run_name,
			calib_group,
			'car_bike_pt' as modes_used,
			COUNT(person) as person
		FROM mode_counts
		WHERE
			(bike_s > 0 AND
			pt_s > 0 AND
			(ride_s > 0 OR car_s > 0) AND
			walk_s = 0)
		GROUP BY run_name, calib_group	
	)
		
		
	SELECT *, ROUND((person / SUM(person) OVER (partition by run_name, calib_group))* 100, 1) AS mode_fragments
	FROM groups
	ORDER BY run_name, calib_group, modes_used

);

