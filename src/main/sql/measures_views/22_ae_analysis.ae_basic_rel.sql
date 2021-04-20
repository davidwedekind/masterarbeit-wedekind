-- ae_analysis.ae_basic_rel

-- Provide basic indicators of access and egress legs per mode, run and relation
-- Relations are LK Boeblingen - LK Esslingen and between focus areas of Boeblingen and Esslingen

-- @author dwedekind


-- Aggregate trips between Boeblingen and Esslingen
WITH BOEB_ESSL AS(
	SELECT RUN_NAME,
		CASE 
			WHEN REL_BOEBL_ESSL = 1 THEN 'LK Boeb - LK Essl'
			ELSE 'Außer LK Boeb - LK Essl'
		END AS REL_GROUP,
		TRIP_CAL_MAIN_MODE,
		AVG(DISTANCE) AS AVG_DIST,
		AVG(TRAV_TIME) AS AVG_DUR

	FROM AE_ANALYSIS.AE_LEGS
	GROUP BY RUN_NAME,
		REL_BOEBL_ESSL,
		TRIP_CAL_MAIN_MODE
),

-- Aggregate trips for trips between focus areas of Boeblingen and Esslingen county
FOCUS_AREAS AS(
	SELECT RUN_NAME,
		CASE 
			WHEN REL_FOCUS_AREAS = 1 THEN 'Fokusrelationen'
			ELSE 'Außer Fokusrelationen'
		END AS REL_GROUP,
		TRIP_CAL_MAIN_MODE,
		AVG(DISTANCE) AS AVG_DIST,
		AVG(TRAV_TIME) AS AVG_DUR

	FROM AE_ANALYSIS.AE_LEGS
	GROUP BY RUN_NAME,
		REL_FOCUS_AREAS,
		TRIP_CAL_MAIN_MODE
)


-- Build final union
SELECT * FROM (			
	SELECT * FROM BOEB_ESSL
	UNION
	SELECT * FROM FOCUS_AREAS
) AS UN

ORDER BY RUN_NAME, REL_GROUP







	
