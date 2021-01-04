WITH rebased AS (	
	SELECT
		run_name,
		area,
		matsim_main_mode,
		SUM(mid_trips_abs_rebased) mid_trips_abs_rebased,
		(SUM(mid_trips_abs_rebased) / SUM(SUM(mid_trips_abs_rebased)) OVER (partition by run_name)) mid_mode_share_rebased
	FROM cal.mid_trip_stats_by_mode_distance_stuttgart_rebased
	GROUP BY run_name, area, matsim_main_mode
	ORDER BY run_name, area, matsim_main_mode
)

SELECT
	reb.run_name,
	reb.area,
	reb.matsim_main_mode,
	cal.mid_trips_abs,
	reb.mid_trips_abs_rebased,
	cal.mid_mode_share/100 mid_mode_share,
	reb.mid_mode_share_rebased
FROM rebased reb
INNER JOIN cal.mid_trip_stats_multiple_level cal
ON cal.area = reb.area
AND cal.matsim_main_mode = reb.matsim_main_mode