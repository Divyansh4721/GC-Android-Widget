package com.gcjewellers.rateswidget;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RatesUpdateWorker extends Worker {
    public RatesUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(getApplicationContext(), RatesWidgetProvider.class);
            intent.setAction("com.gcjewellers.rateswidget.ACTION_REFRESH");
            getApplicationContext().sendBroadcast(intent);
        }
        return Result.success();
    }
}
