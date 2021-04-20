-- pt_analysis.pt_trips_on_s60

-- Provide spatial view on trips which include the usage of S60 between Boeblingen and Esslingen county.

-- @author dwedekind


-- Select all persons that use S60
WITH S60_TRAVELERS AS(
	SELECT RUN_NAME,
		"personId" PERSON,
		(SPLIT_PART(TIME,':',1)::bigint * 60 * 60 + SPLIT_PART(TIME,':',2)::bigint * 60 + SPLIT_PART(TIME,':',3)::bigint) TIME_ROHRER_KURVE
	FROM MATSIM_OUTPUT.PERSON_2_LINK_LIST

	-- Links 'trNew0001', 'trNew0002' are exclusively used by S60
	WHERE "linkId" IN ('trNew0001', 'trNew0002')

	GROUP BY RUN_NAME, PERSON, TIME_ROHRER_KURVE
)

SELECT S60.*,
	T.TRIP_ID,
	T.DEP_TIME,
	T.DEP_TIME + T.TRAV_TIME ARR_TIME
FROM S60_TRAVELERS S60
LEFT JOIN MATSIM_OUTPUT.SIM_TRIPS_ENRICHED T
ON S60.RUN_NAME = T.RUN_NAME
AND S60.PERSON = T.PERSON
AND (
	S60.TIME_ROHRER_KURVE <= ARR_TIME AND S60.TIME_ROHRER_KURVE >= T.DEP_TIME
)

