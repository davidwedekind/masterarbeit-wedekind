CREATE MATERIALIZED VIEW matsim_output."kreis_relationen" AS (

	WITH trips as (
		SELECT
			t.*,
			kr_s.ags as start_kreis,
			kr_e.ags as end_kreis
		FROM matsim_output.trips t
		LEFT JOIN general.kreise kr_s
		ON ST_WITHIN(ST_SetSRID(ST_MakePoint(t.start_x, t.start_y),25832), kr_s.geometry)
		LEFT JOIN general.kreise kr_e
		ON ST_WITHIN(ST_SetSRID(ST_MakePoint(t.end_x, t.end_y),25832), kr_e.geometry)
		)
		
	SELECT
		run_name,
		c_main_mode,
		start_kreis,
		end_kreis,
		COUNT(trip_number) as no_trips,
		round(((COUNT(trip_number)::numeric / SUM(COUNT(trip_number)) OVER (PARTITION BY run_name, start_kreis, end_kreis)) * (100)::numeric), 1) AS mode_share
	FROM trips
	GROUP BY run_name, c_main_mode, start_kreis, end_kreis

);

	