CREATE MATERIALIZED VIEW matsim_input."agents_homes_with_raumdata" AS (

	WITH homes AS (
		SELECT
			agent.person_id,
			agent.geometry,
			gem.ags,
			gem.gen,
			gem.bez,
			gem.regiostar7
		FROM matsim_input.agent_home_locations agent
		INNER JOIN general.gemeinden gem
		ON (st_within(agent.geometry, gem.geometry))
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
		FROM general.gemeinden gem
		INNER JOIN general.areas a
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
