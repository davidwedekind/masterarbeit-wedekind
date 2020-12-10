CREATE MATERIALIZED VIEW matsim_output."b_r_auswertung" AS (

	SELECT
		ROW_NUMBER () OVER (ORDER BY l.run_name, l."index") as "index",
		l.person,
		l.trip_id,
		l.dep_time,
		l.trav_time,
		l.wait_time,
		l.distance,
		l.mode,
		l.start_link,
		l.start_x,
		l.start_y,
		l.end_link,
		l.end_x,
		l.end_y,
		l.access_stop_id,
		l.egress_stop_id,
		l.transit_line,
		l.transit_route,
		l.geometry,
		l.arr_time,
		l.leg_speed,
		l.run_name,
		l.pt_line,
		l.pt_group
	FROM matsim_output.legs l
	LEFT JOIN matsim_output.trips t
	ON
		t.run_name = l.run_name AND
		t.trip_id = l.trip_id
	WHERE t.m_main_mode = 'pt_with_bike_used'
	
);

