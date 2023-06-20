# Teoría de Lenguaje 75.31 - Trabajo Práctico - Web Server

Esta carpeta contiene el código del servidor web que se encarga de recibir los mensajes del bot de Telegram, leer la información de la base de datos y responder a los usuarios.

## Configuración

### Supabase

Ejecutar en Supabase el SQL provisto en el archivo `supabase_setup.sql`.

### Variables de entorno

Crear un archivo `.env` en la raíz del proyecto con las siguientes variables de entorno:

```bash
UID= # ID de usuario (se puede obtener con el comando "id -u")
GID= # ID de grupo (se puede obtener con el comando "id -g")
TELEGRAM_API_TOKEN= # Token de la API de Telegram
SUPABASE_API_URL= # URL de la API de Supabase
SUPABASE_API_KEY= # API Key de Supabase
```

### Ngrok

Iniciar `ngrok` para poder acceder al servidor desde internet:

```bash
docker run -it -e NGROK_AUTHTOKEN=... --network host ngrok/ngrok http 8080`
```

Setear el webhook de Telegram para que apunte a la URL de `ngrok`:

```bash
curl -s --request POST \
    --header "Content-Type: application/json" \
    --data "{\"url\": \"https://....ngrok-free.app\"}" \
    "https://api.telegram.org/bot.../setWebhook" >/dev/null
```

## Ejecución

Iniciar el servidor:

```bash
make
```
