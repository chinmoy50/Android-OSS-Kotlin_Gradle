package com.kickstarter.services.firebase;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.firebase.iid.FirebaseInstanceId;

import timber.log.Timber;

public class UnregisterService extends JobService {

  @Override
  public boolean onStartJob(JobParameters job) {
    Timber.d("onStartJob");
    new Thread(() -> {
      try {
        FirebaseInstanceId.getInstance().deleteInstanceId();
        Timber.d("Deleted token");
      } catch (final Exception e) {
        Timber.e("Failed to delete token: %s", e);
      }
    });

    return true;
  }

  @Override
  public boolean onStopJob(JobParameters job) {
    return false;
  }

}

