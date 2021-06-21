-- br_analysis.cmp_bike_pt

-- Compare optimal bike and pt route
 
-- @author dwedekind

 
-- Compare pt and bike
-- Emulate bike trip
(
	select
		pt.run_name,
		pt.person_id,
		pt.trip_id,
		trips.geometry,
		'pt' matsim_cal_main_mode,
		trips.euclidean_distance/1000::float dist_km,
		pt.trav_time/3600 time_hrs,
		trips.distance_group,
		trips.distance_group_no,
		trips.res_boebl_essl,
		trips.res_focus_areas,
		trips.se_boebl_essl,
		trips.se_focus_areas,
		trips.rel_boebl_essl,
		trips.rel_focus_areas,
		trips.res_rel_boebl_essl,
		trips.res_rel_focus_areas
	from matsim_output.pt_comparator_results pt
	left join matsim_output.sim_trips_enriched trips
	on pt.run_name = trips.run_name
	and pt.trip_id = trips.trip_id
	where pt.routing_mode = 'pt_w_bike_allowed'
)

union

(
	select
		pt.run_name,
		pt.person_id,
		pt.trip_id,
		trips.geometry,
		'bike' matsim_cal_main_mode,
		trips.euclidean_distance/1000::float dist_km,
		(bs.beeline_dfactor * (trips.euclidean_distance/1000::float))/ (3.6 * bs.speed_value) time_hrs,
		trips.distance_group,
		trips.distance_group_no,
		trips.res_boebl_essl,
		trips.res_focus_areas,
		trips.se_boebl_essl,
		trips.se_focus_areas,
		trips.rel_boebl_essl,
		trips.rel_focus_areas,
		trips.res_rel_boebl_essl,
		trips.res_rel_focus_areas
	from matsim_output.pt_comparator_results pt
	left join matsim_output.sim_trips_enriched trips
	on pt.run_name = trips.run_name
	and pt.trip_id = trips.trip_id
	left join matsim_output.bike_speed bs
	on pt.run_name = bs.run_name
	where pt.routing_mode = 'pt_w_bike_allowed'
)