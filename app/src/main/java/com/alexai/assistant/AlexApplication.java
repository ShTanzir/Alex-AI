package com.alexai.assistant;

import android.app.Application;

import com.alexai.assistant.storage.SecureSettingsRepository;

public class AlexApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SecureSettingsRepository.applyTheme(new SecureSettingsRepository(this).getTheme());
    }
}
