SELECT S.*,
	T.GEOMETRY
	
FROM BASIC_ANALYSIS.TRIPS_SWITCH S
LEFT JOIN MATSIM_OUTPUT.SIM_TRIPS_ENRICHED T
ON S.TRIP_ID = T.TRIP_ID
AND S.RUN_NAME = T. RUN_NAME

WHERE S.RUN_NAME = 'm2_1'
	AND S.FROM_BC_MODE = 'pt'
	AND S.TO_M_MODE != 'pt'
	AND ((S.START_KREIS_AGS = '08115' AND S.END_KREIS_AGS = '08116')
		OR (S.START_KREIS_AGS = '08116' AND S.END_KREIS_AGS = '08115'))