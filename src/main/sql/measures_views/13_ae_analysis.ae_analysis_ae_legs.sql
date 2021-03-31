SELECT
	t.matsim_raw_main_mode trip_raw_main_mode,
	t.matsim_cal_main_mode trip_cal_main_mode,
	t.agent_home_krs_ags,
	t.agent_home_krs_gen,
	t.agent_home_gem_ags,
	t.agent_home_gem_gen,
	t.start_kreis_ags,
	t.start_kreis_gen,
	t.end_kreis_ags,
	t.end_kreis_gen,
	t.start_gem_ags,
	t.start_gem_gen,
	t.end_gem_ags,
	t.end_gem_gen,
	l.*
FROM (
WITH leg_windows AS(
	SELECT
		ROW_NUMBER() over (partition by run_name, trip_id order by dep_time asc) leg_id_asc,
		ROW_NUMBER() over (partition by run_name, trip_id order by dep_time desc) leg_id_desc,
		*
	FROM matsim_output.sim_legs_raw
	) 	
SELECT * FROM leg_windows
WHERE (leg_id_asc = 1 OR leg_id_desc = 1)
) l
INNER JOIN matsim_output.sim_trips_enriched t
ON t.run_name = l.run_name AND t.trip_id = l.trip_id
WHERE t.matsim_cal_main_mode IN ('pt', 'car', 'ride')
