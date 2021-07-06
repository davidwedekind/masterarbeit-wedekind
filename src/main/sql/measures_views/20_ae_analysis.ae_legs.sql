-- ae_analysis.ae_legs

-- Provide access and egress legs enriched with trip stats data

-- @author dwedekind


SELECT T.MATSIM_RAW_MAIN_MODE TRIP_RAW_MAIN_MODE,
	T.MATSIM_CAL_MAIN_MODE TRIP_CAL_MAIN_MODE,
	T.SUBPOP,
	T.AREA,
	T.AGENT_HOME_KRS_AGS,
	T.AGENT_HOME_KRS_GEN,
	T.AGENT_HOME_GEM_AGS,
	T.AGENT_HOME_GEM_GEN,
	T.START_KREIS_AGS,
	T.START_KREIS_GEN,
	T.END_KREIS_AGS,
	T.END_KREIS_GEN,
	T.START_GEM_AGS,
	T.START_GEM_GEN,
	T.END_GEM_AGS,
	T.END_GEM_GEN,
	T.RES_BOEBL_ESSL,
	T.RES_FOCUS_AREAS,
	T.SE_BOEBL_ESSL,
	T.SE_FOCUS_AREAS,
	T.REL_BOEBL_ESSL,
	T.REL_FOCUS_AREAS,
	T.RES_REL_BOEBL_ESSL,
	T.RES_REL_FOCUS_AREAS,
	L.*
	
FROM (
	
	-- Filter legs table to access and egress legs only
	-- Therefore, identified first and last leg of each trip via window function
	WITH LEG_WINDOWS AS
		(SELECT 
			ROW_NUMBER() OVER (PARTITION BY RUN_NAME, TRIP_ID ORDER BY DEP_TIME ASC) LEG_ID_ASC,
			ROW_NUMBER() OVER (PARTITION BY RUN_NAME, TRIP_ID ORDER BY DEP_TIME DESC) LEG_ID_DESC,
			*
		FROM MATSIM_OUTPUT.SIM_LEGS_RAW)

	-- Select first and last leg only of each trip
	SELECT *
	FROM LEG_WINDOWS
	WHERE (LEG_ID_ASC = 1 OR LEG_ID_DESC = 1)
	
) l

-- Join trip stats to access and egress legs
INNER JOIN matsim_output.sim_trips_enriched t
ON t.run_name = l.run_name AND t.trip_id = l.trip_id

-- Only 'pt', 'car' and 'ride' are relevant modes for this analysis
WHERE t.matsim_cal_main_mode IN ('pt', 'car', 'ride')
