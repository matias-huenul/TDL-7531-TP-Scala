# Teoría de Lenguaje 75.31 - Trabajo Práctico

## Ejecución

Iniciar el servidor:

```bash
make
```

Iniciar `ngrok` para poder acceder al servidor desde internet:

```bash
docker run -it -e NGROK_AUTHTOKEN=... --network host ngrok/ngrok http 8080
```

Setear el webhook de Telegram para que apunte a la URL de `ngrok`:

```bash
curl -s --request POST \
    --header "Content-Type: application/json" \
    --data "{\"url\": \"https://....ngrok-free.app\"}" \
    "https://api.telegram.org/bot.../setWebhook" >/dev/null
```
