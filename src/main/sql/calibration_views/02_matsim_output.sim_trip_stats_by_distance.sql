-- matsim_output.sim_trip_stats_by_distance

-- Count trips by distance group and area for each run
-- Calculate the group share

-- @author dwedekind

SELECT RUN_NAME,
	AREA,
	DISTANCE_GROUP_NO,
	DISTANCE_GROUP,
	COUNT(TRIP_ID) AS SIM_TRIPS_ABS,
	(COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME, AREA)) AS SIM_TRIPS_REL
FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED

-- Filter on trips of agents living in 'Region Stuttgart' only
WHERE (SUBPOP = 'region_stuttgart'::text)

GROUP BY RUN_NAME,
	AREA,
	DISTANCE_GROUP,
	DISTANCE_GROUP_NO
ORDER BY RUN_NAME,
	AREA,
	DISTANCE_GROUP_NO