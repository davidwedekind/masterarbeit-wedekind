WITH 

fst as (
	SELECT
		*,
		CASE WHEN matsim_cal_main_mode = 'car' THEN 1 ELSE 0 END as car_usage
	FROM matsim_output.sim_trips_enriched
), 

snd as (
	SELECT
		run_name,
		person,
		agent_home_gem_ags,
		agent_home_gem_gen,
		SUM(car_usage) car_usage
	FROM fst
	GROUP BY run_name, agent_home_gem_ags, agent_home_gem_gen, person
), 

thd as (
SELECT
	run_name,
	agent_home_gem_ags,
	agent_home_gem_gen,
	person,
	CASE WHEN car_usage > 0 THEN 1 ELSE 0 END as car_usage
FROM fst
)

SELECT
	run_name,
	agent_home_gem_ags,
	agent_home_gem_gen,
	(COUNT(person) - SUM(car_usage)) as no_car_usage,
	SUM(car_usage) car_usage
FROM thd
GROUP BY run_name, agent_home_gem_ags, agent_home_gem_gen