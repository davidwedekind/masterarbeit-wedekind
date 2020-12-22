CREATE MATERIALIZED VIEW matsim_output."h3_res_7_relationen" AS (

	WITH trips as (
		SELECT
			t.*,
			h3_s.h3_id as start_h3,
			h3_e.h3_id as end_h3
		FROM matsim_output.trips t
		LEFT JOIN general.h3_res_7 h3_s
		ON ST_WITHIN(ST_SetSRID(ST_MakePoint(t.start_x, t.start_y),25832), h3_s.geometry)
		LEFT JOIN general.h3_res_7 h3_e
		ON ST_WITHIN(ST_SetSRID(ST_MakePoint(t.end_x, t.end_y),25832), h3_e.geometry)
		)
		
	SELECT
		run_name,
		c_main_mode,
		start_h3,
		end_h3,
		COUNT(trip_number) as no_trips,
		round(((COUNT(trip_number)::numeric / SUM(COUNT(trip_number)) OVER (PARTITION BY run_name, start_h3, end_h3)) * (100)::numeric), 1) AS mode_share
	FROM trips
	GROUP BY run_name, c_main_mode, start_h3, end_h3
	ORDER BY start_h3, end_h3

);