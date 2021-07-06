-- pt_analysis.route_statistics_rel

-- Calculate route statistics on focus relations
 
-- @author dwedekind
SELECT
	n.run_name,
	gem.ags,
	gem.gen,
	sum(n."Length")
FROM
matsim_output.network n
LEFT JOIN raw.gemeinden gem
ON ST_WITHIN(ST_CENTROID(n.geometry), gem.geometry)
WHERE "Modes" = 'car,ride,walk,bike'
GROUP BY n.run_name, gem.ags, gem.gen