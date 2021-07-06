-- matsim_output.parkings_enriched

-- Enrich the parking.csv data with some network information
-- where the parking occurs

-- @author dwedekind


SELECT P.*,
	G.AGS GEM_AGS,
	G.GEN GEM_GEN,
	K.AGS KRS_AGS,
	K.GEN KRS_GEN,
	
	-- Identify parkings occurring in Boeblingen or Esslingen county
	CASE 
		WHEN K.AGS IN ('08115', '08116') THEN 1
		ELSE 0
	END AS P_BOEBL_ESSL,
	
	-- Identify parkings occurring in focus areas of Boeblingen or Esslingen county
	CASE 
		WHEN G.AGS IN ('08115003', '08115045', '08116078', '08116077') THEN 1
		ELSE 0
	END AS P_FOCUS_AREAS,
	
	N.ZONE_NAME,
	N.ZONE_GROUP,
	N."Length" LINK_LENGTH,
	N.GEOMETRY
	
FROM MATSIM_OUTPUT.PARKINGS P
LEFT JOIN MATSIM_OUTPUT.NETWORK N ON ((P."linkId" = N."Id") 
	AND (P."run_name" = N."run_name"))
	
-- Find out in which county/ community how many parkings occur via spatial join
-- Condition: Center of link has to be within the community geometry to get this community assigned
LEFT JOIN RAW.GEMEINDEN G ON ST_WITHIN(ST_CENTROID(N.GEOMETRY), G.GEOMETRY)
LEFT JOIN RAW.KREISE K ON ST_WITHIN(ST_CENTROID(N.GEOMETRY), K.GEOMETRY)