CREATE MATERIALIZED VIEW calib."cal_ms_stuttgart_rebased" AS (

	WITH cal_ms_by_mode_dist_stuttgart_rebased AS (
		
		SELECT
			r.run_name,
			c.area,
			c.distance_group,
			c.distance_group_no,
			c.cal_main_mode,
			c.mid_wege_abs,
			r.factor,
			c.mid_wege_abs*r.factor mid_wege_abs_rebased
		FROM calib.cal_ms_refactor_stuttgart r
		LEFT JOIN calib.cal_ms_by_mode_dist_stuttgart c
		ON r.distance_group_no = c.distance_group_no
		
	)

	SELECT
		run_name,
		area,
		cal_main_mode,
		SUM(mid_wege_abs) mid_wege_abs,
		SUM(mid_wege_abs_rebased) mid_wege_abs_rebased
	FROM cal_ms_by_mode_dist_stuttgart_rebased
	GROUP BY run_name, area, cal_main_mode
	ORDER BY run_name, area, cal_main_mode
	
);