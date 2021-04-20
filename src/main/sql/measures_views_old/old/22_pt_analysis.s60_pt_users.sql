-- pt_analysis.s60_pt_users

-- Count number of trips on s60 (between Goldberg and Leinfelden-Echterdingen Station)

-- @author dwedekind


SELECT RUN_NAME,
	COUNT(DISTINCT(TRIP_ID))
FROM PT_ANALYSIS.PT_TRIPS_ON_S60
GROUP BY RUN_NAME