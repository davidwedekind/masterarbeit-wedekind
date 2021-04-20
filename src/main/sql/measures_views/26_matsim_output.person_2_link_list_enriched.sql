-- matsim_output.person_2_link_list_enriched

-- Provide enriched view on person 2 link list
-- Add respective network, trip and leg info to persons travelling on links

-- @author dwedekind


SELECT P.*,
	N.GEOMETRY,
	T.TRIP_ID,
	T.DEP_TIME TRIP_DEP_TIME,
	T.DEP_TIME + T.TRAV_TIME TRIP_ARR_TIME,
	L.INDEX LEG_INDEX,
	L.DEP_TIME LEG_DEP_TIME,
	L.DEP_TIME + L.TRAV_TIME LEG_ARR_TIME,
	L.START_LINK LEG_START_LINK,
	L.END_LINK LEG_END_LINK
	
FROM (
	
	-- Reconvert time field into time in seconds
	SELECT *, 
		(SPLIT_PART(TIME,':',1)::bigint * 60 * 60 + SPLIT_PART(TIME,':',2)::bigint * 60 + SPLIT_PART(TIME,':',3)::bigint) TIME_S
	FROM MATSIM_OUTPUT.PERSON_2_LINK_LIST
) AS P

-- Join network information
LEFT JOIN MATSIM_OUTPUT.NETWORK N
ON N."Id" = P."linkId"
AND N.RUN_NAME = P.RUN_NAME

-- Join trip information
-- The only way to do that is to look whether time on link is within ongoing trip timewise
LEFT JOIN MATSIM_OUTPUT.SIM_TRIPS_ENRICHED T
ON P.RUN_NAME = T.RUN_NAME
AND P."personId" = T.PERSON
AND (
	P.TIME_S <= (T.DEP_TIME + T.TRAV_TIME) AND P.TIME_S >= T.DEP_TIME
)

-- Join leg information
-- The only way to do that is to look whether time on link is within ongoing leg timewise
LEFT JOIN MATSIM_OUTPUT.SIM_LEGS_RAW L
ON P.RUN_NAME = L.RUN_NAME
AND P."personId" = L.PERSON
AND (
	P.TIME_S <= (L.DEP_TIME + L.TRAV_TIME) AND P.TIME_S >= L.DEP_TIME
)
