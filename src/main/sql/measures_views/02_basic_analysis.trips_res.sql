-- basic_analysis.trips_by_res

-- Compare trip stats for each area of agent residency per run
-- Trip stats are number of trips, mode share, average trip distance and average trip duration
-- Areas are LH Stuttgart, Region Stuttgart, Agents of Boeblingen and Esslingen county
-- and Agents of focus areas in Boeblingen and Esslingen county

-- @author dwedekind


-- Aggregate trips for all people living in the greater Stuttgart area
WITH REG_STUTTGART_COMPLETE AS
	(SELECT SIM_TRIPS_ENRICHED.RUN_NAME,
			'Reg Stuttgart mit LH Stuttgart' RES_GROUP,
			COUNT(SIM_TRIPS_ENRICHED.TRIP_ID) AS TRIPS,
	 		AVG(SIM_TRIPS_ENRICHED.TRAVELED_DISTANCE) AVG_TRIP_DIST,
	 		AVG(SIM_TRIPS_ENRICHED.TRAV_TIME) AVG_TRIP_DUR
		FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
		WHERE (SIM_TRIPS_ENRICHED.SUBPOP = 'region_stuttgart')
		GROUP BY SIM_TRIPS_ENRICHED.RUN_NAME),
			
-- Aggregate trips for group 'LH Stuttgart'
LH_STUTTGART AS
	(SELECT SIM_TRIPS_ENRICHED.RUN_NAME,
			'LH Stuttgart' RES_GROUP,
			COUNT(SIM_TRIPS_ENRICHED.TRIP_ID) AS TRIPS,
	 		AVG(SIM_TRIPS_ENRICHED.TRAVELED_DISTANCE) AVG_TRIP_DIST,
	 		AVG(SIM_TRIPS_ENRICHED.TRAV_TIME) AVG_TRIP_DUR
		FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
		WHERE (SIM_TRIPS_ENRICHED.AGENT_HOME_KRS_AGS = '08111')
		GROUP BY SIM_TRIPS_ENRICHED.RUN_NAME),

-- Aggregate trips for Region Stuttgart (without Agents living in Stuttgart)
REG_STUTTGART AS
	(SELECT SIM_TRIPS_ENRICHED.RUN_NAME,
			'Reg Stuttgart' RES_GROUP,
			COUNT(SIM_TRIPS_ENRICHED.TRIP_ID) AS TRIPS,
	 	 	AVG(SIM_TRIPS_ENRICHED.TRAVELED_DISTANCE) AVG_TRIP_DIST,
	 		AVG(SIM_TRIPS_ENRICHED.TRAV_TIME) AVG_TRIP_DUR
		FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
		WHERE (SIM_TRIPS_ENRICHED.AREA = 'Region Stuttgart ohne LH Stuttgart')
		GROUP BY SIM_TRIPS_ENRICHED.RUN_NAME),
			
-- Aggregate trips for agents living in either Boeblingen or Esslingen
-- For complementary group, take only residents of greater Stuttgart area into account
BOEB_ESSL AS
	(SELECT SIM_TRIPS_ENRICHED.RUN_NAME,
			CASE 
	 			WHEN SIM_TRIPS_ENRICHED.RES_BOEBL_ESSL = 1 THEN 'LK Boeb & Essl'
	 			ELSE 'Außer LK Boeb & Essl'
	 		END AS RES_GROUP,
			COUNT(SIM_TRIPS_ENRICHED.TRIP_ID) AS TRIPS,
	 		AVG(SIM_TRIPS_ENRICHED.TRAVELED_DISTANCE) AVG_TRIP_DIST,
	 		AVG(SIM_TRIPS_ENRICHED.TRAV_TIME) AVG_TRIP_DUR
		FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
		WHERE SIM_TRIPS_ENRICHED.SUBPOP = 'region_stuttgart'
		GROUP BY SIM_TRIPS_ENRICHED.RUN_NAME,
			SIM_TRIPS_ENRICHED.RES_BOEBL_ESSL),
			
			
-- Aggregate trips for agents living in focus areas of Boeblingen and Esslingen county
-- For complementary group, take only residents of greater Stuttgart area into account
FOCUS_AREAS AS
	(SELECT SIM_TRIPS_ENRICHED.RUN_NAME,
			CASE 
	 			WHEN SIM_TRIPS_ENRICHED.RES_FOCUS_AREAS = 1 THEN 'Fokusgemeinden'
	 			ELSE 'Außer Fokusgemeinden'
	 		END AS RES_GROUP,
			COUNT(SIM_TRIPS_ENRICHED.TRIP_ID) AS TRIPS,
	 		AVG(SIM_TRIPS_ENRICHED.TRAVELED_DISTANCE) AVG_TRIP_DIST,
	 		AVG(SIM_TRIPS_ENRICHED.TRAV_TIME) AVG_TRIP_DUR
		FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
		WHERE SIM_TRIPS_ENRICHED.SUBPOP = 'region_stuttgart'
		GROUP BY SIM_TRIPS_ENRICHED.RUN_NAME,
			SIM_TRIPS_ENRICHED.RES_FOCUS_AREAS),
			

-- Union measure groups
M_GROUPS AS (
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
	) AS UN

	ORDER BY RUN_NAME, RES_GROUP
)


-- Build final table
-- Calculate diffs and changes
SELECT
	M.RUN_NAME,
	M.RES_GROUP,
	BC.TRIPS TRIPS_BC,
	M.TRIPS TRIPS_M,
	(M.TRIPS - BC.TRIPS) AS TRIPS_DIFF,
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
ON BC.RES_GROUP = M.RES_GROUP
WHERE BC.RUN_NAME = 'bc'
			