-- parking_analysis.car_usage_res

-- Find out how many agents are at least using car once per simulation run
-- This could be an indicator of car ownership

-- @author dwedekind


-- First, select all trips and mark those trips of mode car
-- with the integer value of 1 in the car_usage field
WITH FST AS
	(SELECT *,
		CASE
			WHEN MATSIM_CAL_MAIN_MODE = 'car' THEN 1
			ELSE 0
		END AS CAR_USAGE
		FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED),
		
	
-- Second, sum up the field car_usage for each person
SND AS(
	SELECT RUN_NAME,
		PERSON,
		CASE
			WHEN SUM(CAR_USAGE) > 0 THEN 1
			ELSE 0
		END AS CAR_USAGE
	FROM FST

	GROUP BY RUN_NAME,
	PERSON
),

-- Pull all run_name - person combinations with person attributes (needed for later calculation)
PRS AS(
	SELECT FOO_1.*,
		FOO_2.RES_REL_BOEBL_ESSL,
		FOO_2.RES_REL_FOCUS_AREAS
	FROM (
		SELECT R.RUN_NAME,
			P.SUBPOP,
			P.PERSON_ID PERSON,
			P.KRS_AGS,
			P.GEM_AGS,
			P.CALIB_GROUP AREA
		FROM (SELECT DISTINCT(RUN_NAME) FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
		) R
		CROSS JOIN MATSIM_INPUT.SIM_AGENTS_ENRICHED P
	) AS FOO_1
	LEFT JOIN (
		SELECT RUN_NAME,
			PERSON,
			RES_REL_BOEBL_ESSL,
			RES_REL_FOCUS_AREAS
		FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
		GROUP BY RUN_NAME, PERSON, RES_REL_BOEBL_ESSL, RES_REL_FOCUS_AREAS
	) AS FOO_2
	ON FOO_1.RUN_NAME = FOO_2.RUN_NAME
	AND FOO_1.PERSON = FOO_2.PERSON
),


-- Now, aggregate car usage for agents living the greater Stuttgart area
REG_STUTTGART_COMPLETE AS(
	SELECT PRS.RUN_NAME,
		'Reg Stuttgart mit LH Stuttgart' RES_GROUP,
		SUM(SND.CAR_USAGE) CAR_USAGE,
		COUNT(PRS.PERSON)-SUM(SND.CAR_USAGE) NO_CAR_USAGE,
		COUNT(PRS.PERSON) TOTAL_TRAVELERS
	FROM PRS
	LEFT JOIN SND
	ON PRS.RUN_NAME = SND.RUN_NAME
	AND PRS.PERSON = SND.PERSON
	WHERE PRS.SUBPOP = 'region_stuttgart'
	
	GROUP BY PRS.RUN_NAME
),

-- Aggregate car usage for agents living in 'LH Stuttgart'
LH_STUTTGART AS(
	SELECT PRS.RUN_NAME,
		'LH Stuttgart' RES_GROUP,
		SUM(SND.CAR_USAGE) CAR_USAGE,
		COUNT(PRS.PERSON)-SUM(SND.CAR_USAGE) NO_CAR_USAGE,
		COUNT(PRS.PERSON) TOTAL_TRAVELERS
	FROM PRS
	LEFT JOIN SND
	ON PRS.RUN_NAME = SND.RUN_NAME
	AND PRS.PERSON = SND.PERSON
	WHERE PRS.KRS_AGS = '08111'
	
	GROUP BY PRS.RUN_NAME
),

-- Aggregate car usage for agents living in Region Stuttgart (without Agents living in Stuttgart)
REG_STUTTGART AS(
	SELECT PRS.RUN_NAME,
		'Reg Stuttgart' RES_GROUP,
		SUM(SND.CAR_USAGE) CAR_USAGE,
		COUNT(PRS.PERSON)-SUM(SND.CAR_USAGE) NO_CAR_USAGE,
		COUNT(PRS.PERSON) TOTAL_TRAVELERS
	FROM PRS
	LEFT JOIN SND
	ON PRS.RUN_NAME = SND.RUN_NAME
	AND PRS.PERSON = SND.PERSON
	WHERE PRS.AREA = 'Region Stuttgart ohne LH Stuttgart'
	
	GROUP BY PRS.RUN_NAME
),

-- Aggregate car usage for agents living in either Boeblingen or Esslingen
-- For complementary group, take only residents of greater Stuttgart area into account
BOEB_ESSL AS(
	SELECT PRS.RUN_NAME,
		CASE
			WHEN PRS.KRS_AGS IN ('08115', '08116') THEN 'LK Boeb & Essl'
			ELSE 'Außer LK Boeb & Essl'
		END AS RES_GROUP,
		SUM(SND.CAR_USAGE) CAR_USAGE,
		COUNT(PRS.PERSON)-SUM(SND.CAR_USAGE) NO_CAR_USAGE,
		COUNT(PRS.PERSON) TOTAL_TRAVELERS
	FROM PRS
	LEFT JOIN SND
	ON PRS.RUN_NAME = SND.RUN_NAME
	AND PRS.PERSON = SND.PERSON
	
	GROUP BY PRS.RUN_NAME, RES_GROUP
),

-- Aggregate car usage for agents living in focus areas of Boeblingen and Esslingen county
-- For complementary group, take only residents of greater Stuttgart area into account
FOCUS_AREAS AS(
	SELECT PRS.RUN_NAME,
		CASE
			WHEN GEM_AGS IN ('08115003', '08115045', '08116078', '08116077') THEN 'Fokusgemeinden'
			ELSE 'Außer Fokusgemeinden'
		END AS RES_GROUP,
		SUM(SND.CAR_USAGE) CAR_USAGE,
		COUNT(PRS.PERSON)-SUM(SND.CAR_USAGE) NO_CAR_USAGE,
		COUNT(PRS.PERSON) TOTAL_TRAVELERS
	FROM PRS
	LEFT JOIN SND
	ON PRS.RUN_NAME = SND.RUN_NAME
	AND PRS.PERSON = SND.PERSON
	
	GROUP BY PRS.RUN_NAME, RES_GROUP
),

-- Aggregate car usage for agents living anywhere but having at least one trip on relation Boebl - Essl
BOEB_ESSL_TRAV AS(
	SELECT PRS.RUN_NAME,
		CASE
			WHEN RES_REL_BOEBL_ESSL = 1 THEN 'Boebl - Essl Reisende'
			ELSE 'Nicht Boebl - Essl Reisende'
		END AS RES_GROUP,
		SUM(SND.CAR_USAGE) CAR_USAGE,
		COUNT(PRS.PERSON)-SUM(SND.CAR_USAGE) NO_CAR_USAGE,
		COUNT(PRS.PERSON) TOTAL_TRAVELERS
	FROM PRS
	LEFT JOIN SND
	ON PRS.RUN_NAME = SND.RUN_NAME
	AND PRS.PERSON = SND.PERSON
	
	GROUP BY PRS.RUN_NAME, RES_GROUP
),

-- Aggregate car usage for agents living anywhere but having at least one trip on the focus relations between Boebl - Essl
FOCUS_AREAS_TRAV AS(
	SELECT PRS.RUN_NAME,
		CASE
			WHEN RES_REL_FOCUS_AREAS = 1 THEN 'Fokusrelationen Reisende'
			ELSE 'Nicht Fokusrelationen Reisende'
		END AS RES_GROUP,
		SUM(SND.CAR_USAGE) CAR_USAGE,
		COUNT(PRS.PERSON)-SUM(SND.CAR_USAGE) NO_CAR_USAGE,
		COUNT(PRS.PERSON) TOTAL_TRAVELERS
	FROM PRS
	LEFT JOIN SND
	ON PRS.RUN_NAME = SND.RUN_NAME
	AND PRS.PERSON = SND.PERSON
	
	GROUP BY PRS.RUN_NAME, RES_GROUP
),


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
		UNION
		SELECT * FROM BOEB_ESSL_TRAV
		UNION
		SELECT * FROM FOCUS_AREAS_TRAV
	) AS UN

	ORDER BY RUN_NAME, RES_GROUP
)


-- Some final calculations
SELECT RUN_NAME,
	RES_GROUP,
	CAR_USAGE*{**sfactor**} CAR_USAGE,
	NO_CAR_USAGE*{**sfactor**} NO_CAR_USAGE,
	TOTAL_TRAVELERS*{**sfactor**} TOTAL_TRAVELERS,
	CAR_USAGE/TOTAL_TRAVELERS::FLOAT CAR_USAGE_SHARE
FROM M_GROUPS


