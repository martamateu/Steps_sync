# Steps\_sync

An Android app (Kotlin) that automatically synchronises daily step counts from
**Health Connect** to a **Google Apps Script** webhook — no user interaction
required after the first permission grant.

---

## Architecture

```
Android App
└── MainApplication          ← schedules WorkManager job on every app start
    └── SyncWorker           ← CoroutineWorker, runs once per 24 h
        ├── HealthConnectRepository  ← aggregates today's steps via Health Connect API
        └── ApiClient        ← HTTP POST JSON payload to Google Apps Script
```

```
WorkManager (24 h periodic) → Health Connect API → OkHttp POST → Google Apps Script → Google Sheets
```

---

## Project structure

```
app/src/main/
├── kotlin/com/stepssync/
│   ├── Constants.kt                 – WEBHOOK_URL and configuration
│   ├── ApiClient.kt                 – OkHttp HTTP POST
│   ├── HealthConnectRepository.kt   – reads & aggregates daily steps
│   ├── SyncWorker.kt                – WorkManager CoroutineWorker
│   ├── MainApplication.kt           – schedules periodic work on startup
│   └── MainActivity.kt              – minimal UI to grant HC permissions
├── res/values/strings.xml
└── AndroidManifest.xml
```

---

## Setup

### 1. Configure your webhook URL

Open `app/src/main/kotlin/com/stepssync/Constants.kt` and replace the
placeholder with your Google Apps Script deployment URL:

```kotlin
const val WEBHOOK_URL =
    "https://script.google.com/macros/s/<YOUR_SCRIPT_ID>/exec"
```

### 2. Create the Google Apps Script

Create a new Apps Script project and paste the following:

```javascript
function doPost(e) {
  const data = JSON.parse(e.postData.contents);
  const sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  sheet.appendRow([data.date, data.steps, new Date()]);
  return ContentService
    .createTextOutput(JSON.stringify({ status: "ok" }))
    .setMimeType(ContentService.MimeType.JSON);
}
```

Deploy as **Web App**:
- Execute as: **Me**
- Who has access: **Anyone**

Copy the `/exec` URL into `Constants.WEBHOOK_URL`.

### 3. Build & install

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 4. Grant permissions

Launch the app once. Tap **Grant Permissions** and accept the Health Connect
dialog. You can then close the app — the background sync will run automatically.

---

## How it works

| Component | Role |
|-----------|------|
| `MainApplication.onCreate()` | Enqueues a `PeriodicWorkRequest` (24 h, network required). Uses `KEEP` policy so re-launches don't reset the timer. |
| `SyncWorker.doWork()` | Checks permissions → reads today's steps → POSTs to webhook → returns `Result.success()` or `Result.retry()`. |
| `HealthConnectRepository` | Calls `HealthConnectClient.aggregate()` with `StepsRecord.COUNT_TOTAL` over a midnight-to-midnight window in the device's local timezone. |
| `ApiClient` | Builds a JSON body `{ "date": "YYYY-MM-DD", "steps": N }` and sends it via OkHttp POST. |

WorkManager survives device reboots and process death. No foreground service
or wake locks are needed.

---

## JSON payload sent to the webhook

```json
{
  "date": "2024-06-15",
  "steps": 8432
}
```

---

## Permissions declared in the manifest

| Permission | Reason |
|------------|--------|
| `android.permission.INTERNET` | Send data to the webhook |
| `android.permission.health.READ_STEPS` | Read step records from Health Connect (Android 14+) |

Health Connect also requires the user to grant the runtime permission via the
Health Connect consent screen — this is handled by `MainActivity`.

---

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| `androidx.work:work-runtime-ktx` | 2.9.0 | Background periodic execution |
| `androidx.health.connect:connect-client` | 1.1.0-alpha07 | Step data |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.7.3 | Coroutines |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | HTTP POST |

---

## Testing the sync manually

Trigger the worker immediately (without waiting 24 h) from the terminal:

```bash
# Using WorkManager's test command (debug build)
adb shell am broadcast \
  -a "androidx.work.diagnostics.REQUEST_DIAGNOSTICS" \
  -p com.stepssync
```

Or enqueue a one-time run from code (e.g. a debug button):

```kotlin
WorkManager.getInstance(context)
    .enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
```

Check logcat for output:

```bash
adb logcat -s SyncWorker MainApplication
```
