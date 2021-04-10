-- pt_analysis.pt_trips_on_s60

-- Provide spatial view on trips which include the usage of S60 between Boeblingen and Esslingen county.

-- @author dwedekind


WITH LINKS AS
	
	-- Enrich the person 2 link list with correct time formatting
	(SELECT *,
			(SPLIT_PART(TIME,':',1)::bigint * 24 * 60 + SPLIT_PART(TIME,':',2)::bigint * 60 + SPLIT_PART(TIME,':',3)::bigint) TM
		FROM MATSIM_OUTPUT.PERSON_2_LINK_LIST),
		
	-- 	
	RELEVANT AS
	(SELECT L.*,
			T.TRIP_ID,
			T.DEP_TIME,
	 
	 		-- Arrival time of column is not correctly calculated
	 		-- Overwrite with prompt calculation
			(T.DEP_TIME + T.TRAV_TIME) ARR_TIME
		FROM LINKS L
		JOIN
	 
	 		-- Select all persons that have 
			(SELECT RUN_NAME,
					"personId"
				FROM MATSIM_OUTPUT.PERSON_2_LINK_LIST
				WHERE "linkId" IN ('trNew0001', 'trNew0002')
				GROUP BY RUN_NAME,
					"personId") AS P 
	 	ON L."personId" = P."personId"
		AND L.RUN_NAME = P.RUN_NAME
		LEFT JOIN MATSIM_OUTPUT.SIM_TRIPS_ENRICHED T ON L.RUN_NAME = T.RUN_NAME
		AND L."personId" = T.PERSON)
		
		
		
SELECT R.RUN_NAME,
	R."linkId" LINK_ID,
	N.GEOMETRY,
	R."ptSubmode" PT_SUBMODE,
	R."personId" PERSON_ID,
	R."trip_id",
	R.TM,
	R.DEP_TIME,
	R.ARR_TIME
FROM RELEVANT R
LEFT JOIN MATSIM_OUTPUT.NETWORK N ON R.RUN_NAME = N.RUN_NAME
AND R."linkId" = N."Id"