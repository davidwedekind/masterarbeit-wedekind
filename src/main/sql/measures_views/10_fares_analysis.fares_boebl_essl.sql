-- fares_analysis.fares_boebl_essl

-- Provide fares indicators for relation Boeblingen - Esslingen

-- @author dwedekind


-- First, find persons with pt trips on relation of Beoeblingen - Esslingen
WITH RELEVANT_PERSONS AS
	(SELECT DISTINCT(PERSON)
		FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
		WHERE ((START_KREIS_AGS = '08115' AND END_KREIS_AGS = '08116')
									OR (END_KREIS_AGS = '08115' AND START_KREIS_AGS = '08116'))
									AND (MATSIM_CAL_MAIN_MODE = 'pt') ),
																	
	-- Then, select all pt trips of persons travelling on the relation Boeblingen - Esslingen
	-- and aggregate for each person
	RELEVANT_TRIPS AS
	(SELECT T.RUN_NAME,
		T.PERSON,
	 
	 	-- Convert units
		SUM(T.TRAVELED_DISTANCE) / 1000 TOTAL_PT_KM_DIST,
		(SUM(T.TRAV_TIME) / 3600) TOTAL_PT_H_DUR
	 
	FROM RELEVANT_PERSONS P
	LEFT JOIN MATSIM_OUTPUT.SIM_TRIPS_ENRICHED T ON P.PERSON = T.PERSON
	WHERE MATSIM_CAL_MAIN_MODE = 'pt'
	 
	GROUP BY T.RUN_NAME,
		T.PERSON),
		
	
	-- Join fares data to trip data aggregates
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
	FROM RELEVANT_TRIPS T
	JOIN MATSIM_OUTPUT.PERSON_2_FARES F ON F."personId" = T.PERSON
	AND F."run_name" = T.RUN_NAME)
	
	
-- Find aggregate average values per run
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