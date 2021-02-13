 WITH trip_stats_bc AS (
         SELECT sim_trips_enriched_bc.run_name,
            sim_trips_enriched_bc.area,
            sim_trips_enriched_bc.matsim_cal_main_mode,
            count(sim_trips_enriched_bc.trip_id) AS trips_bc,
            ((count(sim_trips_enriched_bc.trip_id))::numeric / sum(count(sim_trips_enriched_bc.trip_id)) OVER (PARTITION BY sim_trips_enriched_bc.run_name)) AS mode_share_bc
           FROM matsim_output.sim_trips_enriched_bc
          GROUP BY sim_trips_enriched_bc.run_name, sim_trips_enriched_bc.area, sim_trips_enriched_bc.matsim_cal_main_mode
        ), trip_stats_measures AS (
         SELECT sim_trips_enriched_measures.run_name,
            sim_trips_enriched_measures.area,
            sim_trips_enriched_measures.matsim_cal_main_mode,
            count(sim_trips_enriched_measures.trip_id) AS trips_m,
            ((count(sim_trips_enriched_measures.trip_id))::numeric / sum(count(sim_trips_enriched_measures.trip_id)) OVER (PARTITION BY sim_trips_enriched_measures.run_name)) AS mode_share_m
           FROM matsim_output.sim_trips_enriched_measures
          GROUP BY sim_trips_enriched_measures.run_name, sim_trips_enriched_measures.area, sim_trips_enriched_measures.matsim_cal_main_mode
        )
 SELECT m.run_name,
    bc.area,
    bc.matsim_cal_main_mode,
    bc.trips_bc,
    bc.mode_share_bc,
    m.trips_m,
    m.mode_share_m,
    (m.trips_m - bc.trips_bc) AS trips_diff,
    (m.mode_share_m - bc.mode_share_bc) AS mode_share_diff
   FROM (trip_stats_bc bc
     JOIN trip_stats_measures m ON (((bc.matsim_cal_main_mode = m.matsim_cal_main_mode) AND (bc.area = m.area))))
  ORDER BY m.run_name, bc.area, bc.matsim_cal_main_mode