-- br_analysis.br_basic_rel

-- Provide basic bike and ride usage indicators per relation
-- Trip stats are number of trips, mode share, average trip distance and average trip duration
-- Relations are LK Boeblingen - LK Esslingen and between focus areas of Boeblingen and Esslingen

-- @author dwedekind


-- Aggregate trips between Boeblingen and Esslingen
WITH BOEB_ESSL AS(
	SELECT RUN_NAME,
		CASE 
			WHEN SIM_TRIPS_ENRICHED.REL_BOEBL_ESSL = 1 THEN 'LK Boeb - LK Essl'
			ELSE 'Außer LK Boeb - LK Essl'
		END AS REL_GROUP,
		MATSIM_RAW_MAIN_MODE,
		COUNT(TRIP_ID) AS PT_TRIPS,
		COUNT(TRIP_ID)/ SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME, REL_BOEBL_ESSL) PT_TRIP_SHARE
	FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
	WHERE MATSIM_RAW_MAIN_MODE IN ('pt', 'pt_with_bike_used')
	GROUP BY RUN_NAME,
		REL_BOEBL_ESSL,
		MATSIM_RAW_MAIN_MODE
),

-- Aggregate trips for trips between focus areas of Boeblingen and Esslingen county
FOCUS_AREAS AS (
	SELECT RUN_NAME,
		CASE 
			WHEN SIM_TRIPS_ENRICHED.REL_FOCUS_AREAS = 1 THEN 'Fokusrelationen'
			ELSE 'Außer Fokusrelationen'
		END AS REL_GROUP,
		MATSIM_RAW_MAIN_MODE,
		COUNT(TRIP_ID) AS PT_TRIPS,
		COUNT(TRIP_ID)/ SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME, REL_FOCUS_AREAS) PT_TRIP_SHARE
	FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
	WHERE MATSIM_RAW_MAIN_MODE IN ('pt', 'pt_with_bike_used')
	GROUP BY RUN_NAME,
		REL_FOCUS_AREAS,
		MATSIM_RAW_MAIN_MODE
)


-- Final union
SELECT RUN_NAME,
	REL_GROUP,
	MATSIM_RAW_MAIN_MODE,
	PT_TRIPS*{**sfactor**} PT_TRIPS,
	PT_TRIP_SHARE
FROM (		
	SELECT * FROM BOEB_ESSL
	UNION
	SELECT * FROM FOCUS_AREAS
) AS UN

ORDER BY RUN_NAME, REL_GROUP, MATSIM_RAW_MAIN_MODE