CREATE MATERIALIZED VIEW matsim_output."gem_relationen" AS (

	WITH trips as (
		SELECT
			t.*,
			gem_s.ags as start_gem,
			gem_e.ags as end_gem
		FROM matsim_output.trips t
		LEFT JOIN general.gemeinden gem_s
		ON ST_WITHIN(ST_SetSRID(ST_MakePoint(t.start_x, t.start_y),25832), gem_s.geometry)
		LEFT JOIN general.gemeinden gem_e
		ON ST_WITHIN(ST_SetSRID(ST_MakePoint(t.end_x, t.end_y),25832), gem_e.geometry)
		)
		
	SELECT
		run_name,
		c_main_mode,
		start_gem,
		end_gem,
		COUNT(trip_number) as no_trips,
		round(((COUNT(trip_number)::numeric / SUM(COUNT(trip_number)) OVER (PARTITION BY run_name, start_gem, end_gem)) * (100)::numeric), 1) AS mode_share
	FROM trips
	GROUP BY run_name, c_main_mode, start_gem, end_gem

);


