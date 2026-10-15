# cragcast history

One CSV per month (`<YYYY>/<MM>.csv`): every crag's verdict for each day and the weather it
came from, saved each evening by `.github/workflows/history.yml` on main. Columns:
date, crag_id, crag_name, province, verdict, rain_last48h_mm, rain_day_mm, max_temp_c,
max_wind_kmh, reasons, generated_at. Data: OpenStreetMap (ODbL), Open-Meteo (CC BY 4.0).
