-- matsim_output.parkings_enriched

-- Enrich the parking.csv data with some network information
-- where the parking occurs

-- @author dwedekind


SELECT P.*,
	N.ZONE_NAME,
	N.ZONE_GROUP,
	N."Length" LINK_LENGTH,
	N.GEOMETRY
	
FROM MATSIM_OUTPUT.PARKINGS P
LEFT JOIN MATSIM_OUTPUT.NETWORK N ON ((P."linkId" = N."Id") 
	AND (P."run_name" = N."run_name"))