-- ae_analysis.matsim_output.pt_comparator_results_corrected

-- @author dwedekind

WITH fst AS(
	select *,
		regexp_replace(regexp_replace(route_description, '^.*arrTime=', ''),'\].*', '') AS arr_time
	from matsim_output.pt_comparator_results
)

select
	run_name,
	person_id,
	trip_id,
	routing_mode,
	start_time,
	split_part(arr_time, ':', 1)::INTEGER*3600+split_part(arr_time, ':', 2)::INTEGER*60+split_part(arr_time, ':', 3)::INTEGER arr_time,
	(split_part(arr_time, ':', 1)::INTEGER*3600+split_part(arr_time, ':', 2)::INTEGER*60+split_part(arr_time, ':', 3)::INTEGER)-start_time trav_time,
	"containsS60",
	time_on_s60,
	is_bike_and_ride,
	is_walk,
	route_description
from fst

