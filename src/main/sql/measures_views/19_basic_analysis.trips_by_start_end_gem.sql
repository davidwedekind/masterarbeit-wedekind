WITH trip_starts AS (
	SELECT
		run_name,
		start_gem_ags,
		start_gem_gen,
		matsim_cal_main_mode,
		COUNT(trip_id) trips_starting
	FROM matsim_output.sim_trips_enriched
	GROUP BY run_name, start_gem_ags, start_gem_gen, matsim_cal_main_mode
),

trip_ends AS (
	SELECT
		run_name,
		end_gem_ags,
		end_gem_gen,
		matsim_cal_main_mode,
		COUNT(trip_id) trips_ending
	FROM matsim_output.sim_trips_enriched
	GROUP BY run_name, end_gem_ags, end_gem_gen, matsim_cal_main_mode
),

headers AS(
	SELECT DISTINCT run_name, gem_ags, gem_gen, matsim_cal_main_mode
	FROM((SELECT run_name, start_gem_ags gem_ags, start_gem_gen gem_gen, matsim_cal_main_mode FROM trip_starts) UNION
		(SELECT run_name, end_gem_ags gem_ags, end_gem_gen gem_gen, matsim_cal_main_mode FROM trip_ends)
	) As un
)

SELECT
	h.run_name,
	h.gem_ags,
	h.gem_gen,
	h.matsim_cal_main_mode,
	s.trips_starting,
	(s.trips_starting / SUM(s.trips_starting) OVER (partition by h.run_name, h.gem_ags)) AS mode_share_start,
	e.trips_ending,
	(e.trips_ending / SUM(e.trips_ending) OVER (partition by h.run_name, h.gem_ags)) AS mode_share_end
FROM trip_starts s
JOIN trip_ends e
ON s.run_name = e.run_name
AND s.start_gem_ags = e.end_gem_ags
AND s.matsim_cal_main_mode = e.matsim_cal_main_mode
FULL JOIN headers h
ON s.run_name = h.run_name
AND s.start_gem_ags = h.gem_ags
AND s.matsim_cal_main_mode = h.matsim_cal_main_mode