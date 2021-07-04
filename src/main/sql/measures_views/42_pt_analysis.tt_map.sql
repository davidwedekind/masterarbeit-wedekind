-- pt_analysis.tt_map
-- Provide trip counts for s60 usage on new section

-- @author dwedekind


SELECT
	sm.*,
	car.trav_time car_trav_time,
	pt.pt_trav_time
from basic_analysis.switch_map sm
left join matsim_output.sim_trips_enriched car
on sm.trip_id = car.trip_id
left join matsim_output.pt_comparator_results_enriched pt
on sm.trip_id = pt.trip_id 
where car.run_name = 'bc'
and car.matsim_cal_main_mode = 'car'
and pt.run_name = 'm5_3'
