SELECT
	t.*,
	s_krs.ags start_kreis_ags,
	s_krs.gen start_kreis_gen,
	e_krs.ags end_kreis_ags,
	e_krs.gen end_kreis_gen
FROM matsim_output.sim_trips_enriched t
JOIN raw.kreise s_krs ON ST_WITHIN(ST_SetSRID( ST_Point(t.start_x, t.start_y), 25832), s_krs.geometry)
JOIN raw.kreise e_krs ON ST_WITHIN(ST_SetSRID( ST_Point(t.end_x, t.end_y), 25832), e_krs.geometry)
WHERE (s_krs.ags='08115' AND e_krs.ags='08116')
OR (s_krs.ags='08116' AND e_krs.ags='08115')