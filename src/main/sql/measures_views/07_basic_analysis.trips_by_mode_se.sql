-- basic_analysis.trips_by_mode_se

-- Compare trip stats that are starting or ending in areas per mode and run
-- Trip stats are number of trips, mode share, average trip distance and average trip duration
-- Areas are LH Stuttgart, Region Stuttgart, Agents of Boeblingen and Esslingen county
-- and Agents of focus areas in Boeblingen and Esslingen county

-- @author dwedekind
			
-- Aggregate trips for agents living in either Boeblingen or Esslingen
-- For complementary group, take only residents of greater Stuttgart area into account
WITH BOEB_ESSL AS
	(SELECT SIM_TRIPS_ENRICHED.RUN_NAME,
			CASE 
	 			WHEN SIM_TRIPS_ENRICHED.SE_BOEBL_ESSL = 1 THEN 'Start/ Ziel LK Boeb & Essl'
	 			ELSE 'Außer Start/ Ziel LK Boeb & Essl'
	 		END AS RES_GROUP,
			SIM_TRIPS_ENRICHED.MATSIM_CAL_MAIN_MODE,
			COUNT(SIM_TRIPS_ENRICHED.TRIP_ID) AS TRIPS,
			((COUNT(SIM_TRIPS_ENRICHED.TRIP_ID))::numeric / SUM(COUNT(SIM_TRIPS_ENRICHED.TRIP_ID)) OVER (PARTITION BY SIM_TRIPS_ENRICHED.RUN_NAME, SIM_TRIPS_ENRICHED.SE_BOEBL_ESSL)) AS MODE_SHARE,
	 		AVG(SIM_TRIPS_ENRICHED.TRAVELED_DISTANCE) AVG_TRIP_DIST,
	 		AVG(SIM_TRIPS_ENRICHED.TRAV_TIME) AVG_TRIP_DUR
		FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
		GROUP BY SIM_TRIPS_ENRICHED.RUN_NAME,
			SIM_TRIPS_ENRICHED.SE_BOEBL_ESSL,
			SIM_TRIPS_ENRICHED.MATSIM_CAL_MAIN_MODE),
			
			
-- Aggregate trips for agents living in focus areas of Boeblingen and Esslingen county
-- For complementary group, take only residents of greater Stuttgart area into account
FOCUS_AREAS AS
	(SELECT SIM_TRIPS_ENRICHED.RUN_NAME,
			CASE 
	 			WHEN SIM_TRIPS_ENRICHED.SE_FOCUS_AREAS = 1 THEN 'Start/ Ziel Fokusgemeinden'
	 			ELSE 'Außer Start/ Ziel Fokusgemeinden'
	 		END AS RES_GROUP,
			SIM_TRIPS_ENRICHED.MATSIM_CAL_MAIN_MODE,
			COUNT(SIM_TRIPS_ENRICHED.TRIP_ID) AS TRIPS,
			((COUNT(SIM_TRIPS_ENRICHED.TRIP_ID))::numeric / SUM(COUNT(SIM_TRIPS_ENRICHED.TRIP_ID)) OVER (PARTITION BY SIM_TRIPS_ENRICHED.RUN_NAME, SIM_TRIPS_ENRICHED.SE_FOCUS_AREAS)) AS MODE_SHARE,
	 		AVG(SIM_TRIPS_ENRICHED.TRAVELED_DISTANCE) AVG_TRIP_DIST,
	 		AVG(SIM_TRIPS_ENRICHED.TRAV_TIME) AVG_TRIP_DUR
		FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
		GROUP BY SIM_TRIPS_ENRICHED.RUN_NAME,
			SIM_TRIPS_ENRICHED.SE_FOCUS_AREAS,
			SIM_TRIPS_ENRICHED.MATSIM_CAL_MAIN_MODE),
			

-- Union measure groups
M_GROUPS AS (
	SELECT * FROM (
		SELECT * FROM BOEB_ESSL
		UNION
		SELECT * FROM FOCUS_AREAS
	) AS UN

	ORDER BY RUN_NAME, RES_GROUP, MATSIM_CAL_MAIN_MODE
),


-- Create headers first to have the full combination of run_name, res_group, mode
-- in order to display 0 fields later
HEADERS AS (
	SELECT P1.RUN_NAME,
		P2.RES_GROUP,
		P4.MATSIM_CAL_MAIN_MODE
	FROM (SELECT DISTINCT(RUN_NAME) FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED) AS P1
	CROSS JOIN (SELECT DISTINCT(RES_GROUP) FROM M_GROUPS) AS P2
	CROSS JOIN (SELECT DISTINCT(MATSIM_CAL_MAIN_MODE) FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED) AS P4

	ORDER BY P1.RUN_NAME,
		P2.RES_GROUP,
		P4.MATSIM_CAL_MAIN_MODE
)


-- Build final table
-- Calculate diffs and changes
-- Build final table
-- Calculate diffs and changes
SELECT
	H.RUN_NAME,
	H.RES_GROUP,
	H.MATSIM_CAL_MAIN_MODE "mode",
	COALESCE(BC.TRIPS, 0) TRIPS_BC,
	COALESCE(BC.MODE_SHARE, 0) MODE_SHARE_BC,
	COALESCE(M.TRIPS, 0) TRIPS_M,
	COALESCE(M.MODE_SHARE, 0) MODE_SHARE_M,
	(COALESCE(M.TRIPS, 0) - COALESCE(BC.TRIPS, 0)) TRIPS_DIFF,
	(COALESCE(M.TRIPS, 0) - COALESCE(BC.TRIPS, 0))/BC.TRIPS::FLOAT TRIPS_CHANGE,
	(COALESCE(M.MODE_SHARE,0 ) - COALESCE(BC.MODE_SHARE, 0)) MODE_SHARE_DIFF,
	COALESCE(BC.AVG_TRIP_DIST, 0) BC_AVG_TRIP_DIST,
	COALESCE(M.AVG_TRIP_DIST, 0) M_AVG_TRIP_DIST,
	(COALESCE(M.AVG_TRIP_DIST, 0) - COALESCE(BC.AVG_TRIP_DIST, 0)) AVG_TRIP_DIST_DIFF,
	(COALESCE(M.AVG_TRIP_DIST, 0) - COALESCE(BC.AVG_TRIP_DIST, 0))/BC.AVG_TRIP_DIST::FLOAT AVG_TRIP_DIST_CHANGE,
	COALESCE(BC.AVG_TRIP_DUR, 0) BC_AVG_TRIP_DUR,
	COALESCE(M.AVG_TRIP_DUR, 0) M_AVG_TRIP_DUR,
	(COALESCE(M.AVG_TRIP_DUR, 0) - COALESCE(BC.AVG_TRIP_DUR, 0)) AVG_TRIP_DUR_DIFF,
	(COALESCE(M.AVG_TRIP_DUR, 0) - COALESCE(BC.AVG_TRIP_DUR, 0))/BC.AVG_TRIP_DUR::FLOAT AVG_TRIP_DUR_CHANGE
	
FROM HEADERS H
LEFT JOIN M_GROUPS M
ON H.RUN_NAME = M.RUN_NAME
AND H.RES_GROUP = M.RES_GROUP
AND H.MATSIM_CAL_MAIN_MODE = M.MATSIM_CAL_MAIN_MODE

LEFT JOIN (SELECT * FROM M_GROUPS WHERE RUN_NAME = 'bc') BC
ON H.RES_GROUP = BC.RES_GROUP
AND H.MATSIM_CAL_MAIN_MODE = BC.MATSIM_CAL_MAIN_MODE

ORDER BY H.RUN_NAME, H.RES_GROUP, H.MATSIM_CAL_MAIN_MODE