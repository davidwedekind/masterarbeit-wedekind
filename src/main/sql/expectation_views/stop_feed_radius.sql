SELECT stop_facilities.id,
    stop_facilities.name,
    955 AS feed_radius,
    st_buffer(stop_facilities.geometry, 955::double precision) AS geom
   FROM matsim_output.stop_facilities
  WHERE stop_facilities.run_name = 'bc'::text AND stop_facilities.vvs_bike_ride = 'true'::text
UNION
 SELECT stop_facilities.id,
    stop_facilities.name,
    1003 AS feed_radius,
    st_buffer(stop_facilities.geometry, 1003::double precision) AS geom
   FROM matsim_output.stop_facilities
  WHERE stop_facilities.run_name = 'bc'::text AND stop_facilities.vvs_bike_ride = 'true'::text
UNION
 SELECT stop_facilities.id,
    stop_facilities.name,
    1050 AS feed_radius,
    st_buffer(stop_facilities.geometry, 1050::double precision) AS geom
   FROM matsim_output.stop_facilities
  WHERE stop_facilities.run_name = 'bc'::text AND stop_facilities.vvs_bike_ride = 'true'::text
UNION
 SELECT stop_facilities.id,
    stop_facilities.name,
    1098 AS feed_radius,
    st_buffer(stop_facilities.geometry, 1050::double precision) AS geom
   FROM matsim_output.stop_facilities
  WHERE stop_facilities.run_name = 'bc'::text AND stop_facilities.vvs_bike_ride = 'true'::text
UNION
 SELECT stop_facilities.id,
    stop_facilities.name,
    1432 AS feed_radius,
    st_buffer(stop_facilities.geometry, 1432::double precision) AS geom
   FROM matsim_output.stop_facilities
  WHERE stop_facilities.run_name = 'bc'::text AND stop_facilities.vvs_bike_ride = 'true'::text
UNION
 SELECT stop_facilities.id,
    stop_facilities.name,
    1910 AS feed_radius,
    st_buffer(stop_facilities.geometry, 1910::double precision) AS geom
   FROM matsim_output.stop_facilities
  WHERE stop_facilities.run_name = 'bc'::text AND stop_facilities.vvs_bike_ride = 'true'::text
UNION
 SELECT stop_facilities.id,
    stop_facilities.name,
    2005 AS feed_radius,
    st_buffer(stop_facilities.geometry, 2005::double precision) AS geom
   FROM matsim_output.stop_facilities
  WHERE stop_facilities.run_name = 'bc'::text AND stop_facilities.vvs_bike_ride = 'true'::text
UNION
 SELECT stop_facilities.id,
    stop_facilities.name,
    2101 AS feed_radius,
    st_buffer(stop_facilities.geometry, 2101::double precision) AS geom
   FROM matsim_output.stop_facilities
  WHERE stop_facilities.run_name = 'bc'::text AND stop_facilities.vvs_bike_ride = 'true'::text
UNION
 SELECT stop_facilities.id,
    stop_facilities.name,
    2196 AS feed_radius,
    st_buffer(stop_facilities.geometry, 2196::double precision) AS geom
   FROM matsim_output.stop_facilities
  WHERE stop_facilities.run_name = 'bc'::text AND stop_facilities.vvs_bike_ride = 'true'::text
UNION
 SELECT stop_facilities.id,
    stop_facilities.name,
    2865 AS feed_radius,
    st_buffer(stop_facilities.geometry, 2865::double precision) AS geom
   FROM matsim_output.stop_facilities
  WHERE stop_facilities.run_name = 'bc'::text AND stop_facilities.vvs_bike_ride = 'true'::text