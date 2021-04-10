-- ae_analysis.ae_oebl_essl

-- Provide indicators of access and egress legs per mode and run
-- specifically for trips on the relation of Esslingen - Boeblingen

-- @author dwedekind


SELECT AE_LEGS.RUN_NAME,
	AE_LEGS.TRIP_CAL_MAIN_MODE,
	AVG(AE_LEGS.DISTANCE) AS AVG
	
FROM AE_ANALYSIS.AE_LEGS

-- This is the filter to catch only access and egress legs of trips
-- on relation Boeblingen - Esslingen
WHERE (((AE_LEGS.START_KREIS_AGS = '08115'::text) AND (AE_LEGS.END_KREIS_AGS = '08116'::text))
							OR ((AE_LEGS.START_KREIS_AGS = '08116'::text) AND (AE_LEGS.END_KREIS_AGS = '08115'::text)))
											
GROUP BY AE_LEGS.RUN_NAME,
	AE_LEGS.TRIP_CAL_MAIN_MODE