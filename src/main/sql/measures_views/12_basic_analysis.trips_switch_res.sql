-- basic_analysis.trips_switch_res

-- Aggregate switches to switch types n bc modes to m measure modes for each resident group
-- Find the share or each switch type
-- This is the basic analysis for the mode switch sankey diagram
-- Resident groups are LH Stuttgart, Region Stuttgart, residents of Boeblingen and Esslingen county
-- and residents of focus areas in Boeblingen and Esslingen county

-- @author dwedekind


-- Aggregate trips for all people living in the greater Stuttgart area
WITH REG_STUTTGART_COMPLETE AS (
	SELECT RUN_NAME,
		'Reg Stuttgart mit LH Stuttgart' RES_GROUP,
		FROM_BC_MODE,
		TO_M_MODE,
		COUNT(TRIP_ID) SWITCHES_ABS,
		COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME) AS SWITCHES_REL

	FROM BASIC_ANALYSIS.TRIPS_SWITCH_LIST
	WHERE SUBPOP = 'region_stuttgart'
	GROUP BY RUN_NAME,
		FROM_BC_MODE,
		TO_M_MODE
),

-- Aggregate trips for group 'LH Stuttgart'
LH_STUTTGART AS (
	SELECT RUN_NAME,
		'LH Stuttgart' RES_GROUP,
		FROM_BC_MODE,
		TO_M_MODE,
		COUNT(TRIP_ID) SWITCHES_ABS,
		COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME) AS SWITCHES_REL

	FROM BASIC_ANALYSIS.TRIPS_SWITCH_LIST
	WHERE AGENT_HOME_KRS_AGS = '08111'
	GROUP BY RUN_NAME,
		FROM_BC_MODE,
		TO_M_MODE
),

-- Aggregate trips for Region Stuttgart (without Agents living in Stuttgart)
REG_STUTTGART AS (
	SELECT RUN_NAME,
		'Reg Stuttgart' RES_GROUP,
		FROM_BC_MODE,
		TO_M_MODE,
		COUNT(TRIP_ID) SWITCHES_ABS,
		COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME) AS SWITCHES_REL

	FROM BASIC_ANALYSIS.TRIPS_SWITCH_LIST
	WHERE AREA = 'Region Stuttgart ohne LH Stuttgart'
	GROUP BY RUN_NAME,
		FROM_BC_MODE,
		TO_M_MODE
),

-- Aggregate trips for agents living in either Boeblingen or Esslingen
-- For complementary group, take only residents of greater Stuttgart area into account
BOEB_ESSL AS (
	SELECT RUN_NAME,
		CASE 
	 		WHEN RES_BOEBL_ESSL = 1 THEN 'LK Boeb & Essl'
	 		ELSE 'Außer LK Boeb & Essl'
	 	END AS RES_GROUP,
		FROM_BC_MODE,
		TO_M_MODE,
		COUNT(TRIP_ID) SWITCHES_ABS,
		COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME) AS SWITCHES_REL

	FROM BASIC_ANALYSIS.TRIPS_SWITCH_LIST
	WHERE SUBPOP = 'region_stuttgart'
	GROUP BY RUN_NAME,
		RES_BOEBL_ESSL,
		FROM_BC_MODE,
		TO_M_MODE
),

-- Aggregate trips for agents living in focus areas of Boeblingen and Esslingen county
-- For complementary group, take only residents of greater Stuttgart area into account
FOCUS_AREAS AS (
	SELECT RUN_NAME,
		CASE 
	 		WHEN RES_FOCUS_AREAS = 1 THEN 'Fokusgemeinden'
	 		ELSE 'Außer Fokusgemeinden'
	 	END AS RES_GROUP,
		FROM_BC_MODE,
		TO_M_MODE,
		COUNT(TRIP_ID) SWITCHES_ABS,
		COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME) AS SWITCHES_REL

	FROM BASIC_ANALYSIS.TRIPS_SWITCH_LIST
	WHERE SUBPOP = 'region_stuttgart'
	GROUP BY RUN_NAME,
		RES_FOCUS_AREAS,
		FROM_BC_MODE,
		TO_M_MODE
),

-- Aggregate trips for agents living anywhere but having at least one trip on relation Boebl - Essl
BOEB_ESSL_TRAV AS (
	SELECT RUN_NAME,
		CASE 
			WHEN RES_REL_BOEBL_ESSL = 1 THEN 'Boebl - Essl Reisende'
			ELSE 'Nicht Boebl - Essl Reisende'
		END AS RES_GROUP,
		FROM_BC_MODE,
		TO_M_MODE,
		COUNT(TRIP_ID) SWITCHES_ABS,
		COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME) AS SWITCHES_REL

	FROM BASIC_ANALYSIS.TRIPS_SWITCH_LIST
	GROUP BY RUN_NAME,
		RES_REL_BOEBL_ESSL,
		FROM_BC_MODE,
		TO_M_MODE
),

-- Aggregate trips for agents living anywhere but having at least one trip on relation between focus areas
FOCUS_AREA_TRAV AS (
	SELECT RUN_NAME,
		CASE 
	 		WHEN RES_REL_FOCUS_AREAS = 1 THEN 'Fokusrelationen Reisende'
	 		ELSE 'Nicht Fokusrelationen Reisende'
	 	END AS RES_GROUP,
		FROM_BC_MODE,
		TO_M_MODE,
		COUNT(TRIP_ID) SWITCHES_ABS,
		COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME) AS SWITCHES_REL

	FROM BASIC_ANALYSIS.TRIPS_SWITCH_LIST
	GROUP BY RUN_NAME,
		RES_REL_FOCUS_AREAS,
		FROM_BC_MODE,
		TO_M_MODE
),


-- Union measure groups
M_GROUPS AS (
	SELECT * FROM (			
		SELECT * FROM REG_STUTTGART_COMPLETE
		UNION
		SELECT * FROM LH_STUTTGART
		UNION
		SELECT * FROM REG_STUTTGART
		UNION
		SELECT * FROM BOEB_ESSL
		UNION
		SELECT * FROM FOCUS_AREAS
		UNION
		SELECT * FROM BOEB_ESSL_TRAV
		UNION
		SELECT * FROM FOCUS_AREA_TRAV
	) AS UN

	ORDER BY RUN_NAME, RES_GROUP
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