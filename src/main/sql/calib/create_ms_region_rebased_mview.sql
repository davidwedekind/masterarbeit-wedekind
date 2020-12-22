CREATE MATERIALIZED VIEW calib."cal_ms_region_rebased" AS (

	SELECT
		cal.area,
		cal.cal_main_mode,
		cal.mid_mode_share,
		sim.sim_wege_abs,
		sim.sim_wege_abs*100 sim_wege_abs_100,
		(cal.mid_mode_share*sim.sim_wege_abs*100) cal_wege_abs_rescaled
	FROM
	calib.cal_ms_metropolregion cal
	INNER JOIN matsim_output.sim_ms_metropolregion sim
	ON cal.area = sim.area
	WHERE cal.area = 'Region Stuttgart ohne LH Stuttgart'
	
);