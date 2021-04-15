WITH PT_RIDERS AS (
	
	-- Select persons that have pt trips on Boeblingen - Esslingen relation
	SELECT
		T.RUN_NAME,
		T.PERSON
	FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED T
	
	WHERE ((T.START_KREIS_AGS = '08115' AND T.END_KREIS_AGS = '08116')
			OR (T.START_KREIS_AGS = '08116' AND T.END_KREIS_AGS = '08115'))
	AND T.MATSIM_CAL_MAIN_MODE = 'pt'
	
	GROUP BY T.RUN_NAME, T.PERSON
),

-- Join fares data ...
FARES_ENRICHED AS(
	SELECT
		REL.RUN_NAME,
		REL.PERSON,
		FAR."outOfZones" OUT_OF_ZONES,
		FAR."noZones" NO_ZONES,
		FAR."fareAmount" FARE_AMOUNT,
		TRIP_STATS.TRAVELED_DISTANCE,
		TRIP_STATS.TRAV_TIME,
		TRIP_STATS.GEOMETRY

	FROM PT_RIDERS REL
	LEFT JOIN MATSIM_OUTPUT.PERSON_2_FARES FAR
	ON REL.RUN_NAME = FAR.RUN_NAME
	AND REL.PERSON = FAR."personId"
	
	-- ... and pt trip stats
	LEFT JOIN (
		SELECT RUN_NAME,
			PERSON,
			SUM(TRAVELED_DISTANCE) TRAVELED_DISTANCE,
			SUM(TRAV_TIME) TRAV_TIME,
			ST_COLLECT(GEOMETRY) GEOMETRY
		
		FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
		WHERE MATSIM_CAL_MAIN_MODE = 'pt'
		GROUP BY RUN_NAME, PERSON
	
	) AS TRIP_STATS
	ON REL.RUN_NAME = TRIP_STATS.RUN_NAME
	AND REL.PERSON = TRIP_STATS.PERSON
)


SELECT * FROM FARES_ENRICHED