SELECT
	sim.run_name,
	sim.pt_group,
	sim.sgmt_rel sim_sgmt_rel,
	cal.sgmt_rel/100 cal_sgmt_rel
FROM matsim_output.sim_oev_segments sim
LEFT JOIN cal.vvs_pt_segments cal
ON cal.pt_segment = sim.pt_group
ORDER BY sim.run_name, sim.pt_group