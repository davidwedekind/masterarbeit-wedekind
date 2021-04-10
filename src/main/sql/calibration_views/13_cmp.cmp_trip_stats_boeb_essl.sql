-- cmp.cmp_trip_stats_boeb_essl

-- Compare simulation values on Esslingen - Boeblingen county relation
-- to values in BVWP Verpflechtungsprognose

-- @author dwedekind


-- First, match the simulation modes to the corresponding modes in the BVWP Verpflechtungsprognose
WITH TMP_1 AS
	(SELECT *
			,CASE
							WHEN MATSIM_MAIN_MODE = 'car' THEN 'Miv'
							WHEN MATSIM_MAIN_MODE = 'ride' THEN 'Miv'
							WHEN MATSIM_MAIN_MODE = 'pt' THEN 'Oepnv'
							WHEN MATSIM_MAIN_MODE = 'bike' THEN 'Rad'
							WHEN MATSIM_MAIN_MODE = 'walk' THEN 'Fuß'
			END AS BVWP_MAIN_MODE
		FROM MATSIM_OUTPUT.SIM_TRIP_STATS_BOEB_ESSL),

-- Second, group by these newly identified values
	TMP_2 AS
	(SELECT RUN_NAME
			,BVWP_MAIN_MODE
			,SUM(SIM_TRIPS_ABS_SCALED_100) SIM_TRIPS_ABS_SCALED_100
			,SUM(SIM_MODE_SHARE) SIM_MODE_SHARE
		FROM TMP_1
		GROUP BY RUN_NAME,
			BVWP_MAIN_MODE
		ORDER BY RUN_NAME,
			BVWP_MAIN_MODE),
			
-- Third, create BVWP comparable
BVWP AS
	(SELECT RUNS.RUN_NAME
			,CAL.*
		FROM CAL.BOEBLINGEN_ESSLINGEN_BVWP CAL
		CROSS JOIN
			(SELECT DISTINCT(RUN_NAME)
				FROM MATSIM_OUTPUT.SIM_TRIP_STATS_BOEB_ESSL) AS RUNS
		ORDER BY RUN_NAME)


-- Finally, compare simulation with BVWP values
SELECT BVWP.RUN_NAME
	,BVWP.BVWP_MAIN_MODE
	,COALESCE(SIM_TRIPS_ABS_SCALED_100, 0) SIM_TRIPS_ABS_SCALED_100
	,COALESCE(SIM_MODE_SHARE, 0) SIM_MODE_SHARE
	,BVWP.BVWP_MODE_SHARE
	
	-- Calculate the rebased absolut trip values as
	-- the product of sum of trips over modes on relation in the simulation * BVWP mode share
	,COALESCE( BVWP.BVWP_MODE_SHARE * (SUM(SIM_TRIPS_ABS_SCALED_100) OVER (PARTITION BY BVWP.RUN_NAME)), 0) BVWP_TRIPS_ABS

FROM BVWP
LEFT JOIN TMP_2
ON TMP_2.BVWP_MAIN_MODE = BVWP.BVWP_MAIN_MODE
AND TMP_2.RUN_NAME = BVWP.RUN_NAME

ORDER BY RUN_NAME, BVWP_MAIN_MODE
