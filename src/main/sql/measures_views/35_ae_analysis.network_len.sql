-- ae_analysis.network_len

-- Network edge lengths
 
-- @author dwedekind
WITH network_fig AS
(
	SELECT
		n.run_name,
		sum(n."Length") len
	FROM
	matsim_output.network n
	LEFT JOIN raw.gemeinden gem
	ON ST_WITHIN(ST_CENTROID(n.geometry), gem.geometry)
	WHERE "Modes" = 'car,ride,walk,bike'
	AND ags IN ('08115003', '08115045', '08116077', '08116078')
	GROUP BY n.run_name
)

SELECT
	F1.*,
	(F1.LEN-F2.LEN)/F2.LEN::float LEN_CHANGE
FROM network_fig F1
CROSS JOIN network_fig F2
WHERE F2.RUN_NAME = 'bc'


