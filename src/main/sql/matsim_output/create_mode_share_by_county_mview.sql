CREATE MATERIALIZED VIEW matsim_output."modal_split_pro_kreis" AS (

	WITH stuttgart_trips AS (
		SELECT 
			t.*,
			SUBSTRING(h.ags, 1, 5) as kreis_ags
		FROM matsim_input.agents_homes_with_raumdata h
		INNER JOIN matsim_output.trips t
		ON h.person_id = t.person
		),

	trip_counts AS (
		SELECT
			run_name,
			kreis_ags,
			c_main_mode,
			COUNT(trip_number) as no_trips
		FROM stuttgart_trips
		GROUP BY run_name, kreis_ags, c_main_mode
		)

	SELECT
		*,  ROUND((no_trips / SUM(no_trips) OVER (partition by run_name, kreis_ags))* 100, 1) AS modal_split_sim
	FROM
		trip_counts
	ORDER BY run_name, kreis_ags, c_main_mode
	
);