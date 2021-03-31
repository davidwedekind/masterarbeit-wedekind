SELECT
	p.run_name,
	p."parkingType",
	g.ags,
	g.gen,
	COUNT(p."parkingId") parkings,
	SUM(p."parkingFee") parking_revenues,
	AVG(p."parkingDuration") avg_parking_duration
FROM matsim_output.parkings_enriched p
JOIN raw.gemeinden g
ON st_within(st_centroid(p.geometry), g.geometry)
GROUP BY p.run_name, g.ags, g.gen, p."parkingType"