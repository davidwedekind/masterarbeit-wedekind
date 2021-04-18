-- br_analysis.br_basic_se

-- Provide basic bike and ride usage indicators per trip starts/endings
-- Respective areas for trip starts/endings are Boeblingen and Esslingen county and focus areas in these two counties

-- @author dwedekind


-- Aggregate trips for agents starting/ending in either Boeblingen or Esslingen
WITH BOEB_ESSL AS(
	SELECT RUN_NAME,
		CASE 
			WHEN SIM_TRIPS_ENRICHED.SE_BOEBL_ESSL = 1 THEN 'Start/ Ziel LK Boeb & Essl'
			ELSE 'Außer Start/ Ziel LK Boeb & Essl'
		END AS SE_GROUP,
		MATSIM_RAW_MAIN_MODE,
		COUNT(TRIP_ID) AS PT_TRIPS,
		COUNT(TRIP_ID)/ SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME, SE_BOEBL_ESSL) PT_TRIP_SHARE
	FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
	WHERE MATSIM_RAW_MAIN_MODE IN ('pt', 'pt_with_bike_used')
	GROUP BY RUN_NAME,
		SE_BOEBL_ESSL,
		MATSIM_RAW_MAIN_MODE
),

-- Aggregate trips for agents starting/ending in focus areas of Boeblingen and Esslingen county
FOCUS_AREAS AS(
	SELECT RUN_NAME,
		CASE 
			WHEN SIM_TRIPS_ENRICHED.SE_FOCUS_AREAS = 1 THEN 'Start/ Ziel Fokusgemeinden'
			ELSE 'Außer Start/ Ziel Fokusgemeinden'
		END AS SE_GROUP,
		MATSIM_RAW_MAIN_MODE,
		COUNT(TRIP_ID) AS PT_TRIPS,
		COUNT(TRIP_ID)/ SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME, SE_FOCUS_AREAS) PT_TRIP_SHARE
	FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
	WHERE MATSIM_RAW_MAIN_MODE IN ('pt', 'pt_with_bike_used')
	GROUP BY RUN_NAME,
		SE_FOCUS_AREAS,
		MATSIM_RAW_MAIN_MODE
)


-- Final union
SELECT RUN_NAME,
	SE_GROUP,
	MATSIM_RAW_MAIN_MODE,
	PT_TRIPS*{**sfactor**} PT_TRIPS,
	PT_TRIP_SHARE
FROM (	
	SELECT * FROM BOEB_ESSL
	UNION
	SELECT * FROM FOCUS_AREAS
) AS UN

ORDER BY RUN_NAME, SE_GROUP, MATSIM_RAW_MAIN_MODE