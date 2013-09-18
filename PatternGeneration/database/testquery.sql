/* RDFDB: */
SELECT * FROM rdf_zone
SELECT * FROM rdf_link WHERE right_postal_area_id IS NULL
SELECT * FROM rdf_link WHERE right_admin_place_id IS NULL
SELECT * FROM rdf_admin_place
SELECT * FROM rdf_place_zone WHERE admin_place_id = '21013384'
SELECT * FROM rdf_link_zone WHERE zone_id = 21010908

/* RDFDB: fetch link from city name */
SELECT * FROM rdf_city_poi_name WHERE name = 'Los Angeles'
SELECT * FROM rdf_city_poi_names WHERE name_id = 1440726286
SELECT * FROM rdf_city_poi WHERE poi_id = 1031365282
SELECT * FROM rdf_location WHERE location_id = 1640310
SELECT * FROM rdf_link WHERE link_id = 824370668

/* RDFDB: fetch link from post code */
SELECT * FROM rdf_postal_area WHERE postal_code = '90007'
SELECT * FROM rdf_postal_area WHERE postal_code = '91335'
SELECT * FROM rdf_link WHERE left_postal_area_id = '4035102' OR right_postal_area_id = '4035102'
/* Example */
SELECT link_id, ref_node_id, nonref_node_id
FROM rdf_link, rdf_postal_area
WHERE postal_code = '90007' AND (postal_area_id = left_postal_area_id OR postal_area_id = right_postal_area_id)

/* RDFDB: fetch tollway, speed category, fun class, truck route, direction */
SELECT link_id, functional_class, travel_direction, ramp, tollway, speed_category FROM rdf_nav_link
/* Example */
SELECT t1.link_id, t1.ref_node_id, t1.nonref_node_id, t3.functional_class, t3.travel_direction, t3.ramp, t3.tollway, t3.speed_category
FROM rdf_link t1, rdf_postal_area t2, rdf_nav_link t3
WHERE t2.postal_code = '90007'
AND (t2.postal_area_id = t1.left_postal_area_id OR t2.postal_area_id = t1.right_postal_area_id)
AND t1.link_id = t3.link_id

/* RDFDB: fetch carpool */
SELECT link_id, carpool_road FROM rdf_nav_link_attribute WHERE carpool_road IS NOT NULL
/* Example */
SELECT t4.link_id, ref_node_id, nonref_node_id, functional_class, travel_direction, ramp, tollway, speed_category, carpool_road
FROM
(SELECT t1.link_id, ref_node_id, nonref_node_id, functional_class, travel_direction, ramp, tollway, speed_category
FROM rdf_link t1, rdf_postal_area t2, rdf_nav_link t3
WHERE t2.postal_code = '90007'
AND (t2.postal_area_id = t1.left_postal_area_id OR t2.postal_area_id = t1.right_postal_area_id)
AND t1.link_id = t3.link_id) t4
LEFT JOIN rdf_nav_link_attribute t5
ON t4.link_id = t5.link_id

/* RDFDB: fetch name */
SELECT road_name_id, street_name FROM rdf_road_name
SELECT link_id, road_name_id FROM rdf_road_link
/* Example */
SELECT t8.link_id, street_name, ref_node_id, nonref_node_id, functional_class, travel_direction, ramp, tollway, speed_category, carpool_road
FROM
(SELECT t6.link_id, road_name_id, ref_node_id, nonref_node_id, functional_class, travel_direction, ramp, tollway, speed_category, carpool_road
FROM
(SELECT t4.link_id, ref_node_id, nonref_node_id, functional_class, travel_direction, ramp, tollway, speed_category, carpool_road
FROM
(SELECT t1.link_id, ref_node_id, nonref_node_id, functional_class, travel_direction, ramp, tollway, speed_category
FROM rdf_link t1, rdf_postal_area t2, rdf_nav_link t3
WHERE t2.postal_code = '90007'
AND (t2.postal_area_id = t1.left_postal_area_id OR t2.postal_area_id = t1.right_postal_area_id)
AND t1.link_id = t3.link_id) t4
LEFT JOIN rdf_nav_link_attribute t5
ON t4.link_id = t5.link_id) t6
LEFT JOIN rdf_road_link t7
ON t6.link_id = t7.link_id) t8
LEFT JOIN rdf_road_name t9
on t8.road_name_id = t9.road_name_id

/* RDFDB: fetch node */
SELECT * FROM rdf_node
SELECT node_id, lat, lon, zlevel FROM rdf_node WHERE node_id IN (211128985, 211141355)

