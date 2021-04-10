-- basic_analysis.od_analysis

-- Count trips for each community to community relation for each mode (and run)
-- Make no difference between directions, calculate the overall value

-- @author dwedekind


-- Define directional uniform start and end ags
WITH T1 AS
	(SELECT RUN_NAME,
			MATSIM_CAL_MAIN_MODE,
			CONCAT('0', (GREATEST(TRIM(LEADING '0' FROM START_GEM_AGS)::INTEGER,
								  TRIM(LEADING '0' FROM END_GEM_AGS)::INTEGER))::TEXT) START_GEM_AGS,
			CONCAT('0', (LEAST(TRIM(LEADING '0' FROM START_GEM_AGS)::INTEGER,
							   TRIM(LEADING '0' FROM END_GEM_AGS)::INTEGER))::TEXT) END_GEM_AGS,
			TRIP_ID
		FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED),
	
	-- Count trips for each uniform relation
	RELATIONS AS
	(SELECT RUN_NAME,
			MATSIM_CAL_MAIN_MODE,
			START_GEM_AGS,
			END_GEM_AGS,
			COUNT(TRIP_ID)
		FROM T1
	 
		GROUP BY RUN_NAME,
			START_GEM_AGS,
			END_GEM_AGS,
			MATSIM_CAL_MAIN_MODE)
			
			
-- Final select (mainly for adding the relation line string geometry)
SELECT R.*,
	GEM_1.GEN START_GEM_GEN,
	GEM_2.GEN END_GEM_GEN,
	
	-- Build trip line string for visualization purpose
	ST_MAKELINE(ST_CENTROID(GEM_1.GEOMETRY), ST_CENTROID(GEM_2.GEOMETRY)) GEOMETRY
	
FROM RELATIONS R
JOIN RAW.GEMEINDEN GEM_1 ON GEM_1.AGS = R.START_GEM_AGS
JOIN RAW.GEMEINDEN GEM_2 ON GEM_2.AGS = R.END_GEM_AGS