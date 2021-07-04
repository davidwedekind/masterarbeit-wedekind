-- basic_analysis.switch_map
-- Provide trip counts for s60 usage on new section

-- @author dwedekind


SELECT
	tsl.*,
	ST_SetSRID( ST_Point(trips.start_x, trips.start_y), 25832) start_point,
	ST_SetSRID( ST_Point(trips.end_x, trips.end_y), 25832) end_point
from basic_analysis.trips_switch_list tsl
left join matsim_output.sim_trips_enriched trips
on tsl.run_name = trips.run_name
and tsl.trip_id = trips.trip_id