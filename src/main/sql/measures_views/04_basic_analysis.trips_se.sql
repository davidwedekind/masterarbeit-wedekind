-- basic_analysis.trips__se

-- Compare trip stats that are starting or ending in areas per mode and run
-- Trip stats are number of trips, mode share, average trip distance and average trip duration
-- Respective areas for trip sarts/endings are Boeblingen and Esslingen county and focus areas in these two counties


-- @author dwedekind

		
-- Aggregate trips for agents starting/ending in either Boeblingen or Esslingen
WITH BOEB_ESSL AS
	(SELECT SIM_TRIPS_ENRICHED.RUN_NAME,
			CASE 
	 			WHEN SIM_TRIPS_ENRICHED.SE_BOEBL_ESSL = 1 THEN 'Start/ Ziel LK Boeb & Essl'
	 			ELSE 'Außer Start/ Ziel LK Boeb & Essl'
	 		END AS SE_GROUP,
			COUNT(SIM_TRIPS_ENRICHED.TRIP_ID) AS TRIPS,
			AVG(SIM_TRIPS_ENRICHED.TRAVELED_DISTANCE) AVG_TRIP_DIST,
	 		AVG(SIM_TRIPS_ENRICHED.TRAV_TIME) AVG_TRIP_DUR
		FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
		GROUP BY SIM_TRIPS_ENRICHED.RUN_NAME,
			SIM_TRIPS_ENRICHED.SE_BOEBL_ESSL),
			
			
-- Aggregate trips for agents starting/ending in focus areas of Boeblingen and Esslingen county
FOCUS_AREAS AS
	(SELECT SIM_TRIPS_ENRICHED.RUN_NAME,
			CASE 
	 			WHEN SIM_TRIPS_ENRICHED.SE_FOCUS_AREAS = 1 THEN 'Start/ Ziel Fokusgemeinden'
	 			ELSE 'Außer Start/ Ziel Fokusgemeinden'
	 		END AS SE_GROUP,
			COUNT(SIM_TRIPS_ENRICHED.TRIP_ID) AS TRIPS,
			AVG(SIM_TRIPS_ENRICHED.TRAVELED_DISTANCE) AVG_TRIP_DIST,
	 		AVG(SIM_TRIPS_ENRICHED.TRAV_TIME) AVG_TRIP_DUR
		FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
		GROUP BY SIM_TRIPS_ENRICHED.RUN_NAME,
			SIM_TRIPS_ENRICHED.SE_FOCUS_AREAS),
			

-- Union measure groups
M_GROUPS AS (
	SELECT * FROM (
		SELECT * FROM BOEB_ESSL
		UNION
		SELECT * FROM FOCUS_AREAS
	) AS UN

	ORDER BY RUN_NAME, SE_GROUP
)


-- Build final table
-- Calculate diffs and changes
-- Scale trips to 100 pct via scaling factor
SELECT
	M.RUN_NAME,
	M.SE_GROUP,
	BC.TRIPS*{**sfactor**} TRIPS_BC,
	M.TRIPS*{**sfactor**} TRIPS_M,
	(M.TRIPS - BC.TRIPS)*{**sfactor**} AS TRIPS_DIFF,
	(M.TRIPS - BC.TRIPS)/BC.TRIPS::FLOAT AS TRIPS_CHANGE,
	BC.AVG_TRIP_DIST BC_AVG_TRIP_DIST,
	M.AVG_TRIP_DIST M_AVG_TRIP_DIST,
	(M.AVG_TRIP_DIST - BC.AVG_TRIP_DIST) AS AVG_TRIP_DIST_DIFF,
	(M.AVG_TRIP_DIST - BC.AVG_TRIP_DIST)/BC.AVG_TRIP_DIST::FLOAT AS AVG_TRIP_DIST_CHANGE,
	BC.AVG_TRIP_DUR BC_AVG_TRIP_DUR,
	M.AVG_TRIP_DUR M_AVG_TRIP_DUR,
	(M.AVG_TRIP_DUR - BC.AVG_TRIP_DUR) AS AVG_TRIP_DUR_DIFF,
	(M.AVG_TRIP_DUR - BC.AVG_TRIP_DUR)/BC.AVG_TRIP_DUR::FLOAT AS AVG_TRIP_DUR_CHANGE
	
FROM M_GROUPS M
LEFT JOIN M_GROUPS BC
ON BC.SE_GROUP = M.SE_GROUP
WHERE BC.RUN_NAME = 'bc'