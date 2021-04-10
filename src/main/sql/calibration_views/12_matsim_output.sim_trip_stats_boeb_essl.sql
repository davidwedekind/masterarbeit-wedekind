-- matsim_output.sim_trip_stats_boeb_essl

-- Aggregate trips on relation by mode for each run

-- @author dwedekind

SELECT RUN_NAME,
	MATSIM_CAL_MAIN_MODE MATSIM_MAIN_MODE,
	COUNT(TRIP_ID) AS SIM_TRIPS_ABS,
	
	-- **sfactor** is placeholder value for the model scaling factor
	-- e.g. 10pct-model to bring values to 100pct => sfactor = 10
	COUNT(TRIP_ID)*{**sfactor**} SIM_TRIPS_ABS_SCALED_100,
	(COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME)) AS SIM_MODE_SHARE
	
FROM MATSIM_OUTPUT.SIM_TRIPS_ON_BOEB_ESSL_REL

GROUP BY RUN_NAME,
	MATSIM_CAL_MAIN_MODE
ORDER BY RUN_NAME,
	MATSIM_CAL_MAIN_MODE