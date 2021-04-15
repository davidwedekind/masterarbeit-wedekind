-- ae_analysis.ae_basic

-- Provide basic indicators of access and egress legs per mode and run

-- @author dwedekind


SELECT AE_LEGS.RUN_NAME,
	AE_LEGS.TRIP_CAL_MAIN_MODE,
	AVG(AE_LEGS.DISTANCE) AS AVG
	
FROM AE_ANALYSIS.AE_LEGS

GROUP BY AE_LEGS.RUN_NAME,
	AE_LEGS.TRIP_CAL_MAIN_MODE