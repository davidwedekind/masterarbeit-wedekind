WITH home_agg AS (
	SELECT
		krs_ags,
		krs_gen,
		krs_bez,
		COUNT(person_id) sim_residents
	FROM matsim_input.sim_agents_enriched
	GROUP BY krs_ags, krs_gen, krs_bez
)

SELECT
	agg.krs_ags,
	agg.krs_gen,
	agg.krs_bez,
	krs.geometry,
	agg.sim_residents,
	agg.sim_residents*4 sim_residents_scaled_100,
	pla.residents destatis_residents,
	((agg.sim_residents*4)::double precision/pla.residents::double precision) sim_destatis_share
FROM home_agg agg
INNER JOIN raw.kreise krs
ON krs.ags = agg.krs_ags
INNER JOIN plausibility.mid_plausi_trip_calc pla
ON pla.ags = agg.krs_ags