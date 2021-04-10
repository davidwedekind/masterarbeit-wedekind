-- fares_analysis.fares_basic

-- Provide basic fares idicators

-- @author dwedekind


-- First, aggregate pt trip data by person
-- This gives us aggregate findings of a persons daily pt trips
WITH TRIPS AS
	(SELECT T.RUN_NAME,
			T.PERSON,
			SUM(T.TRAVELED_DISTANCE) / 1000 TOTAL_PT_KM_DIST,
			(SUM(T.TRAV_TIME) / 3600) TOTAL_PT_H_DUR
		FROM MATSIM_OUTPUT.SIM_TRIPS_RAW T
		WHERE MATSIM_CAL_MAIN_MODE = 'pt'
		GROUP BY RUN_NAME,
			PERSON),
			
	-- Then, join data person's daily pt data to the fare data and build some indicators
	PT_DATA AS
	(SELECT T.RUN_NAME,
			T.PERSON,
			T.TOTAL_PT_KM_DIST,
			T.TOTAL_PT_H_DUR,
			F."noZones",
			F."fareAmount",
			F."outOfZones",
			(F."fareAmount" / T.TOTAL_PT_KM_DIST) EUR_PER_PT_KM,
			(F."fareAmount" / T.TOTAL_PT_H_DUR) EUR_PER_PT_H
		FROM TRIPS T
		JOIN MATSIM_OUTPUT.PERSON_2_FARES F ON F."personId" = T.PERSON
		AND F."run_name" = T.RUN_NAME)
		
		
-- Finally, do simple aggregation on run_name level
SELECT RUN_NAME,
	AVG("noZones") AVG_NO_ZONES_TRAV,
	AVG("fareAmount") AVG_FARE_AMT_PAID,
	AVG("fareAmount") / AVG("noZones") AVG_FARE_AMT_PER_ZONE_PAID,
	AVG(TOTAL_PT_KM_DIST) AVG_TOTAL_PT_KM_DIST,
	AVG(EUR_PER_PT_KM) AVG_EUR_PER_PT_KM,
	AVG(TOTAL_PT_H_DUR) AVG_TOTAL_PT_H_DUR,
	AVG(EUR_PER_PT_H) AVG_EUR_PER_PT_H
FROM PT_DATA
WHERE "outOfZones" = 0
GROUP BY RUN_NAME
ORDER BY RUN_NAME