-- cal.mid_trip_stats_by_mode_distance_stuttgart_rebased

-- Calculate the calibration target number of trips by each mode and area
-- As the senozon model is missing trips, it cannot meet the original mid target values
-- Workaround: Meet only relative (mode share) values
-- and calculate the rebased absolute mid values based on trips existing in the model

-- @author dwedekind

SELECT SIM.RUN_NAME
	,CAL.AREA
	,CAL.DISTANCE_GROUP
	,CAL.DISTANCE_GROUP_NO
	,CAL.MATSIM_MAIN_MODE
	,CAL.MID_MODE_SHARE_GB_DGROUP
	,CAL.MID_TRIPS_ABS

	-- **sfactor** is placeholder value for the model scaling factor
	-- e.g. 10pct-model to bring values to 100pct => sfactor = 10
	,((CAL.MID_MODE_SHARE_GB_DGROUP * SIM.SIM_TRIPS_ABS) / 100) * {**sfactor**} MID_TRIPS_ABS_REBASED

FROM MATSIM_OUTPUT.SIM_TRIP_STATS_BY_DISTANCE SIM
INNER JOIN CAL.MID_TRIP_STATS_BY_MODE_DISTANCE_STUTTGART CAL ON CAL.DISTANCE_GROUP_NO = SIM.DISTANCE_GROUP_NO
	AND CAL.AREA = SIM.AREA

ORDER BY SIM.RUN_NAME
	,CAL.AREA
	,CAL.DISTANCE_GROUP_NO
	,CAL.MATSIM_MAIN_MODE
