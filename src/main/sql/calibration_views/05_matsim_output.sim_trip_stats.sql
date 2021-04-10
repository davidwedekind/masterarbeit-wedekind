-- matsim_output.sim_trip_stats

-- Count trips in simulation run_name, area and matsim_cal_main_mode
-- Calculate the scaled 100pct values
-- Calculate the simulation mode shares by run_name and area

-- @author dwedekind

SELECT RUN_NAME
	,AREA
	,MATSIM_CAL_MAIN_MODE MATSIM_MAIN_MODE
	,COUNT(TRIP_ID) AS SIM_TRIPS_ABS
	
	-- **sfactor** is placeholder value for the model scaling factor
	-- e.g. 10pct-model to bring values to 100pct => sfactor = 10
	,COUNT(TRIP_ID) * {**sfactor**} SIM_TRIPS_ABS_SCALED_100
	,(COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME, AREA)) AS SIM_MODE_SHARE
	
FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED

-- Filter on trips of agents living in 'Region Stuttgart' only
WHERE (SUBPOP = 'region_stuttgart'::TEXT)

-- Aggregation to trips by area and mode for each run
-- Do not break down on distance groups as this is the calucation for the overall mode counts
GROUP BY RUN_NAME
	,AREA
	,MATSIM_CAL_MAIN_MODE
ORDER BY RUN_NAME
	,AREA
	,MATSIM_CAL_MAIN_MODE
