-- pt_analysis.rohr_transfers

-- Count number of transfers at Stuttgart Rohr
 
-- @author dwedekind

 
-- Select trips that have pt legs ending at Stuttgart-Rohr
WITH ENDINGS AS
	(SELECT RUN_NAME,
			TRIP_ID
	 
		FROM MATSIM_OUTPUT.SIM_LEGS_RAW
		WHERE EGRESS_STOP_ID LIKE '8005773%'
	 
		GROUP BY RUN_NAME,
			TRIP_ID)
			

-- And then count the ones that have starting legs Stuttgart-Rohr
SELECT E.RUN_NAME,
	COUNT(E.TRIP_ID) TRANSFERS
	
FROM ENDINGS E
LEFT JOIN MATSIM_OUTPUT.SIM_LEGS_RAW S ON E.RUN_NAME = S.RUN_NAME
AND E.TRIP_ID = S.TRIP_ID
WHERE ACCESS_STOP_ID LIKE '8005773%'

GROUP BY E.RUN_NAME