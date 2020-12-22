CREATE MATERIALIZED VIEW matsim_output."trips_enriched" AS (

	SELECT
		t.run_name,
		t.person,
		t.trip_number,
		t.trip_id,
		t.dep_time,
		t.trav_time,
		t.wait_time,
		t.traveled_distance,
		t.euclidean_distance,
		t.longest_distance_mode,
		t.modes,
		t.start_activity_type,
		t.end_activity_type,
		t.start_facility_id,
		t.start_link,
		t.start_x,
		t.start_y,
		t.end_facility_id,
		t.end_link,
		t.end_x,
		t.end_y,
		t.first_pt_boarding_stop,
		t.last_pt_egress_stop,
		t.geometry,
		t.arr_time,
		t.trip_speed,
		t.beeline_speed,
		t.c_main_mode,
		t.m_main_mode,
		h.regiostar7,
		h.calib_group,
		h.ags AS calib_ags,
			CASE
				WHEN ((t.euclidean_distance >= 0) AND (t.euclidean_distance <= 499)) THEN 'unter_500m'::text
				WHEN ((t.euclidean_distance >= 500) AND (t.euclidean_distance <= 999)) THEN '500m_bis_1km'::text
				WHEN ((t.euclidean_distance >= 1000) AND (t.euclidean_distance <= 1999)) THEN '1km_bis_2km'::text
				WHEN ((t.euclidean_distance >= 2000) AND (t.euclidean_distance <= 4999)) THEN '2km_bis_5km'::text
				WHEN ((t.euclidean_distance >= 5000) AND (t.euclidean_distance <= 9999)) THEN '5km_bis_10km'::text
				WHEN ((t.euclidean_distance >= 10000) AND (t.euclidean_distance <= 19999)) THEN '10km_bis_20km'::text
				WHEN ((t.euclidean_distance >= 20000) AND (t.euclidean_distance <= 49999)) THEN '20km_bis_50km'::text
				WHEN ((t.euclidean_distance >= 50000) AND (t.euclidean_distance <= 99999)) THEN '50km_bis_100km'::text
				ELSE 'ueber_100km'::text
			END AS distance_group,
			CASE
				WHEN ((t.euclidean_distance >= 0) AND (t.euclidean_distance <= 499)) THEN 1
				WHEN ((t.euclidean_distance >= 500) AND (t.euclidean_distance <= 999)) THEN 2
				WHEN ((t.euclidean_distance >= 1000) AND (t.euclidean_distance <= 1999)) THEN 3
				WHEN ((t.euclidean_distance >= 2000) AND (t.euclidean_distance <= 4999)) THEN 4
				WHEN ((t.euclidean_distance >= 5000) AND (t.euclidean_distance <= 9999)) THEN 5
				WHEN ((t.euclidean_distance >= 10000) AND (t.euclidean_distance <= 19999)) THEN 6
				WHEN ((t.euclidean_distance >= 20000) AND (t.euclidean_distance <= 49999)) THEN 7
				WHEN ((t.euclidean_distance >= 50000) AND (t.euclidean_distance <= 99999)) THEN 8
				ELSE 9
			END AS distance_group_no
	FROM (matsim_input.agents h
	JOIN matsim_output.trips t ON ((h.person_id = t.person)))
	WHERE (h.subpop = 'region_stuttgart'::text)

);