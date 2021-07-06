-- matsim_output.pt_comparator_results_enriched

-- Compare enrich_trips of comparator results
 
-- @author dwedekind

 
-- Compare pt and bike
-- Emulate bike trip0
select
	pt.run_name,
	pt.person_id,
	pt.trip_id,
	trips.geometry,
	trips.euclidean_distance dist,
	trips.distance_group,
	trips.distance_group_no,
	trips.res_boebl_essl,
	trips.res_focus_areas,
	trips.se_boebl_essl,
	trips.se_focus_areas,
	trips.rel_boebl_essl,
	trips.rel_focus_areas,
	trips.res_rel_boebl_essl,
	trips.res_rel_focus_areas,
	trips.trav_time actual_trav_time,
	trips.matsim_cal_main_mode actual_mode,
	pt.trav_time pt_trav_time
from matsim_output.sim_trips_enriched trips 
inner join matsim_output.pt_comparator_results_corrected pt
on pt.run_name = trips.run_name
and pt.trip_id = trips.trip_id
