-- basic_analysis.trips_switch_list

-- Join trip data of bc run to trip data of measure runs
-- Have data for each trip for each measure run in direct comparison to the base case
-- in a non-aggregate state (for later switch analysis)

-- @author dwedekind


SELECT M.RUN_NAME,
	BC.PERSON,
	BC.TRIP_ID,
	BC.AREA,
	BC.SUBPOP,
	BC.AGENT_HOME_KRS_AGS,
	BC.AGENT_HOME_KRS_GEN,
	BC.START_KREIS_AGS,
	BC.START_KREIS_GEN,
	BC.END_KREIS_AGS,
	BC.END_KREIS_GEN,
	BC.RES_BOEBL_ESSL,
	BC.RES_FOCUS_AREAS,
	BC.SE_BOEBL_ESSL,
	BC.SE_FOCUS_AREAS,
	BC.REL_BOEBL_ESSL,
	BC.REL_FOCUS_AREAS,
	BC.MATSIM_CAL_MAIN_MODE FROM_BC_MODE,
	M.MATSIM_CAL_MAIN_MODE TO_M_MODE
FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED_BC BC
FULL JOIN MATSIM_OUTPUT.SIM_TRIPS_ENRICHED_MEASURES M ON BC.TRIP_ID = M.TRIP_ID