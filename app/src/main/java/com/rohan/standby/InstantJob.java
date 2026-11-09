package com.rohan.standby;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

/** Safety net: while the phone charges, Android runs this about every 15 minutes (and after a reboot), even if the system stopped the watcher. It simply starts the watcher again. */
public class InstantJob extends JobService {
    private static final int ID = 7101;
    @Override public boolean onStartJob(JobParameters params) { Instant.sync(this); return false; }
    @Override public boolean onStopJob(JobParameters params) { return false; }
    static void schedule(Context c) {
        try {
            JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE); if (js == null || js.getPendingJob(ID) != null) return;
            js.schedule(new JobInfo.Builder(ID, new ComponentName(c, InstantJob.class)).setRequiresCharging(true).setPersisted(true).setPeriodic(15 * 60 * 1000L).build());
        } catch (RuntimeException ignored) { }
    }
    static void cancel(Context c) { try { JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE); if (js != null) js.cancel(ID); } catch (RuntimeException ignored) { } }
}
