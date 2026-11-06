package com.rohan.standby;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.os.Process;
import android.service.notification.StatusBarNotification;
import android.view.View;

/** Debug only. Draws the call or alarm takeover frame from a fake notification. Extras: --ez call true|false, --ei n 2|3 (number of actions). */
public class CallPreview extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b); getWindow().getDecorView().setSystemUiVisibility(5894); Fonts.init(this);
        final boolean call = getIntent().getBooleanExtra("call", true); int n3 = getIntent().getIntExtra("n", 2);
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, PreviewActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder nb = new Notification.Builder(this, "x").setSmallIcon(android.R.drawable.sym_def_app_icon).setCategory(call ? Notification.CATEGORY_CALL : Notification.CATEGORY_ALARM).setContentTitle(call ? "Sébastien" : "Wake up").setContentText(call ? "mobile" : "6:30 AM");
        int type = getIntent().getIntExtra("type", 1); if (call) { nb.addExtras(new Bundle()); android.os.Bundle ex = new android.os.Bundle(); ex.putInt("android.callType", type); nb.addExtras(ex); if (type == 2) nb.setUsesChronometer(true).setWhen(System.currentTimeMillis() - 83000); }
        if (n3 == 3) nb.addAction(new Notification.Action.Builder(null, "Voicemail", pi).build());
        if (!call || type == 1) nb.addAction(new Notification.Action.Builder(null, call ? "Decline" : "Snooze", pi).build()).addAction(new Notification.Action.Builder(null, call ? "Accept" : "Stop", pi).build()); else nb.addAction(new Notification.Action.Builder(null, "End call", pi).build());
        final StatusBarNotification sbn = new StatusBarNotification("p", "p", 1, "t", 1, 1, 0, nb.build(), Process.myUserHandle(), 0); final CallFrame f = new CallFrame(this);
        setContentView(new View(this) { @Override protected void onDraw(Canvas c) { f.draw(c, getWidth(), getHeight(), sbn); if (f.animated()) postInvalidateDelayed(33); } });
    }
}
