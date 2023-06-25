CREATE OR REPLACE FUNCTION estimate_property_value(
  _location varchar,
  _rooms int DEFAULT NULL,
  _property_type varchar DEFAULT NULL,
  _surface_total int DEFAULT NULL
)
RETURNS TABLE (estimated_property_value int, currency varchar) AS $$
  SELECT
    CAST(AVG(CAST(price AS int)) AS int) AS estimated_property_value,
    currency
  FROM
    properties
  WHERE
    neighborhood = _location
    AND (_rooms IS NULL OR rooms = _rooms)
    AND (_property_type IS NULL OR property_type = _property_type)
    AND (_surface_total IS NULL OR surface_total between _surface_total-10 and _surface_total+10)
    AND operation_type = 'Venta'
  GROUP BY currency
$$ LANGUAGE SQL IMMUTABLE