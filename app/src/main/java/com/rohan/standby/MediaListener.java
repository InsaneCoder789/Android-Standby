package com.rohan.standby;

import android.service.notification.NotificationListenerService;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.media.MediaMetadata;
import android.content.ComponentName;
import android.graphics.Bitmap;
import java.util.List;

public class MediaListener extends NotificationListenerService {
    static MediaListener instance;
    private final java.util.LinkedHashMap<String,android.service.notification.StatusBarNotification> alerts=new java.util.LinkedHashMap<>();
    private final java.util.LinkedHashMap<String,Timers.State> timers=new java.util.LinkedHashMap<>();
    /** The Clock app's running timer (the one ending soonest), or null. */
    Timers.State timer(){synchronized(timers){Timers.State best=null;long now=System.currentTimeMillis();for(Timers.State t:timers.values())if(best==null||t.left(now)<best.left(now))best=t;return best;}}
    private void trackTimer(android.service.notification.StatusBarNotification n){
        android.app.Notification no=n.getNotification();android.os.Bundle x=no.extras;if(x==null||(no.flags&android.app.Notification.FLAG_GROUP_SUMMARY)!=0){synchronized(timers){timers.remove(n.getKey());}return;}
        String channel=no.getChannelId()==null?"":no.getChannelId().toLowerCase(java.util.Locale.ROOT),title=String.valueOf(x.getCharSequence(android.app.Notification.EXTRA_TITLE));
        if(!channel.contains("timer")||channel.contains("stopwatch")||channel.contains("expired")){synchronized(timers){timers.remove(n.getKey());}return;}
        boolean paused=false;if(no.actions!=null)for(android.app.Notification.Action a:no.actions){String t=String.valueOf(a.title).toLowerCase(java.util.Locale.ROOT);if(t.contains("resume")||t.contains("start")||t.contains("play"))paused=true;}
        long now=System.currentTimeMillis();java.util.Calendar cal=java.util.Calendar.getInstance();cal.set(java.util.Calendar.HOUR_OF_DAY,0);cal.set(java.util.Calendar.MINUTE,0);cal.set(java.util.Calendar.SECOND,0);cal.set(java.util.Calendar.MILLISECOND,0);
        Timers.State old;synchronized(timers){old=timers.get(n.getKey());}
        long lastLeft=old==null?0:old.left(now);Timers.State next;
        CharSequence info=x.getCharSequence("android.ongoingActivityNoti.secondaryInfo");
        if(info!=null)next=Timers.fromText(info.toString(),no.when,paused,lastLeft,cal.getTimeInMillis(),now,title);
        else if(x.getBoolean(android.app.Notification.EXTRA_SHOW_CHRONOMETER)&&x.getBoolean("android.chronometerCountDown"))next=paused?null:Timers.fromCountdown(no.when,now,title);
        else next=null;
        synchronized(timers){if(next==null)timers.remove(n.getKey());else timers.put(n.getKey(),next);}
    }
    @Override public void onNotificationPosted(android.service.notification.StatusBarNotification n) {
        try{trackTimer(n);}catch(RuntimeException ignored){}
        String category=n.getNotification().category;if(android.app.Notification.CATEGORY_CALL.equals(category))trackCall(n);
        synchronized(alerts){alerts.remove(n.getKey());if(!notACall(n.getPackageName())&&(ringing(n.getNotification())||voicemail(n.getNotification())))alerts.put(n.getKey(),n);}
        if(!notACall(n.getPackageName())&&ringing(n.getNotification())&&Prefs.showAlerts(this))wakeForAlert();
    }
    /** A call or alarm is ringing: turn the screen on and bring Standby forward, so the frame is seen even when the display was off or Standby had been closed. */
    private void wakeForAlert(){
        try{
            if(!Instant.charging(this))return;   // hard rule: off the charger Standby never wakes the screen or opens, whatever is ringing
            android.os.PowerManager pm=(android.os.PowerManager)getSystemService(POWER_SERVICE);
            if(pm!=null&&checkSelfPermission(android.Manifest.permission.WAKE_LOCK)==android.content.pm.PackageManager.PERMISSION_GRANTED){
                @SuppressWarnings("deprecation") android.os.PowerManager.WakeLock w=pm.newWakeLock(android.os.PowerManager.FULL_WAKE_LOCK|android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP|android.os.PowerManager.ON_AFTER_RELEASE,"standby:alert");
                w.acquire(20000);
            }
            if(!Instant.showing&&Instant.overlayAllowed(this))startActivity(new android.content.Intent(this,StandbyActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK|android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP|android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION));
        }catch(RuntimeException ignored){}
    }
    /** Only a call or alarm that is ringing right now takes over the screen. Missed, upcoming and snoozed alarm notices share the alarm category but are just notices. */
    static boolean ringing(android.app.Notification no){
        String category=no.category;String ch=no.getChannelId()==null?"":no.getChannelId().toLowerCase(java.util.Locale.ROOT);
        // Samsung's Clock posts the ringing alarm or finished timer as an ongoing notice on a "firing" channel with no category when the screen is already on.
        if(ch.contains("firing")&&(no.flags&android.app.Notification.FLAG_ONGOING_EVENT)!=0)return true;
        if(!android.app.Notification.CATEGORY_CALL.equals(category)&&!android.app.Notification.CATEGORY_ALARM.equals(category))return false;
        if(ch.contains("missed")||ch.contains("upcoming")||ch.contains("snooze"))return false;
        return no.fullScreenIntent!=null||(no.flags&android.app.Notification.FLAG_ONGOING_EVENT)!=0;
    }
    /** The phone app's "new voicemail" notice. It gets its own frame, shown for a minute after it arrives. */
    static boolean voicemail(android.app.Notification no){
        if(android.app.Notification.CATEGORY_CALL.equals(no.category)||android.app.Notification.CATEGORY_ALARM.equals(no.category))return false;
        String ch=no.getChannelId()==null?"":no.getChannelId().toLowerCase(java.util.Locale.ROOT);
        android.os.Bundle x=no.extras;String t=x==null?"":String.valueOf(x.getCharSequence(android.app.Notification.EXTRA_TITLE)).toLowerCase(java.util.Locale.ROOT);
        return ch.contains("voicemail")||ch.contains("vvm")||t.contains("voicemail")||t.contains("voice mail");
    }
    /** A call that rang and then ended unanswered (so it went to voicemail). Shown for a minute on its own voicemail frame. */
    static final class Missed{final String title;final long when=System.currentTimeMillis();boolean dismissed;Missed(String t){title=t;}}
    /** Only a call from the phone app itself can go to the carrier's voicemail. Calls from WhatsApp, Telegram, Meet and the like ring on this phone but are answered or missed elsewhere too (a laptop, a watch). */
    private boolean carrierDialer(String pkg){
        if(pkg==null)return false;
        switch(pkg){case "com.samsung.android.incallui":case "com.samsung.android.dialer":case "com.android.incallui":case "com.google.android.dialer":case "com.android.dialer":case "com.android.server.telecom":case "com.android.phone":case "com.sec.phone":return true;default:}
        try{android.telecom.TelecomManager tm=(android.telecom.TelecomManager)getSystemService(TELECOM_SERVICE);if(tm!=null)return pkg.equals(tm.getDefaultDialerPackage());}catch(RuntimeException ignored){}
        return false;
    }
    /** Apps whose persistent "in a voice channel" notice is not a phone call and must not take over the screen. */
    private static boolean notACall(String pkg){return "com.discord".equals(pkg);}
    private Missed missed;private String ringKey,ringTitle;private boolean ringCarrier;private boolean answered;volatile boolean userEnded;
    Missed missed(){synchronized(alerts){return missed!=null&&!missed.dismissed&&System.currentTimeMillis()-missed.when<60000?missed:null;}}
    void dismissMissed(){synchronized(alerts){if(missed!=null)missed.dismissed=true;}}
    private int phoneState(){try{android.telephony.TelephonyManager t=(android.telephony.TelephonyManager)getSystemService(TELEPHONY_SERVICE);if(t!=null&&checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE)==android.content.pm.PackageManager.PERMISSION_GRANTED)return t.getCallState();}catch(RuntimeException ignored){}return -1;}
    private void trackCall(android.service.notification.StatusBarNotification n){
        int st=phoneState();synchronized(alerts){
            if(st==android.telephony.TelephonyManager.CALL_STATE_RINGING){if(!n.getKey().equals(ringKey)){ringKey=n.getKey();ringCarrier=carrierDialer(n.getPackageName());CharSequence t=n.getNotification().extras.getCharSequence(android.app.Notification.EXTRA_TITLE);ringTitle=t==null?"Unknown":t.toString();answered=false;userEnded=false;missed=null;}}
            else if(st==android.telephony.TelephonyManager.CALL_STATE_OFFHOOK)answered=true;}
    }
    private final java.util.HashSet<String> dismissed=new java.util.HashSet<>();
    void dismiss(android.service.notification.StatusBarNotification n){synchronized(alerts){dismissed.add(n.getKey()+n.getPostTime());}}
    @Override public void onNotificationRemoved(android.service.notification.StatusBarNotification n) { int st=phoneState();synchronized(alerts){alerts.remove(n.getKey());if(n.getKey().equals(ringKey)){if(ringCarrier&&!answered&&!userEnded&&st==android.telephony.TelephonyManager.CALL_STATE_IDLE)missed=new Missed(ringTitle);ringKey=null;}} synchronized(timers){timers.remove(n.getKey());} }
    android.service.notification.StatusBarNotification alert() {
        android.app.KeyguardManager km=(android.app.KeyguardManager)getSystemService(KEYGUARD_SERVICE);boolean locked=km!=null&&km.isDeviceLocked();
        android.service.notification.StatusBarNotification latest=null;
        android.service.notification.StatusBarNotification voice=null;long now=System.currentTimeMillis();
        synchronized(alerts){for(android.service.notification.StatusBarNotification n:alerts.values()){android.app.Notification no=n.getNotification();
            if(voicemail(no)){if(!dismissed.contains(n.getKey()+n.getPostTime())&&now-n.getPostTime()<60000)voice=n;continue;}
            boolean call=android.app.Notification.CATEGORY_CALL.equals(no.category);
            // A call always takes over: the phone's own call notice is marked private, so hiding it on a locked phone left the call frame blank.
            latest=n;}} // alerts only holds ringing calls, alarms and timers: none is private, and Standby sits over the lock screen, where the phone counts as locked
        if(latest==null)latest=voice;
        return latest;
    }
    @Override public void onListenerConnected(){instance=this;Instant.sync(this); synchronized(alerts){alerts.clear();} synchronized(timers){timers.clear();} try{android.service.notification.StatusBarNotification[] active=getActiveNotifications();if(active!=null)for(android.service.notification.StatusBarNotification n:active)onNotificationPosted(n);}catch(SecurityException ignored){} }
    @Override public void onListenerDisconnected(){if(instance==this)instance=null;synchronized(alerts){alerts.clear();}synchronized(timers){timers.clear();}
        try {
            requestRebind(new ComponentName(this, MediaListener.class));
        } catch (RuntimeException ignored) {
            // Android can decline the rebind when notification access has been revoked.
        }
    }
    MediaController controller(){
        try {
            MediaSessionManager manager=(MediaSessionManager)getSystemService(MEDIA_SESSION_SERVICE);
            List<MediaController> all=manager.getActiveSessions(new ComponentName(this,MediaListener.class));
            for(MediaController c:all){PlaybackState s=c.getPlaybackState();if(s!=null && s.getState()==PlaybackState.STATE_PLAYING)return c;}
            return all.isEmpty()?null:all.get(0);
        }catch(SecurityException e){return null;}
    }
    static String title(MediaController c){MediaMetadata m=c==null?null:c.getMetadata();return m==null?null:m.getString(MediaMetadata.METADATA_KEY_TITLE);}
    static String artist(MediaController c){MediaMetadata m=c==null?null:c.getMetadata();return m==null?null:m.getString(MediaMetadata.METADATA_KEY_ARTIST);}
    static Bitmap art(MediaController c){MediaMetadata m=c==null?null:c.getMetadata();if(m==null)return null;Bitmap b=m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);return b!=null?b:m.getBitmap(MediaMetadata.METADATA_KEY_ART);}
}
