CREATE MATERIALIZED VIEW calib."cal_ms_refactor_stuttgart" AS (

	SELECT
		sim.run_name,
		sim.area,
		sim.distance_group_no,
		sim.distance_group,
		sim.sim_wege_abs,
		sim.sim_wege_abs*100 sim_wege_abs_100,
		cal.mid_wege_abs,
		(sim.sim_wege_abs*100)/cal.mid_wege_abs factor
	FROM matsim_output.sim_ms_by_dist_stuttgart sim
	LEFT JOIN calib.cal_ms_by_dist_stuttgart cal
	ON sim.area = cal.area
	AND sim.distance_group_no = cal.distance_group_no
	ORDER BY run_name, distance_group_no
	
);