SELECT
	t.*,
	h.krs_ags agent_home_krs_ags,
	h.krs_gen agent_home_krs_gen,
	h.gem_ags agent_home_gem_ags,
	h.gem_gen agent_home_gem_gen,
	h.regiostar7,
	h.calib_group area,
		CASE
			WHEN ((t.euclidean_distance >= 0) AND (t.euclidean_distance <= 499)) THEN 'unter_500m'::text
			WHEN ((t.euclidean_distance >= 500) AND (t.euclidean_distance <= 999)) THEN '500m_bis_1km'::text
			WHEN ((t.euclidean_distance >= 1000) AND (t.euclidean_distance <= 1999)) THEN '1km_bis_2km'::text
			WHEN ((t.euclidean_distance >= 2000) AND (t.euclidean_distance <= 4999)) THEN '2km_bis_5km'::text
			WHEN ((t.euclidean_distance >= 5000) AND (t.euclidean_distance <= 9999)) THEN '5km_bis_10km'::text
			WHEN ((t.euclidean_distance >= 10000) AND (t.euclidean_distance <= 19999)) THEN '10km_bis_20km'::text
			WHEN ((t.euclidean_distance >= 20000) AND (t.euclidean_distance <= 49999)) THEN '20km_bis_50km'::text
			WHEN ((t.euclidean_distance >= 50000) AND (t.euclidean_distance <= 99999)) THEN '50km_bis_100km'::text
			ELSE 'ueber_100km'::text
		END AS distance_group,
		CASE
			WHEN ((t.euclidean_distance >= 0) AND (t.euclidean_distance <= 499)) THEN 1
			WHEN ((t.euclidean_distance >= 500) AND (t.euclidean_distance <= 999)) THEN 2
			WHEN ((t.euclidean_distance >= 1000) AND (t.euclidean_distance <= 1999)) THEN 3
			WHEN ((t.euclidean_distance >= 2000) AND (t.euclidean_distance <= 4999)) THEN 4
			WHEN ((t.euclidean_distance >= 5000) AND (t.euclidean_distance <= 9999)) THEN 5
			WHEN ((t.euclidean_distance >= 10000) AND (t.euclidean_distance <= 19999)) THEN 6
			WHEN ((t.euclidean_distance >= 20000) AND (t.euclidean_distance <= 49999)) THEN 7
			WHEN ((t.euclidean_distance >= 50000) AND (t.euclidean_distance <= 99999)) THEN 8
			ELSE 9
		END AS distance_group_no,
	s_krs.ags start_kreis_ags,
	s_krs.gen start_kreis_gen,
	e_krs.ags end_kreis_ags,
	e_krs.gen end_kreis_gen
FROM (matsim_input.sim_agents_enriched h
JOIN matsim_output.sim_trips_raw t ON ((h.person_id = t.person)))
JOIN raw.kreise s_krs ON ST_WITHIN(ST_SetSRID( ST_Point(t.start_x, t.start_y), 25832), s_krs.geometry)
JOIN raw.kreise e_krs ON ST_WITHIN(ST_SetSRID( ST_Point(t.end_x, t.end_y), 25832), e_krs.geometry)
WHERE (h.subpop = 'region_stuttgart'::text)
AND (t.run_name = 'bc')