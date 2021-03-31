SELECT
	m.run_name,
	bc.person,
	bc.trip_id,
	bc.area,
	bc.agent_home_krs_ags,
	bc.agent_home_krs_gen,
	bc.start_kreis_ags,
	bc.start_kreis_gen,
	bc.end_kreis_ags,
	bc.end_kreis_gen,
	bc.matsim_cal_main_mode from_bc_mode,
	m.matsim_cal_main_mode to_m_mode
FROM matsim_output.sim_trips_enriched_bc bc
JOIN matsim_output.sim_trips_enriched_measures m
ON bc.trip_id = m.trip_id