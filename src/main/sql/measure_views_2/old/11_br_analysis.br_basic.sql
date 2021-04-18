-- br_analysis.br_basic_res

-- Provide basic bike and ride usage indicators per home location area

-- @author dwedekind


-- Aggregate trips for all people living in the greater Stuttgart area
WITH REG_STUTTGART_COMPLETE AS (
	SELECT RUN_NAME,
		'Reg Stuttgart mit LH Stuttgart' RES_GROUP,
		MATSIM_RAW_MAIN_MODE,
		COUNT(TRIP_ID) AS PT_TRIPS,
		COUNT(TRIP_ID)/ SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME) PT_TRIP_SHARE
	FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
	WHERE MATSIM_RAW_MAIN_MODE IN ('pt', 'pt_with_bike_used')
	AND SUBPOP = 'region_stuttgart'
	GROUP BY RUN_NAME,
		MATSIM_RAW_MAIN_MODE
),

-- Aggregate trips for group 'LH Stuttgart'
LH_STUTTGART AS (
	SELECT RUN_NAME,
		'LH Stuttgart' RES_GROUP,
		MATSIM_RAW_MAIN_MODE,
		COUNT(TRIP_ID) AS PT_TRIPS,
		COUNT(TRIP_ID)/ SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME) PT_TRIP_SHARE
	FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
	WHERE MATSIM_RAW_MAIN_MODE IN ('pt', 'pt_with_bike_used')
	AND AGENT_HOME_KRS_AGS = '08111'
	GROUP BY RUN_NAME,
		MATSIM_RAW_MAIN_MODE
),

-- Aggregate trips for Region Stuttgart (without Agents living in Stuttgart)
REG_STUTTGART AS (
	SELECT RUN_NAME,
		'Reg Stuttgart' RES_GROUP,
		MATSIM_RAW_MAIN_MODE,
		COUNT(TRIP_ID) AS PT_TRIPS,
		COUNT(TRIP_ID)/ SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME) PT_TRIP_SHARE
	FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
	WHERE MATSIM_RAW_MAIN_MODE IN ('pt', 'pt_with_bike_used')
	AND AREA = 'Region Stuttgart ohne LH Stuttgart'
	GROUP BY RUN_NAME,
		MATSIM_RAW_MAIN_MODE
),

-- Aggregate trips for agents living in either Boeblingen or Esslingen
-- For complementary group, take only residents of greater Stuttgart area into account
-- Aggregate trips for Region Stuttgart (without Agents living in Stuttgart)
BOEB_ESSL AS (
	SELECT RUN_NAME,
		CASE 
			WHEN SIM_TRIPS_ENRICHED.RES_BOEBL_ESSL = 1 THEN 'LK Boeb & Essl'
			ELSE 'Außer LK Boeb & Essl'
		END AS RES_GROUP,
		MATSIM_RAW_MAIN_MODE,
		COUNT(TRIP_ID) AS PT_TRIPS,
		COUNT(TRIP_ID)/ SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME, RES_BOEBL_ESSL) PT_TRIP_SHARE
	FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
	WHERE MATSIM_RAW_MAIN_MODE IN ('pt', 'pt_with_bike_used')
	AND SUBPOP = 'region_stuttgart'
	GROUP BY RUN_NAME,
		RES_BOEBL_ESSL,
		MATSIM_RAW_MAIN_MODE
),

-- Aggregate trips for agents living in focus areas of Boeblingen and Esslingen county
-- For complementary group, take only residents of greater Stuttgart area into account
FOCUS_AREAS AS (
	SELECT RUN_NAME,
		CASE 
			WHEN SIM_TRIPS_ENRICHED.RES_FOCUS_AREAS = 1 THEN 'Fokusgemeinden'
			ELSE 'Außer Fokusgemeinden'
		END AS RES_GROUP,
		MATSIM_RAW_MAIN_MODE,
		COUNT(TRIP_ID) AS PT_TRIPS,
		COUNT(TRIP_ID)/ SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME, RES_FOCUS_AREAS) PT_TRIP_SHARE
	FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
	WHERE MATSIM_RAW_MAIN_MODE IN ('pt', 'pt_with_bike_used')
	AND SUBPOP = 'region_stuttgart'
	GROUP BY RUN_NAME,
		RES_FOCUS_AREAS,
		MATSIM_RAW_MAIN_MODE
),

-- Aggregate trips for agents living anywhere but having at least one trip on relation Boebl - Essl
BOEB_ESSL_TRAV AS (
	SELECT RUN_NAME,
		CASE 
			WHEN SIM_TRIPS_ENRICHED.RES_REL_BOEBL_ESSL = 1 THEN 'Boebl - Essl Reisende'
			ELSE 'Nicht Boebl - Essl Reisende'
		END AS RES_GROUP,
		MATSIM_RAW_MAIN_MODE,
		COUNT(TRIP_ID) AS PT_TRIPS,
		COUNT(TRIP_ID)/ SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME, RES_REL_BOEBL_ESSL) PT_TRIP_SHARE
	FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
	WHERE MATSIM_RAW_MAIN_MODE IN ('pt', 'pt_with_bike_used')
	AND SUBPOP = 'region_stuttgart'
	GROUP BY RUN_NAME,
		RES_REL_BOEBL_ESSL,
		MATSIM_RAW_MAIN_MODE
),

-- Aggregate trips for agents living anywhere but having at least one trip on relation between focus areas
FOCUS_AREAS_TRAV AS(
	SELECT RUN_NAME,
		CASE 
			WHEN SIM_TRIPS_ENRICHED.RES_REL_FOCUS_AREAS = 1 THEN 'Fokusrelationen Reisende'
			ELSE 'Nicht Fokusrelationen Reisende'
		END AS RES_GROUP,
		MATSIM_RAW_MAIN_MODE,
		COUNT(TRIP_ID) AS PT_TRIPS,
		COUNT(TRIP_ID)/ SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME, RES_REL_FOCUS_AREAS) PT_TRIP_SHARE
	FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
	WHERE MATSIM_RAW_MAIN_MODE IN ('pt', 'pt_with_bike_used')
	AND SUBPOP = 'region_stuttgart'
	GROUP BY RUN_NAME,
		RES_REL_FOCUS_AREAS,
		MATSIM_RAW_MAIN_MODE
)


-- Final union
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

ORDER BY RUN_NAME, RES_GROUP, MATSIM_RAW_MAIN_MODE