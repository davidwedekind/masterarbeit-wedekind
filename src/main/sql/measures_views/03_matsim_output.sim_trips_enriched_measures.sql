-- matsim_output.sim_trips_enriched_measures

-- Extract measure runs (all other runs than the base case run)

-- @author dwedekind


SELECT *
FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
WHERE RUN_NAME != 'bc'