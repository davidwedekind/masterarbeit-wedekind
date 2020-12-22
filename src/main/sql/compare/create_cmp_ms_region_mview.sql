CREATE MATERIALIZED VIEW compare."cmp_ms_region" AS (

	SELECT
		sim.run_name,
		sim.area,
		sim.sim_main_mode,
		sim.sim_wege_abs,
		sim.sim_wege_abs*100 sim_wege_abs_100,
		cal.mid_wege_abs
	FROM matsim_output.sim_ms_metropolregion sim
	INNER JOIN calib.cal_ms_metropolregion cal
	ON sim.area = cal.area
	AND sim.sim_main_mode = cal.cal_main_mode
	WHERE sim.area = 'Region Stuttgart ohne LH Stuttgart'
	
);