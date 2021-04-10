-- cmp.cmp_trip_stats_surroundings_rebased

-- Compare simulation trip stats to mid values
-- (Region Stuttgart - Stuttgart surroundings overall values)

-- @author dwedekind

-- Count the number of trips for each area beforehand
-- Then determine rebased mid trips based on the original mid mode share
-- and the number of trips in simulation for each area
WITH SUMS AS
	(SELECT RUN_NAME
			,AREA
			,SUM(SIM_TRIPS_ABS_SCALED_100) TRIP_SUM_SCALED_100
		FROM MATSIM_OUTPUT.SIM_TRIP_STATS
		WHERE AREA = 'Region Stuttgart ohne LH Stuttgart'
		GROUP BY RUN_NAME,
			AREA)
			
SELECT SIM.RUN_NAME
	,CAL.AREA
	,CAL.MATSIM_MAIN_MODE
	,SIM.SIM_TRIPS_ABS
	,SIM.SIM_TRIPS_ABS_SCALED_100
	,SIM_TRIPS_ABS / SUM(SIM_TRIPS_ABS) OVER (PARTITION BY SIM.RUN_NAME) AS SIM_MODE_SHARE
	,CAL.MID_MODE_SHARE
	
	-- Determine rebased absolute number of trips here
	,(CAL.MID_MODE_SHARE * SUMS.TRIP_SUM_SCALED_100) / 100 MID_TRIPS_ABS
	
FROM MATSIM_OUTPUT.SIM_TRIP_STATS SIM
JOIN CAL.MID_TRIP_STATS_MULTIPLE_LEVEL CAL ON (SIM.AREA = CAL.AREA AND SIM.MATSIM_MAIN_MODE = CAL.MATSIM_MAIN_MODE)
JOIN SUMS ON (SIM.AREA = SUMS.AREA AND SIM.RUN_NAME = SUMS.RUN_NAME)

ORDER BY SIM.RUN_NAME,
	SIM.MATSIM_MAIN_MODE