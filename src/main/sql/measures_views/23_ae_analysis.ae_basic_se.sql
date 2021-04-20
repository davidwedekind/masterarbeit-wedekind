-- ae_analysis.ae_basic_res

-- Provide basic indicators of access and egress legs per mode, run and trip starting/end location
-- Respective areas for trip sarts/endings are Boeblingen and Esslingen county and focus areas in these two counties

-- @author dwedekind


-- Aggregate trips for agents starting/ending in either Boeblingen or Esslingen
WITH BOEB_ESSL AS(
	SELECT RUN_NAME,
		CASE 
			WHEN SE_BOEBL_ESSL = 1 THEN 'Start/ Ziel LK Boeb & Essl'
			ELSE 'Außer Start/ Ziel LK Boeb & Essl'
		END AS SE_GROUP,
		TRIP_CAL_MAIN_MODE,
		AVG(DISTANCE) AS AVG_DIST,
		AVG(TRAV_TIME) AS AVG_DUR

	FROM AE_ANALYSIS.AE_LEGS
	GROUP BY RUN_NAME,
		SE_BOEBL_ESSL,
		TRIP_CAL_MAIN_MODE
),

-- Aggregate trips for agents starting/ending in focus areas of Boeblingen and Esslingen county
FOCUS_AREAS AS(
	SELECT RUN_NAME,
		CASE 
			WHEN SE_FOCUS_AREAS = 1 THEN 'Start/ Ziel Fokusgemeinden'
			ELSE 'Außer Start/ Ziel Fokusgemeinden'
		END AS SE_GROUP,
		TRIP_CAL_MAIN_MODE,
		AVG(DISTANCE) AS AVG_DIST,
		AVG(TRAV_TIME) AS AVG_DUR

	FROM AE_ANALYSIS.AE_LEGS
	GROUP BY RUN_NAME,
		SE_FOCUS_AREAS,
		TRIP_CAL_MAIN_MODE
)



-- Build final union
SELECT * FROM (			
	SELECT * FROM BOEB_ESSL
	UNION
	SELECT * FROM FOCUS_AREAS
) AS UN

ORDER BY RUN_NAME, SE_GROUP