package com.rohan.standby;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.service.notification.StatusBarNotification;
import android.text.TextPaint;
import android.text.TextUtils;

/** Call/alarm takeover. Calls get a contact disc, name and the right buttons for the state (ringing or on a call); alarms get a pill pair. */
final class CallFrame {
    private static final int OTHER = 0, END = 1, ACCEPT = 2;
    private static final int[] DISCS = {0xff3fb0ff, 0xff5e5ce6, 0xffff9f0a, 0xff30c859, 0xfff06cb4, 0xff64d2ff};
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF[] targets={new RectF(),new RectF(),new RectF()};
    private final PendingIntent[] intents=new PendingIntent[3];
    private final int[] kinds=new int[3];
    private final String[] labels=new String[3];
    private final Context context;
    private String renderedKey; private boolean ringing;
    private String contactKey,contactName; private Bitmap contactPhoto;
    CallFrame(Context context){this.context=context;}
    /** Ringing calls pulse, so they want frequent redraws; everything else once a second. */
    boolean animated(){return ringing;}
    void draw(Canvas c,float w,float h,StatusBarNotification sbn){
        for(int i=0;i<targets.length;i++){targets[i].setEmpty();intents[i]=null;kinds[i]=OTHER;labels[i]=null;}
        renderedKey=sbn.getKey();Notification n=sbn.getNotification();boolean call=Notification.CATEGORY_CALL.equals(n.category);
        paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        CharSequence title=n.extras.getCharSequence(Notification.EXTRA_TITLE),detail=n.extras.getCharSequence(Notification.EXTRA_TEXT);
        int count=0;if(n.actions!=null)for(Notification.Action a:n.actions){if(count>=3)break;if(a==null||a.actionIntent==null||a.getRemoteInputs()!=null)continue;intents[count]=a.actionIntent;labels[count]=a.title==null?"":a.title.toString();kinds[count]=kind(labels[count]);count++;}
        if(call)drawCall(c,w,h,n,sbn.getKey(),title==null?"Unknown":title.toString(),detail==null?"":detail.toString(),count);else{ringing=false;drawAlarm(c,w,h,n,title==null?"Alarm":title.toString(),detail,count);}
    }
    static int kind(String label){String t=label.toLowerCase(java.util.Locale.ROOT);
        if(t.contains("answer")||t.contains("accept"))return ACCEPT;
        if(t.contains("decline")||t.contains("reject")||t.contains("hang")||t.contains("end")||t.contains("disconnect")||t.contains("ignore"))return END;return OTHER;}
    /** The contact for a number, when the title is one and contacts are allowed. */
    private void resolve(String key,String title){
        if(key.equals(contactKey))return;contactKey=key;contactName=null;contactPhoto=null;
        if(context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS)!=android.content.pm.PackageManager.PERMISSION_GRANTED||!title.matches("[+()\\d\\s-]{3,}"))return;
        try(android.database.Cursor q=context.getContentResolver().query(android.net.Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,android.net.Uri.encode(title)),new String[]{android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME,android.provider.ContactsContract.PhoneLookup.PHOTO_URI},null,null,null)){
            if(q!=null&&q.moveToFirst()){contactName=q.getString(0);String uri=q.getString(1);if(uri!=null)try(java.io.InputStream in=context.getContentResolver().openInputStream(android.net.Uri.parse(uri))){contactPhoto=android.graphics.BitmapFactory.decodeStream(in);}}
        }catch(Exception ignored){}
    }
    private void drawCall(Canvas c,float w,float h,Notification n,String key,String title,String detail,int count){
        resolve(key,title);String name=contactName!=null?contactName:title;String sub=contactName!=null?title:detail;
        int type=n.extras.getInt("android.callType",0);boolean chrono=n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER);
        boolean hasAccept=false;for(int i=0;i<count;i++)if(kinds[i]==ACCEPT)hasAccept=true;
        boolean incoming=type==1||(type==0&&hasAccept&&!chrono);boolean ongoing=!incoming&&(type==2||chrono);ringing=incoming;long now=System.currentTimeMillis();
        // Buttons: always a way to end or decline, and accept when it rings; fall back to the phone's own call control when the notification has no actions.
        boolean canControl=context.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS)==android.content.pm.PackageManager.PERMISSION_GRANTED;
        if(canControl&&(count==0||!hasEnd(count))){if(count<3){kinds[count]=END;labels[count]=incoming?"Decline":"End";count++;}}
        if(canControl&&incoming&&!hasAccept&&count<3){kinds[count]=ACCEPT;labels[count]="Accept";count++;}
        if(count==0&&n.contentIntent!=null){intents[0]=n.contentIntent;kinds[0]=OTHER;labels[0]="Open phone";count=1;}
        int tint=incoming?0xff17314d:ongoing?0xff10301f:0xff1a1a1e;
        paint.setStyle(Paint.Style.FILL);paint.setShader(new RadialGradient(w*.22f,h*.40f,w*.8f,tint,0xff050507,Shader.TileMode.CLAMP));float o=BurnIn.OVERSCAN;c.drawRect(-w*o,-h*o,w*(1+o),h*(1+o),paint);paint.setShader(null);
        float ax=w*.245f,ay=h*.5f,ar=h*.265f;int disc=DISCS[Math.floorMod(name.hashCode(),DISCS.length)];
        if(incoming)for(int i=0;i<3;i++){float t=((now%2400)/2400f+i/3f)%1f;paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(h*.012f);paint.setColor((disc&0x00ffffff)|((int)(120*(1-t))<<24));c.drawCircle(ax,ay,ar*(1.04f+.32f*t),paint);}
        paint.setStyle(Paint.Style.FILL);paint.setColor(disc);c.drawCircle(ax,ay,ar,paint);
        Bitmap photo=contactPhoto!=null?contactPhoto:avatar(n);
        if(photo!=null){c.save();Path clip=new Path();clip.addCircle(ax,ay,ar,Path.Direction.CW);c.clipPath(clip);float s=Math.max(2*ar/photo.getWidth(),2*ar/photo.getHeight()),pw=photo.getWidth()*s,ph=photo.getHeight()*s;c.drawBitmap(photo,null,new RectF(ax-pw/2,ay-ph/2,ax+pw/2,ay+ph/2),paint);c.restore();}
        else ClockFrames.text(c,paint,initials(name),ax,ay,ar*.85f,0xffffffff,Fonts.UI_SEMI,Paint.Align.CENTER,ar*1.5f);
        float x=w*.485f,maxW=w*.43f;
        String state=incoming?"INCOMING CALL":ongoing?"ON CALL":"CALL";paint.setLetterSpacing(.14f);ClockFrames.text(c,paint,state,x,h*.215f,h*.05f,incoming?0xff64d2ff:0xff30d158,Fonts.UI_SEMI,Paint.Align.LEFT,maxW);paint.setLetterSpacing(0f);
        ClockFrames.text(c,paint,name,x,h*.35f,h*.112f,0xffffffff,Fonts.UI_SEMI,Paint.Align.LEFT,maxW);
        if(ongoing&&chrono&&n.when>0)ClockFrames.tabular(c,paint,Timers.format(now-n.when),x,h*.52f,h*.11f,0xe6ffffff,Fonts.ROUNDED_BOLD,maxW*.6f);
        else if(!sub.isEmpty())ClockFrames.text(c,paint,sub,x,h*.50f,h*.055f,0xb3ffffff,Fonts.UI_MED,Paint.Align.LEFT,maxW);
        float cy=h*.76f;int shown=0;int[] order=new int[count];for(int k=0;k<3;k++)for(int i=0;i<count;i++)if(kinds[i]==k)order[shown++]=i;
        float rowWidth=maxW, gap=Math.min(w*.05f,rowWidth*.09f), r=Math.min(h*.095f,(rowWidth-gap*Math.max(0,shown-1))/(Math.max(1,shown)*2f));
        float step=shown>1?(rowWidth-2*r)/(shown-1):0;
        for(int slot=0;slot<shown&&slot<3;slot++){int idx=order[slot];float cx=x+r+slot*step;targets[idx].set(cx-r,cy-r,cx+r,cy+r);
            int kd=kinds[idx];paint.setStyle(Paint.Style.FILL);paint.setColor(kd==END?0xffff3b30:kd==ACCEPT?0xff30c859:0x33ffffff);c.drawCircle(cx,cy,r,paint);
            if(kd==OTHER)voicemail(c,cx,cy,r);else handset(c,cx,cy,r,kd==END);
            String label=kd==END&&incoming?"Decline":kd==END?"End":kd==ACCEPT?"Accept":labels[idx]==null||labels[idx].isEmpty()?"More":labels[idx];
            ClockFrames.text(c,paint,label,cx,cy+r+h*.065f,h*.04f,0xf2ffffff,Fonts.UI_MED,Paint.Align.CENTER,Math.min(r*2.6f,shown>1?step*.94f:rowWidth));}
    }
    private boolean hasEnd(int count){for(int i=0;i<count;i++)if(kinds[i]==END)return true;return false;}
    private void handset(Canvas c,float x,float y,float r,boolean decline){
        c.save();paint.setStyle(Paint.Style.STROKE);paint.setColor(Color.WHITE);paint.setStrokeWidth(r*.26f);
        if(decline){c.drawArc(new RectF(x-r*.5f,y-r*.15f,x+r*.5f,y+r*.85f),205,130,false,paint);}
        else{c.rotate(135,x,y);c.drawArc(new RectF(x-r*.5f,y-r*.28f,x+r*.5f,y+r*.72f),205,130,false,paint);}
        paint.setStyle(Paint.Style.FILL);c.restore();
    }
    private void voicemail(Canvas c,float x,float y,float r){
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xffffffff);for(int i=-1;i<=1;i++)c.drawCircle(x+i*r*.3f,y,r*.07f,paint);
    }
    private void drawAlarm(Canvas c,float w,float h,Notification n,String title,CharSequence detail,int count){
        c.drawColor(Color.BLACK);paint.setStyle(Paint.Style.FILL);paint.setShader(new RadialGradient(w*.2f,h*.3f,w*.7f,new int[]{0xff4a2a08,0xff000000},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,paint);paint.setShader(null);
        ClockFrames.text(c,paint,"ALARM",w*.08f,h*.24f,h*.05f,ClockFrames.ORANGE,Fonts.UI_SEMI,Paint.Align.LEFT,w*.5f);
        ClockFrames.text(c,paint,title,w*.08f,h*.43f,h*.15f,0xffffffff,Fonts.UI_MED,Paint.Align.LEFT,w*.84f);
        if(detail!=null)ClockFrames.text(c,paint,detail.toString(),w*.08f,h*.57f,h*.06f,0xff8e8e93,Fonts.UI_MED,Paint.Align.LEFT,w*.84f);
        float gap=w*.025f,total=w*.84f,bw=count>0?(total-gap*(count-1))/count:total*.5f;int item=0;
        if(n.actions!=null)for(Notification.Action a:n.actions){if(item>=count)break;if(a==null||a.actionIntent==null||a.getRemoteInputs()!=null)continue;
            RectF r=targets[item];r.set(w*.08f+item*(bw+gap),h*.70f,w*.08f+item*(bw+gap)+bw,h*.86f);boolean primary=item==count-1;
            paint.setStyle(Paint.Style.FILL);paint.setColor(primary?ClockFrames.ORANGE:0xff2c2c2e);c.drawRoundRect(r,h*.08f,h*.08f,paint);
            ClockFrames.text(c,paint,a.title==null?"Open":a.title.toString(),r.centerX(),r.centerY(),h*.058f,primary?0xff000000:0xffffffff,Fonts.UI_SEMI,Paint.Align.CENTER,bw*.85f);item++;}
        if(count==0&&n.contentIntent!=null){targets[0].set(w*.08f,h*.70f,w*.40f,h*.86f);intents[0]=n.contentIntent;paint.setColor(0xff2c2c2e);c.drawRoundRect(targets[0],h*.08f,h*.08f,paint);ClockFrames.text(c,paint,"Open",targets[0].centerX(),targets[0].centerY(),h*.058f,0xffffffff,Fonts.UI_SEMI,Paint.Align.CENTER,w*.3f);}
    }
    private Bitmap avatar(Notification n){
        try{Icon icon=n.getLargeIcon();if(icon==null)return null;Drawable d=icon.loadDrawable(context);if(d==null)return null;int s=512;Bitmap b=Bitmap.createBitmap(s,s,Bitmap.Config.ARGB_8888);Canvas cv=new Canvas(b);d.setBounds(0,0,s,s);d.draw(cv);return b;}catch(RuntimeException e){return null;}
    }
    private static String initials(String name){String[] parts=name.trim().split("\\s+");String r="";for(int i=0;i<parts.length&&i<2;i++)if(!parts[i].isEmpty())r+=Character.toUpperCase(parts[i].charAt(0));return r.isEmpty()?"?":r;}
    boolean touch(float x,float y,StatusBarNotification sbn){
        if(sbn==null||!sbn.getKey().equals(renderedKey))return false;
        for(int i=0;i<targets.length;i++)if(targets[i].contains(x,y)){
            if(Notification.CATEGORY_CALL.equals(sbn.getNotification().category)&&kinds[i]!=OTHER&&telecom(kinds[i]==ACCEPT))return true;
            if(intents[i]!=null){try{intents[i].send();}catch(PendingIntent.CanceledException ignored){}return true;}
        }
        return false;
    }
    /** Answers or ends the call through the phone itself, which needs the Answer calls permission. */
    private boolean telecom(boolean accept){
        if(context.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return false;
        try{android.telecom.TelecomManager tm=(android.telecom.TelecomManager)context.getSystemService(Context.TELECOM_SERVICE);if(tm==null)return false;if(accept){tm.acceptRingingCall();return true;}return tm.endCall();}catch(SecurityException e){return false;}
    }
}
