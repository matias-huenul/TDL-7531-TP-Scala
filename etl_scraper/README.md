# ETL Scraper

This is a simple ETL scraper that scrapes data from a website, transforms it, and loads it into a database. It also use Mercadolibre API to retrive more properties.
It is schedule to update the rent database every 7 days and the sale database every 30 days.
Also it can be schedule to run at a cetaing time of the day.

## Environment variables

Create a `.env` file in the root of the project with the following environment variables:

```bash
UID= # User ID (can be obtained with the command "id -u")
GID= # Group ID (can be obtained with the command "id -g")
JDBC_URL= # JDBC URL of the database
DB_USER= # JDBC user of the database
DB_PASSWORD= # JDBC password of the database
```

In our case we use Supabase as a database.


