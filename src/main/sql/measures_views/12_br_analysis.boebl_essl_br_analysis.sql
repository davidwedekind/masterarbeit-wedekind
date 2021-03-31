WITH trip_stats_bc AS (
	SELECT
		sim_trips_enriched_bc.run_name,
		sim_trips_enriched_bc.matsim_raw_main_mode,
        count(sim_trips_enriched_bc.trip_id) AS trips_bc
    FROM matsim_output.sim_trips_enriched_bc
	WHERE sim_trips_enriched_bc.matsim_raw_main_mode = 'pt_with_bike_used'
	AND ((start_kreis_ags = '08115' AND end_kreis_ags = '08116')
	OR (end_kreis_ags = '08115' AND start_kreis_ags = '08116'))
    GROUP BY sim_trips_enriched_bc.run_name, sim_trips_enriched_bc.matsim_raw_main_mode
        )
	,trip_stats_measures AS (
		SELECT
			sim_trips_enriched_measures.run_name,
            sim_trips_enriched_measures.matsim_raw_main_mode,
            count(sim_trips_enriched_measures.trip_id) AS trips_m
        FROM matsim_output.sim_trips_enriched_measures
		WHERE sim_trips_enriched_measures.matsim_raw_main_mode = 'pt_with_bike_used'
		AND ((start_kreis_ags = '08115' AND end_kreis_ags = '08116')
		OR (end_kreis_ags = '08115' AND start_kreis_ags = '08116'))
        GROUP BY sim_trips_enriched_measures.run_name, sim_trips_enriched_measures.matsim_raw_main_mode
        )
SELECT m.run_name,
    bc.matsim_raw_main_mode,
    bc.trips_bc,
    m.trips_m,
    (m.trips_m - bc.trips_bc) AS trips_diff
FROM (trip_stats_bc bc
JOIN trip_stats_measures m ON ((bc.matsim_raw_main_mode = m.matsim_raw_main_mode)))
ORDER BY m.run_name, bc.matsim_raw_main_mode