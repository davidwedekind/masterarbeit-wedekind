-- matsim_output.sim_trip_stats_by_mode_distance

-- Count trips in simulation run_name, area, matsim_cal_main_mode and distance_group
-- Calculate the scaled 100pct values

-- @author dwedekind

SELECT RUN_NAME
	,AREA
	,DISTANCE_GROUP
	,DISTANCE_GROUP_NO
	,MATSIM_CAL_MAIN_MODE MATSIM_MAIN_MODE
	,COUNT(TRIP_ID) SIM_TRIPS_ABS
	
	-- **sfactor** is placeholder value for the model scaling factor
	-- e.g. 10pct-model to bring values to 100pct => sfactor = 10
	,COUNT(TRIP_ID)*{**sfactor**} SIM_TRIPS_ABS_SCALED_100
	,(COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME, AREA, MATSIM_CAL_MAIN_MODE)) AS SIM_MODE_SHARE_GB_MODE
	,(COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME, AREA, DISTANCE_GROUP)) AS SIM_MODE_SHARE_GB_DGROUP
	
FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED

-- Filter on trips of agents living in 'Region Stuttgart' only
WHERE (SUBPOP = 'region_stuttgart'::TEXT)

-- Aggregation to trips by area, mode and distance_group for each run
GROUP BY RUN_NAME,
	AREA,
	DISTANCE_GROUP,
	DISTANCE_GROUP_NO,
	MATSIM_CAL_MAIN_MODE
ORDER BY RUN_NAME,
	AREA,
	DISTANCE_GROUP,
	DISTANCE_GROUP_NO,
	MATSIM_CAL_MAIN_MODE