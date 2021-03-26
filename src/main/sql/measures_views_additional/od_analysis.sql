WITH t1 AS (
	SELECT
		t.trip_id,
		gem_1.ags AS start_gem_ags,
		gem_2.ags AS end_gem_ags
	FROM matsim_output.sim_trips_enriched t
	JOIN raw.gemeinden gem_1 ON (st_within(st_setsrid(st_point(t.start_x, t.start_y), 25832), gem_1.geometry))
	JOIN raw.gemeinden gem_2 ON (st_within(st_setsrid(st_point(t.end_x, t.end_y), 25832), gem_2.geometry))
	WHERE t.run_name = 'cal_149_04'
	),
	
	t2 AS(
		SELECT
			CONCAT('0',(GREATEST(TRIM(LEADING '0' FROM t1.start_gem_ags)::INTEGER,TRIM(LEADING '0' FROM t1.end_gem_ags)::INTEGER))::TEXT) start_gem_ags,
			CONCAT('0',(LEAST(TRIM(LEADING '0' FROM t1.start_gem_ags)::INTEGER,TRIM(LEADING '0' FROM t1.end_gem_ags)::INTEGER))::TEXT) end_gem_ags,
			t1.trip_id
		FROM t1
	),
	
	relations AS (
		
		SELECT
			start_gem_ags,
			end_gem_ags,
			COUNT(trip_id)
		FROM t2
		GROUP BY start_gem_ags, end_gem_ags
	)

SELECT 
	r.*,
	ST_MakeLine(St_Centroid(gem_1.geometry),St_Centroid(gem_2.geometry)) geometry
FROM relations r
JOIN raw.gemeinden gem_1
ON gem_1.ags = r.start_gem_ags
JOIN raw.gemeinden gem_2
ON gem_2.ags = r.end_gem_ags
	
	
	

