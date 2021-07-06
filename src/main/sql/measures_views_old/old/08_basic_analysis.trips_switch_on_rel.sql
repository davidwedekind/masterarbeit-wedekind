-- basic_analysis.trips_switch_on_rel

-- Aggregate switches to switch types n bc modes to m measure modes
-- for the Boeblingen-Esslingen county relation
-- Find the share or each switch type
-- This is the basic analysis for the mode switch sankey diagram

-- @author dwedekind


SELECT RUN_NAME,
	FROM_BC_MODE,
	TO_M_MODE,
	COUNT(TRIP_ID) SWITCHES_ABS,
	COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME) AS SWITCHES_REL
	
FROM BASIC_ANALYSIS.TRIPS_SWITCH CMP

-- Filter on trips of relation Boeblingen-Esslingen only by using the county ags
WHERE (START_KREIS_AGS = '08115'
							AND END_KREIS_AGS = '08116')
	OR (START_KREIS_AGS = '08116'
					AND END_KREIS_AGS = '08115')
					
GROUP BY RUN_NAME,
	FROM_BC_MODE,
	TO_M_MODE