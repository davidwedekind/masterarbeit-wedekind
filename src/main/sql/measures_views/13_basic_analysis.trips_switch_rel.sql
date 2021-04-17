-- basic_analysis.trips_switch_rel

-- Aggregate switches to switch types n bc modes to m measure modes for each relation group
-- Find the share or each switch type
-- This is the basic analysis for the mode switch sankey diagram
-- Relations are LK Boeblingen - LK Esslingen and between focus areas of Boeblingen and Esslingen

-- @author dwedekind



-- Aggregate trips between Boeblingen and Esslingen
WITH BOEB_ESSL AS (
	SELECT RUN_NAME,
		CASE 
			WHEN REL_BOEBL_ESSL = 1 THEN 'LK Boeb - LK Essl'
			ELSE 'Außer LK Boeb - LK Essl'
		END AS REL_GROUP,
		FROM_BC_MODE,
		TO_M_MODE,
		COUNT(TRIP_ID) SWITCHES_ABS,
		COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME) AS SWITCHES_REL

	FROM BASIC_ANALYSIS.TRIPS_SWITCH_LIST

	GROUP BY RUN_NAME,
	REL_BOEBL_ESSL,
	FROM_BC_MODE,
	TO_M_MODE
),

-- Aggregate trips for trips between focus areas of Boeblingen and Esslingen county
FOCUS_AREAS AS (
	SELECT RUN_NAME,
		CASE 
	 		WHEN REL_FOCUS_AREAS = 1 THEN 'Fokusrelationen'
	 		ELSE 'Außer Fokusrelationen'
	 	END AS REL_GROUP,
		FROM_BC_MODE,
		TO_M_MODE,
		COUNT(TRIP_ID) SWITCHES_ABS,
		COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME) AS SWITCHES_REL

	FROM BASIC_ANALYSIS.TRIPS_SWITCH_LIST

	GROUP BY RUN_NAME,
	REL_FOCUS_AREAS,
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

	ORDER BY RUN_NAME, REL_GROUP
)


-- Build final table
-- Scale trips to 100 pct via scaling factor
SELECT RUN_NAME,
	REL_GROUP,
	FROM_BC_MODE,
	TO_M_MODE,
	SWITCHES_ABS*{**sfactor**} SWITCHES_ABS,
	SWITCHES_REL
FROM M_GROUPS