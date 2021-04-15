-- basic_analysis.trips_switch_agg_by_res

-- Aggregate switches to switch types n bc modes to m measure modes for each area
-- Find the share or each switch type
-- This is the basic analysis for the mode switch sankey diagram

-- @author dwedekind


-- Aggregate trips for all people living in the greater Stuttgart area

SELECT RUN_NAME,
	AREA,
	FROM_BC_MODE,
	TO_M_MODE,
	COUNT(TRIP_ID) SWITCHES_ABS,
	COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME, AREA) AS SWITCHES_REL
	
FROM BASIC_ANALYSIS.TRIPS_SWITCH CMP
GROUP BY RUN_NAME,
	RES_GROUP,
	FROM_BC_MODE,
	TO_M_MODE