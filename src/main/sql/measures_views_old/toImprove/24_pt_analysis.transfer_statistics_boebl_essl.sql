-- pt_analysis.pt_statistics_boebl_essl

-- Provide pt transfer statistics on pt trips of relation Boeblingen-Esslingen
 
-- @author dwedekind


-- First, calculate statistics per each trip
WITH stats As (
	SELECT
		t.run_name,
		t.trip_id,
		SUM((t.traveled_distance/t.trav_time * 3.6)) travel_speed,
	
		-- Number of transfers defined as number of pt legs per trip - 1
		COUNT(l.index) - 1 transfers
	
	FROM matsim_output.sim_trips_enriched t
	LEFT JOIN matsim_output.sim_legs_raw l
	ON t.trip_id = l.trip_id
	AND t.run_name = l.run_name
	
	-- Look at pt trips only for transfers
	WHERE t.matsim_cal_main_mode = 'pt'
	-- Look at trips on Boeblingen - Esslingen relation only
	AND ((t.start_kreis_ags = '08115' AND t.end_kreis_ags = '08116')
		 OR (t.start_kreis_ags = '08116' AND t.end_kreis_ags = '08115'))
	AND l.mode = 'pt'
	
	GROUP BY t.run_name, t.trip_id
)



-- Final calculation of average per run
SELECT run_name,
	AVG(transfers) avg_transfers,
	AVG(travel_speed) avg_travel_speed
FROM stats
GROUP BY run_name