SELECT
    ae_analysis_ae_legs.run_name,
    ae_analysis_ae_legs.trip_cal_main_mode,
    avg(ae_analysis_ae_legs.distance) AS avg
FROM ae_analysis.ae_analysis_ae_legs
WHERE (((ae_analysis_ae_legs.start_kreis_ags = '08115'::text) AND (ae_analysis_ae_legs.end_kreis_ags = '08116'::text)) OR ((ae_analysis_ae_legs.start_kreis_ags = '08116'::text) AND (ae_analysis_ae_legs.end_kreis_ags = '08115'::text)))
GROUP BY ae_analysis_ae_legs.run_name, ae_analysis_ae_legs.trip_cal_main_mode;