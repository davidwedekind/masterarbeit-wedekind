CREATE MATERIALIZED VIEW compare."cmp_ms_stuttgart" AS (

	SELECT
		sim.run_name,
		sim.area,
		sim.sim_main_mode,
		sim.sim_wege_abs,
		sim.sim_wege_abs*100 sim_wege_abs_100,
		cal.mid_wege_abs,
		cal.mid_wege_abs_rebased
	FROM matsim_output.sim_ms_metropolregion sim
	INNER JOIN calib.cal_ms_stuttgart_rebased cal
	ON sim.run_name = cal.run_name
	AND sim.area = cal.area
	AND sim.sim_main_mode = cal.cal_main_mode
	
);