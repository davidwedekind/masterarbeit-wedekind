CREATE MATERIALIZED VIEW matsim_input."sim_agents_enriched" AS (
	WITH homes AS (
		SELECT
			agents.person_id,
			agents.geometry,
			krs.ags krs_ags,
			krs.gen krs_gen,
			krs.bez krs_bez,
			gem.ags gem_ags,
			gem.gen gem_gen,
			gem.bez gem_bez,
			gem.regiostar7
		FROM matsim_input.sim_agents_raw agents
		INNER JOIN raw.gemeinden gem
		ON (st_within(agents.geometry, gem.geometry))
		INNER JOIN raw.kreise krs
		ON (st_within(agents.geometry, krs.geometry))
	),

	region_stuttgart_gemeinden AS (
		SELECT
			gem.ags,
			gem.regiostar7,
			a.subpop,
			gem.geometry,
			CASE
				WHEN gem.regiostar7 = '71' THEN 'LH Stuttgart' ELSE 'Region Stuttgart ohne LH Stuttgart'
			END AS calib_group
		FROM raw.gemeinden gem
		INNER JOIN raw.areas a
		ON (st_within(gem.geometry, a.geometry))
		WHERE a.subpop = 'region_stuttgart'
	)

	SELECT
		h.*,
		COALESCE(rsg.subpop,'outside_region_stuttgart') AS subpop,
		COALESCE(rsg.calib_group,'Nan') AS calib_group
	FROM homes h
	LEFT JOIN region_stuttgart_gemeinden rsg
	ON (st_within(h.geometry, rsg.geometry))
	
);
