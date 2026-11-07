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
        String category=n.getNotification().category;
        synchronized(alerts){alerts.remove(n.getKey());if(ringing(n.getNotification()))alerts.put(n.getKey(),n);}
    }
    /** Only a call or alarm that is ringing right now takes over the screen. Missed, upcoming and snoozed alarm notices share the alarm category but are just notices. */
    static boolean ringing(android.app.Notification no){
        String category=no.category;if(!android.app.Notification.CATEGORY_CALL.equals(category)&&!android.app.Notification.CATEGORY_ALARM.equals(category))return false;
        String ch=no.getChannelId()==null?"":no.getChannelId().toLowerCase(java.util.Locale.ROOT);
        if(ch.contains("missed")||ch.contains("upcoming")||ch.contains("snooze"))return false;
        return no.fullScreenIntent!=null||(no.flags&android.app.Notification.FLAG_ONGOING_EVENT)!=0;
    }
    @Override public void onNotificationRemoved(android.service.notification.StatusBarNotification n) { synchronized(alerts){alerts.remove(n.getKey());} synchronized(timers){timers.remove(n.getKey());} }
    android.service.notification.StatusBarNotification alert() {
        android.app.KeyguardManager km=(android.app.KeyguardManager)getSystemService(KEYGUARD_SERVICE);boolean locked=km!=null&&km.isDeviceLocked();
        android.service.notification.StatusBarNotification latest=null;
        synchronized(alerts){for(android.service.notification.StatusBarNotification n:alerts.values()){if(!locked||n.getNotification().visibility==android.app.Notification.VISIBILITY_PUBLIC)latest=n;}}
        return latest;
    }
    @Override public void onListenerConnected(){instance=this; synchronized(alerts){alerts.clear();} synchronized(timers){timers.clear();} try{android.service.notification.StatusBarNotification[] active=getActiveNotifications();if(active!=null)for(android.service.notification.StatusBarNotification n:active)onNotificationPosted(n);}catch(SecurityException ignored){} }
    @Override public void onListenerDisconnected(){if(instance==this)instance=null;synchronized(alerts){alerts.clear();}synchronized(timers){timers.clear();}}
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
