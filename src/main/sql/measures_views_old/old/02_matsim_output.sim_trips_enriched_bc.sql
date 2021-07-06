-- matsim_output.sim_trips_enriched_bc

-- Extract base case run

-- @author dwedekind


SELECT *
FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED
WHERE RUN_NAME = 'bc'