-- basic_analysis.trips_switch_se

-- Aggregate switches for trips that are starting or ending in areas per mode and run
-- for the Boeblingen-Esslingen county relation
-- Find the share or each switch type
-- This is the basic analysis for the mode switch sankey diagram


-- @author dwedekind


-- Aggregate trips for agents starting/ending in either Boeblingen or Esslingen
WITH BOEB_ESSL AS (
	SELECT RUN_NAME,
		CASE 
	 		WHEN SE_BOEBL_ESSL = 1 THEN 'Start/ Ziel LK Boeb & Essl'
	 		ELSE 'Außer Start/ Ziel LK Boeb & Essl'
	 	END AS SE_GROUP,
		FROM_BC_MODE,
		TO_M_MODE,
		COUNT(TRIP_ID) SWITCHES_ABS,
		COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME) AS SWITCHES_REL

	FROM BASIC_ANALYSIS.TRIPS_SWITCH_LIST
	GROUP BY RUN_NAME,
		SE_BOEBL_ESSL,
		FROM_BC_MODE,
		TO_M_MODE
),

-- Aggregate trips for agents starting/ending in focus areas of Boeblingen and Esslingen county
FOCUS_AREAS AS (
	SELECT RUN_NAME,
		CASE 
	 		WHEN SE_FOCUS_AREAS = 1 THEN 'Start/ Ziel Fokusgemeinden'
	 		ELSE 'Außer Start/ Ziel Fokusgemeinden'
	 	END AS SE_GROUP,
		FROM_BC_MODE,
		TO_M_MODE,
		COUNT(TRIP_ID) SWITCHES_ABS,
		COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME) AS SWITCHES_REL

	FROM BASIC_ANALYSIS.TRIPS_SWITCH_LIST
	GROUP BY RUN_NAME,
		SE_FOCUS_AREAS,
		FROM_BC_MODE,
		TO_M_MODE
),


-- Union measure groups
M_GROUPS AS (
	SELECT * FROM (
		SELECT * FROM BOEB_ESSL
		UNION
		SELECT * FROM FOCUS_AREAS
	) AS UN

	ORDER BY RUN_NAME, SE_GROUP
)


-- Build final table
-- Scale trips to 100 pct via scaling factor
SELECT RUN_NAME,
	RES_GROUP,
	FROM_BC_MODE,
	TO_M_MODE,
	SWITCHES_ABS*{**sfactor**} SWITCHES_ABS,
	SWITCHES_REL
FROM M_GROUPS