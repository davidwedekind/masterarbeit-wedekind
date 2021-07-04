-- ae_analysis.ae_basic_rel_v2

-- Provide basic indicators of access and egress legs per mode, run and relation
-- Relations are LK Boeblingen - LK Esslingen and between focus areas of Boeblingen and Esslingen

-- @author dwedekind

WITH LEGS AS (
	-- access legs
	(
		SELECT *,
			CASE
				WHEN START_GEM_AGS IN ('08115003', '08115045', '08116077', '08116078') THEN 'Fokusgemeinden'
				ELSE 'Außer Fokusgemeinden'
			END AS SE_FOKUS_RELEVANT
		FROM AE_ANALYSIS.AE_LEGS
		WHERE LEG_ID_ASC = 1
	)
	UNION

	-- egress legs
	(
		SELECT *,
			CASE
				WHEN END_GEM_AGS IN ('08115003', '08115045', '08116077', '08116078') THEN 'Fokusgemeinden'
				ELSE 'Außer Fokusgemeinden'
			END AS SE_FOKUS_RELEVANT
		FROM AE_ANALYSIS.AE_LEGS
		WHERE LEG_ID_DESC = 1
	)
)

SELECT
	RUN_NAME,
	SE_FOKUS_RELEVANT,
	TRIP_CAL_MAIN_MODE,
	AVG(DISTANCE)*1.27 AS AVG_DIST,
	AVG(TRAV_TIME) AS AVG_DUR
FROM LEGS
GROUP BY RUN_NAME, SE_FOKUS_RELEVANT, TRIP_CAL_MAIN_MODE