# Steps_sync

Android app minimalista tipo daemon para sincronizar un resumen diario de pasos desde Health Connect hacia un Google Apps Script mediante `HTTP POST`.

## Arquitectura

```text
app
├── app/MainApplication       -> programa WorkManager único cada 24h
├── app/MainActivity          -> pantalla mínima solo para conceder permisos
├── data/HealthConnectRepository -> agrega StepsRecord.COUNT_TOTAL del último día completo
├── data/SyncStateStore       -> evita duplicados por fecha con SharedPreferences
├── network/ApiClient         -> envía { date, steps } al webhook
└── sync/SyncWorker           -> orquesta lectura + POST + control de errores
```

## Configuración clave

- Webhook fijo en `app/src/main/kotlin/com/stepssync/config/Constants.kt`
- Payload enviado:

```json
{
  "date": "YYYY-MM-DD",
  "steps": 1234
}
```

## Cómo probar

1. Instala Health Connect en el dispositivo si no está disponible.
2. Instala la app y ábrela una vez.
3. Concede el permiso de lectura de pasos.
4. Verifica en `adb logcat -s MainApplication SyncWorker MainActivity` que el worker quede programado.
5. Espera la ejecución periódica de WorkManager o fuerza una ejecución desde Android Studio / WorkManager Inspector si está disponible.

## Checklist de errores típicos

- Health Connect no instalado o desactualizado.
- Permiso `READ_STEPS` no concedido.
- El webhook de Apps Script no está desplegado como `/exec` público.
- Sin conectividad de red en la ventana de ejecución del worker.
- Datos de pasos vacíos porque el último día completo todavía no tiene registros.
- WorkManager no ejecuta exactamente a una hora fija: Android puede diferir la tarea dentro de su ventana.

## Validación local esperada

- `./gradlew lintDebug`
- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`

En este entorno no fue posible descargar dependencias de Android/Google para ejecutar el build completo.
