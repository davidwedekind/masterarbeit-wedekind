-- cmp.cmp_trip_stats_surroundings_rebased

-- Extract car legs for simulation to real-world driving-time comparison
-- The comparison is limited to trips with beeline completely within model area geometry only

-- @author dwedekind

SELECT LEGS.*
	
-- Limit the output to trips with beeline completely within model area by performing
-- geojoin with condition of the leg geometry to be completely within model area geometry 'stuttgart_umland'
-- Set the car-legs-only condition
FROM (MATSIM_OUTPUT.SIM_LEGS_RAW LEGS 
	  LEFT JOIN RAW.AREAS AR ON (ST_WITHIN(LEGS.GEOMETRY, AR.GEOMETRY)))
WHERE ((AR.SUBPOP = 'stuttgart_umland'::text) AND (LEGS.MODE = 'car'::text))