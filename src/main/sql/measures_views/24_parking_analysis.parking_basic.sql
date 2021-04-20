-- parking_analysis.parking_basic

-- Provide basic indicators for each run, area of parking and parking type
-- Areas are 'Reg Stuttgart mit LH Stuttgart', 'LH Stuttgart', 'Reg Stuttgart', 'LK Boeb & Essl' and 'Fokusgemeinden'

-- @author dwedekind


-- Aggregate all parkings in the greater Stuttgart area
WITH REG_STUTTGART_COMPLETE AS(
	SELECT RUN_NAME,
		'Reg Stuttgart mit LH Stuttgart' P_GROUP,
		"parkingType" PARKING_TYPE,
		COUNT("parkingId") PARKINGS,
		SUM("parkingFee") PARKING_REVENUES,
		AVG("parkingDuration") AVG_PARKING_DURATION

	FROM MATSIM_OUTPUT.PARKINGS_ENRICHED 
	WHERE KRS_AGS IN ('08111', '08115', '08116', '08117', '08118', '08119')

	GROUP BY RUN_NAME,
		"parkingType"
),

-- Aggregate all parkings in the 'LH Stuttgart'
LH_STUTTGART AS(
	SELECT RUN_NAME,
		'LH Stuttgart' P_GROUP,
		"parkingType" PARKING_TYPE,
		COUNT("parkingId") PARKINGS,
		SUM("parkingFee") PARKING_REVENUES,
		AVG("parkingDuration") AVG_PARKING_DURATION

	FROM MATSIM_OUTPUT.PARKINGS_ENRICHED 
	WHERE KRS_AGS IN ('08111')

	GROUP BY RUN_NAME,
		"parkingType"
),

-- Aggregate all parkings in the Region Stuttgart (without LH Stuttgart)
REG_STUTTGART AS(
	SELECT RUN_NAME,
		'Reg Stuttgart' P_GROUP,
		"parkingType" PARKING_TYPE,
		COUNT("parkingId") PARKINGS,
		SUM("parkingFee") PARKING_REVENUES,
		AVG("parkingDuration") AVG_PARKING_DURATION

	FROM MATSIM_OUTPUT.PARKINGS_ENRICHED 
	WHERE KRS_AGS IN ('08115', '08116', '08117', '08118', '08119')

	GROUP BY RUN_NAME,
		"parkingType"
),

-- Aggregate all parkings in Boeblingen and Esslingen county
BOEB_ESSL AS(
	SELECT RUN_NAME,
		CASE 
			WHEN P_BOEBL_ESSL = 1 THEN 'LK Boeb & Essl'
			ELSE 'Außer LK Boeb & Essl'
		END AS P_GROUP,
		"parkingType" PARKING_TYPE,
		COUNT("parkingId") PARKINGS,
		SUM("parkingFee") PARKING_REVENUES,
		AVG("parkingDuration") AVG_PARKING_DURATION

	FROM MATSIM_OUTPUT.PARKINGS_ENRICHED 

	GROUP BY RUN_NAME,
		P_BOEBL_ESSL,
		"parkingType"
),

-- Aggregate all parkings in Focus areas of Boeblingen and Esslingen county
FOCUS_AREAS AS(
	SELECT RUN_NAME,
		CASE 
			WHEN P_FOCUS_AREAS = 1 THEN 'Fokusgemeinden'
			ELSE 'Außer Fokusgemeinden'
		END AS P_GROUP,
		"parkingType" PARKING_TYPE,
		COUNT("parkingId") PARKINGS,
		SUM("parkingFee") PARKING_REVENUES,
		AVG("parkingDuration") AVG_PARKING_DURATION

	FROM MATSIM_OUTPUT.PARKINGS_ENRICHED 

	GROUP BY RUN_NAME,
		P_FOCUS_AREAS,
		"parkingType"
),


-- Final union
M_GROUPS AS (
	SELECT * FROM (			
		SELECT * FROM REG_STUTTGART_COMPLETE
		UNION
		SELECT * FROM LH_STUTTGART
		UNION
		SELECT * FROM REG_STUTTGART
		UNION
		SELECT * FROM BOEB_ESSL
		UNION
		SELECT * FROM FOCUS_AREAS
	) AS UN

	ORDER BY RUN_NAME, P_GROUP
)


-- Some final calculations
SELECT RUN_NAME,
	P_GROUP,
	PARKING_TYPE,
	PARKINGS*{**sfactor**} PARKINGS,
	PARKING_REVENUES*(-1)*{**sfactor**} PARKING_REVENUES,
	AVG_PARKING_DURATION
FROM M_GROUPS

ORDER BY RUN_NAME, P_GROUP, PARKING_TYPE