-- matsim_output.sim_oev_segments

-- Determine the usage of the different pt submodes
-- namely ssb (Stuttgarter Straßenbahnen), sbahn (S-Bahn Stuttgart), bus, dbregio (for simplification all other)

-- @author dwedekind

-- Filter legs to legs within the vvs area only
WITH LEGS_WITHIN_VVS AS
	(SELECT LEGS.*
		FROM MATSIM_OUTPUT.SIM_LEGS_RAW LEGS
		INNER JOIN RAW.AREAS VVS ON ST_WITHIN(LEGS.GEOMETRY,VVS.GEOMETRY)
		WHERE VVS.SUBPOP = 'vvs_area' ),
	
	-- Count by trip
	PT_GROUP_PER_TRIP AS
	(SELECT RUN_NAME
			,TRIP_ID
			,PT_GROUP
			,COUNT(INDEX) COUNTER
		FROM LEGS_WITHIN_VVS
		WHERE MODE = 'pt'
		GROUP BY RUN_NAME,
			TRIP_ID,
			PT_GROUP)

-- Count total
SELECT RUN_NAME
	,PT_GROUP
	,COUNT(TRIP_ID) SGMT_ABS
	,COUNT(TRIP_ID) / SUM(COUNT(TRIP_ID)) OVER (PARTITION BY RUN_NAME) AS SGMT_REL
FROM PT_GROUP_PER_TRIP
GROUP BY RUN_NAME,
	PT_GROUP