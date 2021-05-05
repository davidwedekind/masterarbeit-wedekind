SELECT 
	CASE
		WHEN be.ags LIKE '08115%' THEN 'Böblingen'
		ELSE 'Esslingen'
	END AS kreis_gen,
	be.ags,
	be.gen,
	ag.agents,
	(ag.agents*4) agents_100pct,
	be.res_from_14,
	((ag.agents*4) / be.res_from_14::float) AS agent_res_share
	
FROM raw.boebl_essl_ew be
LEFT JOIN (SELECT sim_agents_enriched.gem_ags ags,
	sim_agents_enriched.gem_gen,
    count(sim_agents_enriched.person_id) AS agents
FROM matsim_input.sim_agents_enriched
GROUP BY sim_agents_enriched.gem_ags, sim_agents_enriched.gem_gen
ORDER BY sim_agents_enriched.gem_gen) AS ag

ON ag.ags = be.ags
ORDER BY agent_res_share DESC