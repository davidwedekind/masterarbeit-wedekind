CREATE MATERIALIZED VIEW matsim_output."oev_segmente" AS (

	WITH trips_2_pt_modes AS (
		SELECT
			l.run_name,
			l.trip_id,
			array_agg(l.pt_group) as pt_modes
		FROM matsim_output.legs l
		LEFT JOIN general.areas ar
		ON ST_WITHIN(l.geometry, ar.geometry)
		WHERE l.mode = 'pt' AND ar.subpop = 'vvs_area'
		GROUP BY run_name, trip_id
	),

	pt_mode_matrix as(
		SELECT
			run_name,
			trip_id,
			pt_modes,
			CASE WHEN 'dbregio'=ANY(pt_modes) THEN 1 ELSE 0 END as dbregio_count,
			CASE WHEN 'bus'=ANY(pt_modes) THEN 1 ELSE 0 END as bus_count,
			CASE WHEN 'stb'=ANY(pt_modes) THEN 1 ELSE 0 END as stb_count,
			CASE WHEN 'sbahn'=ANY(pt_modes) THEN 1 ELSE 0 END as sbahn_count
		FROM trips_2_pt_modes
	),

	pt_mode_counts AS (
		
		SELECT
			run_name,
			'dbregio' as pt_group,
			SUM(dbregio_count) as "value"
		FROM pt_mode_matrix
		GROUP BY run_name

		UNION

			SELECT
				run_name,
				'bus' as pt_group,
				SUM(bus_count) as "value"
			FROM pt_mode_matrix
			GROUP BY run_name

		UNION

			SELECT
				run_name,
				'stb' as pt_group,
				SUM(stb_count) as "value"
			FROM pt_mode_matrix
			GROUP BY run_name

		UNION

			SELECT
				run_name,
				'sbahn' as pt_group,
				SUM(sbahn_count) as "value"
			FROM pt_mode_matrix
			GROUP BY run_name
		
	)

	SELECT *, round(((("value")::numeric / sum("value") OVER (PARTITION BY run_name)) * (100)::numeric), 1) AS pt_split
	FROM pt_mode_counts

);
