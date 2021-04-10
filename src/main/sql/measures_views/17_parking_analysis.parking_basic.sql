-- parking_analysis.parking_basic

-- Provide basic indicators for parking

-- @author dwedekind


SELECT P.RUN_NAME,
	P."parkingType",
	G.AGS,
	G.GEN,
	COUNT(P."parkingId") PARKINGS,
	SUM(P."parkingFee") PARKING_REVENUES,
	AVG(P."parkingDuration") AVG_PARKING_DURATION

-- Find out in which community how many parkings occur via spatial join
-- Condition: Center of link has to be within the community geometry to get this community assigned
FROM MATSIM_OUTPUT.PARKINGS_ENRICHED P
JOIN RAW.GEMEINDEN G ON ST_WITHIN(ST_CENTROID(P.GEOMETRY), G.GEOMETRY)

GROUP BY P.RUN_NAME,
	G.AGS,
	G.GEN,
	P."parkingType"