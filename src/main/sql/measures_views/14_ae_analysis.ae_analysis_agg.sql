SELECT
    ae_analysis_ae_legs.run_name,
    ae_analysis_ae_legs.trip_cal_main_mode,
    avg(ae_analysis_ae_legs.distance) AS avg
FROM ae_analysis.ae_analysis_ae_legs
GROUP BY ae_analysis_ae_legs.run_name, ae_analysis_ae_legs.trip_cal_main_mode