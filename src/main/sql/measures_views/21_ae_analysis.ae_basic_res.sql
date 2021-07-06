-- ae_analysis.ae_basic_res

-- Provide basic indicators of access and egress legs per mode, run and agent group of residents
-- Resident groups are LH Stuttgart, Region Stuttgart, residents of Boeblingen and Esslingen county
-- and residents of focus areas in Boeblingen and Esslingen county

-- @author dwedekind


-- Aggregate trips for all people living in the greater Stuttgart area
WITH REG_STUTTGART_COMPLETE AS(
	SELECT RUN_NAME,
		'Reg Stuttgart mit LH Stuttgart' RES_GROUP,
		TRIP_CAL_MAIN_MODE,
		AVG(DISTANCE) AS AVG_DIST,
		AVG(TRAV_TIME) AS AVG_DUR

	FROM AE_ANALYSIS.AE_LEGS
	WHERE SUBPOP = 'region_stuttgart'
	GROUP BY RUN_NAME,
		TRIP_CAL_MAIN_MODE
),

-- Aggregate trips for group 'LH Stuttgart'
LH_STUTTGART AS(
	SELECT RUN_NAME,
		'LH Stuttgart' RES_GROUP,
		TRIP_CAL_MAIN_MODE,
		AVG(DISTANCE) AS AVG_DIST,
		AVG(TRAV_TIME) AS AVG_DUR

	FROM AE_ANALYSIS.AE_LEGS
	WHERE AGENT_HOME_KRS_AGS = '08111'
	GROUP BY RUN_NAME,
		TRIP_CAL_MAIN_MODE
),

-- Aggregate trips for Region Stuttgart (without Agents living in Stuttgart)
REG_STUTTGART AS(
	SELECT RUN_NAME,
		'Reg Stuttgart' RES_GROUP,
		TRIP_CAL_MAIN_MODE,
		AVG(DISTANCE) AS AVG_DIST,
		AVG(TRAV_TIME) AS AVG_DUR

	FROM AE_ANALYSIS.AE_LEGS
	WHERE AREA = 'Region Stuttgart ohne LH Stuttgart'
	GROUP BY RUN_NAME,
		TRIP_CAL_MAIN_MODE
),

-- Aggregate trips for agents living in either Boeblingen or Esslingen
-- For complementary group, take only residents of greater Stuttgart area into account
BOEB_ESSL AS(
	SELECT RUN_NAME,
		CASE 
			WHEN RES_BOEBL_ESSL = 1 THEN 'LK Boeb & Essl'
			ELSE 'Außer LK Boeb & Essl'
		END AS RES_GROUP,
		TRIP_CAL_MAIN_MODE,
		AVG(DISTANCE) AS AVG_DIST,
		AVG(TRAV_TIME) AS AVG_DUR

	FROM AE_ANALYSIS.AE_LEGS
	WHERE SUBPOP = 'region_stuttgart'
	GROUP BY RUN_NAME,
		RES_BOEBL_ESSL,
		TRIP_CAL_MAIN_MODE
),

-- Aggregate trips for agents living in focus areas of Boeblingen and Esslingen county
-- For complementary group, take only residents of greater Stuttgart area into account
FOCUS_AREAS AS(
	SELECT RUN_NAME,
		CASE 
			WHEN RES_FOCUS_AREAS = 1 THEN 'Fokusgemeinden'
			ELSE 'Außer Fokusgemeinden'
		END AS RES_GROUP,
		TRIP_CAL_MAIN_MODE,
		AVG(DISTANCE) AS AVG_DIST,
		AVG(TRAV_TIME) AS AVG_DUR

	FROM AE_ANALYSIS.AE_LEGS
	WHERE SUBPOP = 'region_stuttgart'
	GROUP BY RUN_NAME,
		RES_FOCUS_AREAS,
		TRIP_CAL_MAIN_MODE
),

-- Aggregate trips for agents living anywhere but having at least one trip on relation Boebl - Essl
BOEB_ESSL_TRAV AS(
	SELECT RUN_NAME,
		CASE 
			WHEN RES_REL_BOEBL_ESSL = 1 THEN 'Boebl - Essl Reisende'
			ELSE 'Nicht Boebl - Essl Reisende'
		END AS RES_GROUP,
		TRIP_CAL_MAIN_MODE,
		AVG(DISTANCE) AS AVG_DIST,
		AVG(TRAV_TIME) AS AVG_DUR

	FROM AE_ANALYSIS.AE_LEGS
	GROUP BY RUN_NAME,
		RES_REL_BOEBL_ESSL,
		TRIP_CAL_MAIN_MODE
),

-- Aggregate trips for agents living anywhere but having at least one trip on relation between focus areas
FOCUS_AREAS_TRAV AS(
	SELECT RUN_NAME,
		CASE 
			WHEN RES_REL_FOCUS_AREAS = 1 THEN 'Fokusrelationen Reisende'
			ELSE 'Nicht Fokusrelationen Reisende'
		END AS RES_GROUP,
		TRIP_CAL_MAIN_MODE,
		AVG(DISTANCE) AS AVG_DIST,
		AVG(TRAV_TIME) AS AVG_DUR

	FROM AE_ANALYSIS.AE_LEGS
	GROUP BY RUN_NAME,
		RES_REL_FOCUS_AREAS,
		TRIP_CAL_MAIN_MODE
)



-- Build final union
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
	SELECT * FROM FOCUS_AREAS_TRAV
) AS UN

ORDER BY RUN_NAME, RES_GROUP





	
