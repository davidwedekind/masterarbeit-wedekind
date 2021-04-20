-- pt_analysis.pt_trip_stats_on_s60

-- Provide trip counts for s60 usage

-- @author dwedekind


SELECT SND.RUN_NAME,
	COUNT(SND.TRIP_ID)*{**sfactor**} no_trips
FROM (
	
	SELECT FST.RUN_NAME,
		FST.TRIP_ID

	FROM MATSIM_OUTPUT.PERSON_2_LINK_LIST_ENRICHED FST
	
	WHERE FST."ptLine" = 'S 60 - 1'
	
	GROUP BY FST.RUN_NAME, FST.TRIP_ID
	
) AS SND
		
GROUP BY SND.RUN_NAME
ORDER BY SND.RUN_NAME