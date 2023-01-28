package com.simplemobiletools.gallery.pro.aes;

import android.content.Intent;

import com.simplemobiletools.commons.activities.AboutActivity;
import com.simplemobiletools.commons.activities.BaseSimpleActivity;
import com.simplemobiletools.gallery.pro.activities.SettingsActivity;

public abstract class BaseActivityOverride extends BaseSimpleActivity {

    @Override
    public void startActivity(Intent intent) {
        if (intent.getComponent() != null && AboutActivity.class.getName().equals(intent.getComponent().getShortClassName())) {
            //   intent.setClass(getApplicationContext(), SettingsActivity.class);
        }
        super.startActivity(intent);
    }
}
