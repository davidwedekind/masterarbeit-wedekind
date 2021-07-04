-- pt_analysis.trip_stats_on_s60_new
-- Provide trip counts for s60 usage on new section

-- @author dwedekind

SELECT 
	L1.RUN_NAME,
	L1.NO_TRIPS + L2.NO_TRIPS NO_TRIPS
FROM PT_ANALYSIS.TRIP_STATS_ON_S60_LINKS L1
LEFT JOIN PT_ANALYSIS.TRIP_STATS_ON_S60_LINKS L2
ON L1.RUN_NAME = L2.RUN_NAME
WHERE L1.LINK_ID = 'trNew0001'
AND L2.LINK_ID = 'trNew0002'
