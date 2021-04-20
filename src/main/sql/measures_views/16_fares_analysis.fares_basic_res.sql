-- fares_analysis.fares_basic_res

-- Provide basic fares idicators for each residents group

-- @author dwedekind


-- First, aggregate pt trip data by person
-- This gives us aggregate findings of a persons daily pt trips
WITH TRIPS AS
	(SELECT T.RUN_NAME,
			T.PERSON,
			SUM(T.TRAVELED_DISTANCE) / 1000 TOTAL_PT_KM_DIST,
			(SUM(T.TRAV_TIME) / 3600) TOTAL_PT_H_DUR
		FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED T
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
	AND F."run_name" = T.RUN_NAME), 
	
-- Aggregate trips for all people living in the greater Stuttgart area
REG_STUTTGART_COMPLETE AS(
	SELECT P.RUN_NAME,
		'Reg Stuttgart mit LH Stuttgart' RES_GROUP,
		AVG(P."noZones") AVG_NO_ZONES_TRAV,
		AVG(P."fareAmount") AVG_FARE_AMT_PAID,
		AVG(P."fareAmount") / AVG("noZones") AVG_FARE_AMT_PER_ZONE_PAID,
		AVG(P.TOTAL_PT_KM_DIST) AVG_TOTAL_PT_KM_DIST,
		AVG(P.EUR_PER_PT_KM) AVG_EUR_PER_PT_KM,
		AVG(P.TOTAL_PT_H_DUR) AVG_TOTAL_PT_H_DUR,
		AVG(P.EUR_PER_PT_H) AVG_EUR_PER_PT_H
	FROM PT_DATA P
	INNER JOIN (SELECT RUN_NAME, 
					PERSON 
				FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
				WHERE SUBPOP = 'region_stuttgart'
				GROUP BY RUN_NAME, PERSON							
	) AS FOO
	ON P.RUN_NAME = FOO.RUN_NAME AND P.PERSON = FOO.PERSON
	GROUP BY P.RUN_NAME
),

-- Aggregate trips for group 'LH Stuttgart'
LH_STUTTGART AS(
	SELECT P.RUN_NAME,
		'LH Stuttgart' RES_GROUP,
		AVG(P."noZones") AVG_NO_ZONES_TRAV,
		AVG(P."fareAmount") AVG_FARE_AMT_PAID,
		AVG(P."fareAmount") / AVG("noZones") AVG_FARE_AMT_PER_ZONE_PAID,
		AVG(P.TOTAL_PT_KM_DIST) AVG_TOTAL_PT_KM_DIST,
		AVG(P.EUR_PER_PT_KM) AVG_EUR_PER_PT_KM,
		AVG(P.TOTAL_PT_H_DUR) AVG_TOTAL_PT_H_DUR,
		AVG(P.EUR_PER_PT_H) AVG_EUR_PER_PT_H
	FROM PT_DATA P
	INNER JOIN (SELECT RUN_NAME, 
					PERSON 
				FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
				WHERE AGENT_HOME_KRS_AGS = '08111'
				GROUP BY RUN_NAME, PERSON		
	) AS FOO
	ON P.RUN_NAME = FOO.RUN_NAME AND P.PERSON = FOO.PERSON
	GROUP BY P.RUN_NAME
),

-- Aggregate trips for Region Stuttgart (without Agents living in Stuttgart)
REG_STUTTGART AS(
	SELECT P.RUN_NAME,
		'Reg Stuttgart' RES_GROUP,
		AVG(P."noZones") AVG_NO_ZONES_TRAV,
		AVG(P."fareAmount") AVG_FARE_AMT_PAID,
		AVG(P."fareAmount") / AVG("noZones") AVG_FARE_AMT_PER_ZONE_PAID,
		AVG(P.TOTAL_PT_KM_DIST) AVG_TOTAL_PT_KM_DIST,
		AVG(P.EUR_PER_PT_KM) AVG_EUR_PER_PT_KM,
		AVG(P.TOTAL_PT_H_DUR) AVG_TOTAL_PT_H_DUR,
		AVG(P.EUR_PER_PT_H) AVG_EUR_PER_PT_H
	FROM PT_DATA P
	INNER JOIN (SELECT RUN_NAME, 
					PERSON 
				FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
				WHERE AREA = 'Region Stuttgart ohne LH Stuttgart'
				GROUP BY RUN_NAME, PERSON
	) AS FOO
	ON P.RUN_NAME = FOO.RUN_NAME AND P.PERSON = FOO.PERSON
	GROUP BY P.RUN_NAME
),

-- Aggregate trips for agents living in either Boeblingen or Esslingen
-- For complementary group, take only residents of greater Stuttgart area into account
BOEB_ESSL AS(
	SELECT P.RUN_NAME,
		CASE 
			WHEN FOO.RES_BOEBL_ESSL = 1 THEN 'LK Boeb & Essl'
			ELSE 'Außer LK Boeb & Essl'
		END AS RES_GROUP,
		AVG(P."noZones") AVG_NO_ZONES_TRAV,
		AVG(P."fareAmount") AVG_FARE_AMT_PAID,
		AVG(P."fareAmount") / AVG("noZones") AVG_FARE_AMT_PER_ZONE_PAID,
		AVG(P.TOTAL_PT_KM_DIST) AVG_TOTAL_PT_KM_DIST,
		AVG(P.EUR_PER_PT_KM) AVG_EUR_PER_PT_KM,
		AVG(P.TOTAL_PT_H_DUR) AVG_TOTAL_PT_H_DUR,
		AVG(P.EUR_PER_PT_H) AVG_EUR_PER_PT_H
	FROM PT_DATA P
	INNER JOIN (SELECT RUN_NAME,
					RES_BOEBL_ESSL,
					PERSON 
				FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
				GROUP BY RUN_NAME, RES_BOEBL_ESSL, PERSON
	) AS FOO
	ON P.RUN_NAME = FOO.RUN_NAME AND P.PERSON = FOO.PERSON
	GROUP BY P.RUN_NAME, FOO.RES_BOEBL_ESSL
),

-- Aggregate trips for agents living in focus areas of Boeblingen and Esslingen county
-- For complementary group, take only residents of greater Stuttgart area into account
FOCUS_AREAS AS(
	SELECT P.RUN_NAME,
		CASE 
			WHEN FOO.RES_FOCUS_AREAS = 1 THEN 'Fokusgemeinden'
			ELSE 'Außer Fokusgemeinden'
		END AS RES_GROUP,
		AVG(P."noZones") AVG_NO_ZONES_TRAV,
		AVG(P."fareAmount") AVG_FARE_AMT_PAID,
		AVG(P."fareAmount") / AVG("noZones") AVG_FARE_AMT_PER_ZONE_PAID,
		AVG(P.TOTAL_PT_KM_DIST) AVG_TOTAL_PT_KM_DIST,
		AVG(P.EUR_PER_PT_KM) AVG_EUR_PER_PT_KM,
		AVG(P.TOTAL_PT_H_DUR) AVG_TOTAL_PT_H_DUR,
		AVG(P.EUR_PER_PT_H) AVG_EUR_PER_PT_H
	FROM PT_DATA P
	INNER JOIN (SELECT RUN_NAME,
					RES_FOCUS_AREAS,
					PERSON 
				FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
				GROUP BY RUN_NAME, RES_FOCUS_AREAS, PERSON
	) AS FOO
	ON P.RUN_NAME = FOO.RUN_NAME AND P.PERSON = FOO.PERSON
	GROUP BY P.RUN_NAME, FOO.RES_FOCUS_AREAS
),

-- Aggregate trips for agents living anywhere but having at least one trip on relation Boebl - Essl
BOEB_ESSL_TRAV AS (
	SELECT P.RUN_NAME,
		CASE 
			WHEN FOO.RES_REL_BOEBL_ESSL = 1 THEN 'Boebl - Essl Reisende'
			ELSE 'Nicht Boebl - Essl Reisende'
		END AS RES_GROUP,
		AVG(P."noZones") AVG_NO_ZONES_TRAV,
		AVG(P."fareAmount") AVG_FARE_AMT_PAID,
		AVG(P."fareAmount") / AVG("noZones") AVG_FARE_AMT_PER_ZONE_PAID,
		AVG(P.TOTAL_PT_KM_DIST) AVG_TOTAL_PT_KM_DIST,
		AVG(P.EUR_PER_PT_KM) AVG_EUR_PER_PT_KM,
		AVG(P.TOTAL_PT_H_DUR) AVG_TOTAL_PT_H_DUR,
		AVG(P.EUR_PER_PT_H) AVG_EUR_PER_PT_H
	FROM PT_DATA P
	INNER JOIN (SELECT RUN_NAME,
					RES_REL_BOEBL_ESSL,
					PERSON 
				FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
				GROUP BY RUN_NAME, RES_REL_BOEBL_ESSL, PERSON
	) AS FOO
	ON P.RUN_NAME = FOO.RUN_NAME AND P.PERSON = FOO.PERSON
	GROUP BY P.RUN_NAME, FOO.RES_REL_BOEBL_ESSL
),

-- Aggregate trips for agents living anywhere but having at least one trip on relation between focus areas
FOCUS_AREAS_TRAV AS (
	SELECT P.RUN_NAME,
		CASE 
			WHEN FOO.RES_REL_FOCUS_AREAS = 1 THEN 'Fokusrelationen Reisende'
			ELSE 'Nicht Fokusrelationen Reisende'
		END AS RES_GROUP,
		AVG(P."noZones") AVG_NO_ZONES_TRAV,
		AVG(P."fareAmount") AVG_FARE_AMT_PAID,
		AVG(P."fareAmount") / AVG("noZones") AVG_FARE_AMT_PER_ZONE_PAID,
		AVG(P.TOTAL_PT_KM_DIST) AVG_TOTAL_PT_KM_DIST,
		AVG(P.EUR_PER_PT_KM) AVG_EUR_PER_PT_KM,
		AVG(P.TOTAL_PT_H_DUR) AVG_TOTAL_PT_H_DUR,
		AVG(P.EUR_PER_PT_H) AVG_EUR_PER_PT_H
	FROM PT_DATA P
	INNER JOIN (SELECT RUN_NAME,
					RES_REL_FOCUS_AREAS,
					PERSON 
				FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
				GROUP BY RUN_NAME, RES_REL_FOCUS_AREAS, PERSON
	) AS FOO
	ON P.RUN_NAME = FOO.RUN_NAME AND P.PERSON = FOO.PERSON
	GROUP BY P.RUN_NAME, FOO.RES_REL_FOCUS_AREAS
),


-- Final union
M_GROUPS AS(
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
)


-- Some final calculations
SELECT RUN_NAME,
	RES_GROUP,
	AVG_NO_ZONES_TRAV,
	AVG_FARE_AMT_PAID*(-1) AVG_FARE_AMT_PAID,
	AVG_FARE_AMT_PER_ZONE_PAID*(-1) AVG_FARE_AMT_PER_ZONE_PAID,
	AVG_TOTAL_PT_KM_DIST,
	AVG_EUR_PER_PT_KM*(-1) AVG_EUR_PER_PT_KM,
	AVG_TOTAL_PT_H_DUR,
	AVG_EUR_PER_PT_H*(-1) AVG_EUR_PER_PT_H
FROM M_GROUPS
ORDER BY RUN_NAME, RES_GROUP