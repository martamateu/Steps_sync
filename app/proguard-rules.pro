# Add project specific ProGuard rules here.
# Keep Health Connect classes
-keep class androidx.health.connect.** { *; }
# Keep WorkManager workers
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
