package com.rohan.standby;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restarts the Instant mode watcher after the phone reboots or the app is updated. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) { Instant.sync(context); }
}
