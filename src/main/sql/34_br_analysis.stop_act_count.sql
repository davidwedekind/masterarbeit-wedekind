-- br_analysis.stop_act_count

-- Count activities in given radius 
 
-- @author dwedekind
SELECT br.id,
	br.name,
	count(ag.act_no)
FROM br_analysis.stop_feed_radius br
INNER JOIN matsim_input.agent_activities ag
ON ST_Within(ST_SetSRID( ST_Point( ag.x, ag.y), 25832), br.geom)
GROUP BY br.id, br.name