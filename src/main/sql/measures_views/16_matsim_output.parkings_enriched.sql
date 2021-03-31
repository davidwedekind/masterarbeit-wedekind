SELECT
    p.*,
    n.zone_name,
    n.zone_group,
    n.link_length,
    n.geometry
FROM matsim_output.parkings p
LEFT JOIN matsim_output.network n ON ((p."linkId" = n."Id") AND (p."run_name" = n."run_name"))