-- br_analysis.stop_feed_radius

-- Create stop feed radia
 
-- @author dwedekind
(
	SELECT sf.*,
		5 AS feed_min,
		(bs.speed_value * 3.6 * (5/60::float) * 1000)/bs.beeline_dfactor AS feed_radius,
		st_buffer(sf.geometry, 1003::double precision) AS geom
	FROM matsim_output.stop_facilities sf
	LEFT JOIN matsim_output.bike_speed bs
	ON bs.run_name = sf.run_name
	WHERE sf.vvs_bike_ride = 'true'
)

UNION

(
	SELECT sf.*,
		10 AS feed_min,
		(bs.speed_value * 3.6 * (10/60::float) * 1000)/bs.beeline_dfactor AS feed_radius,
		st_buffer(sf.geometry, 1003::double precision) AS geom
	FROM matsim_output.stop_facilities sf
	LEFT JOIN matsim_output.bike_speed bs
	ON bs.run_name = sf.run_name
	WHERE sf.vvs_bike_ride = 'true'
)

UNION

(
	SELECT sf.*,
		15 AS feed_min,
		(bs.speed_value * 3.6 * (15/60::float) * 1000)/bs.beeline_dfactor AS feed_radius,
		st_buffer(sf.geometry, 1003::double precision) AS geom
	FROM matsim_output.stop_facilities sf
	LEFT JOIN matsim_output.bike_speed bs
	ON bs.run_name = sf.run_name
	WHERE sf.vvs_bike_ride = 'true'
)