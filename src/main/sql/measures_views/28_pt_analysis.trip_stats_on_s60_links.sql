-- pt_analysis.pt_trip_stats_on_s60_links

-- Provide trip stats for as s60 usage distinct for each link

-- @author dwedekind


SELECT FST.RUN_NAME,
	FST."linkId" LINK_ID,
	FST.GEOMETRY,
	COUNT(FST."id")*{**sfactor**} NO_TRIPS
	
FROM MATSIM_OUTPUT.PERSON_2_LINK_LIST_ENRICHED FST

WHERE FST."ptLine" = 'S 60 - 1'
		
GROUP BY FST.RUN_NAME, FST."linkId", FST.GEOMETRY
ORDER BY FST.RUN_NAME, FST."linkId"