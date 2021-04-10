-- parking_analysis.car_usage

-- Find out how many agents are at least using car once per simulation run
-- This could be an indicator of car ownership

-- @author dwedekind


-- First, select all trips and mark those trips of mode car
-- with the integer value of 1 in the car_usage field
WITH FST AS
	(SELECT *,

			CASE
							WHEN MATSIM_CAL_MAIN_MODE = 'car' THEN 1
							ELSE 0
			END AS CAR_USAGE
	 
		FROM MATSIM_OUTPUT.SIM_TRIPS_ENRICHED),
		
	
	-- Second, sum up the field car_usage for each person
	-- Preserve AGENT_HOME_GEM_AGS, AGENT_HOME_GEM_GEN, thus include in the group clause
	SND AS
	(SELECT RUN_NAME,
			PERSON,
			AGENT_HOME_GEM_AGS,
			AGENT_HOME_GEM_GEN,
			SUM(CAR_USAGE) CAR_USAGE
		FROM FST
	 
		GROUP BY RUN_NAME,
			AGENT_HOME_GEM_AGS,
			AGENT_HOME_GEM_GEN,
			PERSON),
			
	-- Third, identify persons with a car_usage of > 0
	-- This means, that at least one trip has been made via car
	THD AS
	(SELECT RUN_NAME,
			AGENT_HOME_GEM_AGS,
			AGENT_HOME_GEM_GEN,
			PERSON,
			CASE
							WHEN CAR_USAGE > 0 THEN 1
							ELSE 0
			END AS CAR_USAGE
		FROM FST)
		
-- Finally, count persons that use cars for each community
-- Oppose, persons that do not user cars
SELECT RUN_NAME,
	AGENT_HOME_GEM_AGS,
	AGENT_HOME_GEM_GEN,
	(COUNT(PERSON) - SUM(CAR_USAGE)) AS NO_CAR_USAGE,
	SUM(CAR_USAGE) CAR_USAGE
	
FROM THD

GROUP BY RUN_NAME,
	AGENT_HOME_GEM_AGS,
	AGENT_HOME_GEM_GEN