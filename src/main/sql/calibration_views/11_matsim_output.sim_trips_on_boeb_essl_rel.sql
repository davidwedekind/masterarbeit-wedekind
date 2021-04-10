-- matsim_output.sim_trips_on_boeb_essl_rel

-- Extract trips between the two relevant counties on tangential relation

-- @author dwedekind

SELECT T.*
	,S_KRS.AGS START_KREIS_AGS
	,S_KRS.GEN START_KREIS_GEN
	,E_KRS.AGS END_KREIS_AGS
	,E_KRS.GEN END_KREIS_GEN

-- At this point trips of agents all agents but not limited to living in 'Region Stuttgart' are relevant
-- Hence no filter condition subpop = 'stuttgart_umland'
FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED T

-- Double spatial join to county geometries in order to
-- filter trips on tangetial relation only
JOIN RAW.KREISE S_KRS ON ST_WITHIN(ST_SETSRID(ST_POINT(T.START_X,T.START_Y),25832),S_KRS.GEOMETRY)
JOIN RAW.KREISE E_KRS ON ST_WITHIN(ST_SETSRID(ST_POINT(T.END_X,T.END_Y),25832),E_KRS.GEOMETRY)
WHERE (S_KRS.AGS = '08115' AND E_KRS.AGS = '08116')
	OR (S_KRS.AGS = '08116' AND E_KRS.AGS = '08115')