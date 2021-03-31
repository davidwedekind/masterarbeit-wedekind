WITH trips AS (
	SELECT
		t.run_name,
		t.person,
		SUM(t.traveled_distance) / 1000 total_pt_km_dist,
		(SUM(t.trav_time)  / 3600) total_pt_h_dur
	FROM matsim_output.sim_trips_raw t
	WHERE matsim_cal_main_mode = 'pt'
	GROUP BY run_name, person

), pt_data AS(
	SELECT
		t.run_name,
		t.person,
		t.total_pt_km_dist,
		t.total_pt_h_dur,
		f."noZones",
		f."fareAmount",
		f."outOfZones",
		(f."fareAmount" / t.total_pt_km_dist) eur_per_pt_km,
		(f."fareAmount" / t.total_pt_h_dur) eur_per_pt_h
	FROM trips t
	JOIN matsim_output.person_2_fares f
	ON f."personId" = t.person
	AND f."run_name" = t.run_name
)

SELECT
	run_name,
	AVG("noZones") avg_no_zones_trav,
	AVG("fareAmount") avg_fare_amt_paid,
	AVG("fareAmount")/AVG("noZones") avg_fare_amt_per_zone_paid,
	AVG(total_pt_km_dist) avg_total_pt_km_dist,
	AVG(eur_per_pt_km) avg_eur_per_pt_km,
	AVG(total_pt_h_dur) avg_total_pt_h_dur,
	AVG(eur_per_pt_h) avg_eur_per_pt_h
FROM pt_data
WHERE "outOfZones" = 0
GROUP BY run_name
ORDER BY run_name