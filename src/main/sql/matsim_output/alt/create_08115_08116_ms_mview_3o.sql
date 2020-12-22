CREATE MATERIALIZED VIEW matsim_output."08115_08116_modal_split" AS (

	SELECT
		run_name,
		c_main_mode,
		SUM(no_trips),
		round((((SUM(no_trips))::numeric / SUM(SUM(no_trips)) OVER (partition by run_name)) * (100)::numeric), 1) AS modal_split_sim
		
	FROM matsim_output.kreis_relationen
	WHERE start_kreis != end_kreis
	AND start_kreis in ('08115', '08116')
	AND end_kreis in ('08115', '08116')
	GROUP BY run_name, c_main_mode
	
);

