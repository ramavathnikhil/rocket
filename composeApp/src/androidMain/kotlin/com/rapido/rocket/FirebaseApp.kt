package com.rapido.rocket

import com.google.firebase.FirebaseOptions
import com.google.firebase.initialize

actual object FirebaseApp {
    actual fun initialize() {
        if (com.google.firebase.FirebaseApp.getApps().isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(
                android.app.Application.getApplicationContext(),
                FirebaseOptions.Builder()
                    .setProjectId("rapido-captain-test")
                    .setApplicationId("1:468013252606:android:47e9fd55e214c0e4888aaf")
                    .setApiKey("AIzaSyCSjWuJ5iHWfmyQKgHRDuVhg1-VZ05qcIg")
                    .setStorageBucket("rapido-captain-test.firebasestorage.app")
                    .setGcmSenderId("468013252606")
                    .setDatabaseUrl("https://rapido-captain-test.firebaseio.com")
                    .build()
            )
        }
    }
} 