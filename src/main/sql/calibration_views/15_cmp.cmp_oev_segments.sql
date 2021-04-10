-- cmp.cmp_oev_segments

-- Compare the usage of the different pt submodes used in the simulation to vvs numbers
-- Submodes are: ssb (Stuttgarter Straßenbahnen), sbahn (S-Bahn Stuttgart), bus, dbregio (for simplification all other)

-- @author dwedekind

SELECT SIM.RUN_NAME
	,SIM.PT_GROUP
	,SIM.SGMT_REL SIM_SGMT_REL
	,CAL.SGMT_REL / 100 CAL_SGMT_REL
FROM MATSIM_OUTPUT.SIM_OEV_SEGMENTS SIM
LEFT JOIN CAL.VVS_PT_SEGMENTS CAL ON CAL.PT_SEGMENT = SIM.PT_GROUP
ORDER BY SIM.RUN_NAME,
	SIM.PT_GROUP