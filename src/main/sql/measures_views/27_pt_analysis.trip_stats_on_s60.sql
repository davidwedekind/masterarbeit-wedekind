-- pt_analysis.pt_trip_stats_on_s60

-- Provide trip counts for s60 usage

-- @author dwedekind

WITH BASE AS(

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
)

SELECT
	B1.*,
	B2.no_trips no_trips_bc,
	(B1.no_trips-B2.no_trips)/B2.no_trips::float trips_change
FROM BASE B1
CROSS JOIN BASE B2
WHERE B2.RUN_NAME = 'bc'
ORDER BY B1.RUN_NAME
