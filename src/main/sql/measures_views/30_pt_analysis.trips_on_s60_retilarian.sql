-- pt_analysis.pt_trips_on_s60_retilarian

-- Prepare base for s60 retilarian qgis view

-- @author dwedekind


SELECT RUN_NAME,
	"linkId" LINK_ID,
	GEOMETRY,
	COUNT(TRIP_ID)*{**sfactor**} NO_TRIPS

FROM PT_ANALYSIS.TRIPS_ON_S60_LINKS

GROUP BY RUN_NAME, LINK_ID, GEOMETRY
