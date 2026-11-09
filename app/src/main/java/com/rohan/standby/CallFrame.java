package com.rohan.standby;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.service.notification.StatusBarNotification;
import android.text.TextPaint;
import android.text.TextUtils;

/** Call/alarm takeover. Calls get a contact disc, name and the right buttons for the state (ringing or on a call); alarms get a pill pair. */
final class CallFrame {
    private static final int OTHER = 0, END = 1, ACCEPT = 2, MUTE = 3, SPEAKER = 4, BLUETOOTH = 5, INCOMING = 0, ONGOING = 1, HOLD = 2, PLAIN = 3, LISTEN = 6, DISMISS = 7;
    private static final int[] DISCS = {0xff3fb0ff, 0xff5e5ce6, 0xffff9f0a, 0xff30c859, 0xfff06cb4, 0xff64d2ff};
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF[] targets={new RectF(),new RectF(),new RectF(),new RectF(),new RectF(),new RectF()};
    private final PendingIntent[] intents=new PendingIntent[6];
    private final int[] kinds=new int[6];private StatusBarNotification shownSbn;private String listenLabel="Listen";private boolean live,dragging;private float slide;private PendingIntent acceptIntent;private final RectF trackRect=new RectF();
    private final String[] labels=new String[6];
    private final Context context;
    private String renderedKey; private boolean ringing; private PendingIntent openIntent;
    private String contactKey,contactName,contactType; private Bitmap contactPhoto;
    CallFrame(Context context){this.context=context;}
    /** Ringing calls pulse, so they want frequent redraws; everything else once a second. */
    boolean animated(){return ringing||live;}
    void draw(Canvas c,float w,float h,StatusBarNotification sbn){
        for(int i=0;i<targets.length;i++){targets[i].setEmpty();intents[i]=null;kinds[i]=OTHER;labels[i]=null;}
        renderedKey=sbn.getKey();Notification n=sbn.getNotification();boolean call=Notification.CATEGORY_CALL.equals(n.category);
        paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        CharSequence title=n.extras.getCharSequence(Notification.EXTRA_TITLE),detail=n.extras.getCharSequence(Notification.EXTRA_TEXT);
        int count=0;if(n.actions!=null)for(Notification.Action a:n.actions){if(count>=3)break;if(a==null||a.actionIntent==null||a.getRemoteInputs()!=null)continue;intents[count]=a.actionIntent;labels[count]=a.title==null?"":a.title.toString();kinds[count]=kind(labels[count]);count++;}
        shownSbn=sbn;if(MediaListener.voicemail(n)){ringing=false;drawVoicemail(c,w,h,n,title==null?"":title.toString(),detail==null?"":detail.toString(),count);}else if(call)drawCall(c,w,h,n,sbn.getKey(),title==null?"Unknown":title.toString(),detail==null?"":detail.toString(),count);else{ringing=false;drawAlarm(c,w,h,n,title==null?"Alarm":title.toString(),detail,count);}
    }
    static int kind(String label){String t=label.toLowerCase(java.util.Locale.ROOT);
        if(t.contains("answer")||t.contains("accept"))return ACCEPT;
        if(t.contains("decline")||t.contains("reject")||t.contains("hang")||t.contains("end")||t.contains("disconnect")||t.contains("ignore"))return END;return OTHER;}
    /** The contact for a number, when the title is one and contacts are allowed. */
    private void resolve(String key,String title){
        if(key.equals(contactKey))return;contactKey=key;contactName=null;contactType=null;contactPhoto=null;
        if(context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS)!=android.content.pm.PackageManager.PERMISSION_GRANTED||!title.matches("[+()\\d\\s-]{3,}"))return;
        try(android.database.Cursor q=context.getContentResolver().query(android.net.Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,android.net.Uri.encode(title)),new String[]{android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME,android.provider.ContactsContract.PhoneLookup.PHOTO_URI,android.provider.ContactsContract.PhoneLookup.TYPE,android.provider.ContactsContract.PhoneLookup.LABEL},null,null,null)){
            if(q!=null&&q.moveToFirst()){contactName=q.getString(0);String uri=q.getString(1);try{contactType=String.valueOf(android.provider.ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(),q.getInt(2),q.getString(3)));}catch(RuntimeException ignored){}if(uri!=null)try(java.io.InputStream in=context.getContentResolver().openInputStream(android.net.Uri.parse(uri))){contactPhoto=android.graphics.BitmapFactory.decodeStream(in);}}
        }catch(Exception ignored){}
    }
    /** Everything the call screen shows, so it can be drawn from a real call or from test data. */
    static final class Call {
        String name="Unknown",number="",typeLabel,sub,btName; int state=PLAIN; long since; Bitmap photo; boolean muted,speaker,bluetooth,btAvailable,canEnd=true;
    }
    private void drawCall(Canvas c,float w,float h,Notification n,String key,String title,String detail,int count){
        resolve(key,title);
        PendingIntent endI=null,acceptI=null,openI=n.contentIntent;for(int i=0;i<count;i++){if(kinds[i]==END&&endI==null)endI=intents[i];if(kinds[i]==ACCEPT&&acceptI==null)acceptI=intents[i];}
        int type=n.extras.getInt("android.callType",0);boolean chrono=n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER);
        int phone=-1;try{android.telephony.TelephonyManager tel=(android.telephony.TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);if(tel!=null&&context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE)==android.content.pm.PackageManager.PERMISSION_GRANTED)phone=tel.getCallState();}catch(RuntimeException ignored){}
        // Samsung's ringing notice has no Accept action and reuses the "ongoing" channel, so ask the phone itself whether it is ringing.
        boolean hasAccept=acceptI!=null;boolean incoming=phone==android.telephony.TelephonyManager.CALL_STATE_RINGING||type==1||(type==0&&hasAccept&&!chrono);boolean ongoing=!incoming&&(type==2||chrono||phone==android.telephony.TelephonyManager.CALL_STATE_OFFHOOK);
        boolean canControl=context.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS)==android.content.pm.PackageManager.PERMISSION_GRANTED;
        Call k=new Call();k.name=contactName!=null?contactName:title;k.number=contactName!=null?title:"";k.typeLabel=contactType;
        CharSequence subText=n.extras.getCharSequence(Notification.EXTRA_SUB_TEXT);String extra=subText==null?"":subText.toString();
        k.sub=!detail.isEmpty()&&!detail.matches("(?i)\\s*(ongoing|incoming|active|current|calling|dialling|dialing)?\\s*(call|voice call)?\\.{0,3}\\s*")&&!detail.equalsIgnoreCase(k.number)&&!detail.equalsIgnoreCase(k.name)?(extra.isEmpty()?detail:detail+"  ·  "+extra):extra;
        k.state=incoming?INCOMING:detail.toLowerCase(java.util.Locale.ROOT).contains("hold")?HOLD:ongoing?ONGOING:PLAIN;k.since=chrono&&n.when>0?n.when:0;
        k.photo=contactPhoto!=null?contactPhoto:avatar(n);k.canEnd=canControl||endI!=null;readAudio(k);
        for(int i=0;i<kinds.length;i++){targets[i].setEmpty();intents[i]=null;kinds[i]=OTHER;labels[i]=null;}
        render(c,w,h,k,endI,acceptI,openI,canControl);
    }
    // ───────── scene backgrounds: the same five looks as the alarm, so a call feels like part of the same screen ─────────
    static int themeOverride = -1;
    private static final int[][] SCENE = {{0xff1c1207, 0xff080604, 0xffff9f0a}, {0xff1b0a14, 0xff4a1a10, 0xffff7a45}, {0xff0c0a1f, 0xff2b1745, 0xffb18cff}, {0xff03060f, 0xff0a1730, 0xff7fb2ff}, {0xff1a050b, 0xff040102, 0xffb3261e}};
    private int sceneTheme() { return themeOverride >= 0 ? themeOverride : alarmTheme(Prefs.alarmTheme(context), java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)); }
    /** Morning, Sunset, Evening, Night or Midnight behind the call, tinted toward the call's state colour, with a soft glow behind the contact. Scenery stays out of the text column. */
    private void callBackground(Canvas c, float w, float h, int tint) {
        int th = sceneTheme(); int[] sc = SCENE[th]; float o = BurnIn.OVERSCAN; paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, 0, h, NowBar.mix(sc[0], tint, .50f), NowBar.mix(sc[1], tint, .20f), Shader.TileMode.CLAMP)); c.drawRect(-w * o, -h * o, w * (1 + o), h * (1 + o), paint); paint.setShader(null);
        int glow = NowBar.mix(tint, 0xffffffff, .30f); paint.setShader(new RadialGradient(w * .235f, h * .5f, h * .85f, (0x66 << 24) | (glow & 0xffffff), glow & 0x00ffffff, Shader.TileMode.CLAMP)); c.drawRect(-w * o, -h * o, w * (1 + o), h * (1 + o), paint); paint.setShader(null);
        if (th <= 1) { float sx = w * .84f, sy = h * 1.32f; float[] rad = {.62f, .48f, .36f}, al = {.03f, .05f, .08f}; for (int i = 0; i < 3; i++) { paint.setColor((((int) (255 * al[i])) << 24) | (sc[2] & 0xffffff)); c.drawCircle(sx, sy, h * rad[i], paint); } }
        else {
            float mx = w * .86f, my = h * .13f, mr = h * .045f;
            if (th == 2) { Path a = new Path(), q = new Path(); a.addCircle(mx, my, mr, Path.Direction.CW); q.addCircle(mx + mr * .5f, my - mr * .2f, mr * .92f, Path.Direction.CW); a.op(q, Path.Op.DIFFERENCE); paint.setColor(0xffe8e0ff); c.drawPath(a, paint); }
            else if (th == 3) { paint.setColor(0x14dfe8ff); c.drawCircle(mx, my, mr * 1.9f, paint); paint.setColor(0xffdfe8ff); c.drawCircle(mx, my, mr, paint); paint.setColor(0x22304060); c.drawCircle(mx - mr * .3f, my - mr * .2f, mr * .22f, paint); c.drawCircle(mx + mr * .35f, my + mr * .3f, mr * .15f, paint); }
            else { paint.setColor(0x24b3261e); c.drawCircle(mx, my, mr * 1.6f, paint); paint.setColor(0xff3a1216); c.drawCircle(mx, my, mr * .8f, paint); }
            if (th >= 2) for (int i = 0; i < 26; i++) { float fx = .02f + .96f * hash(i + 3), fy = .03f + .8f * hash(i + 31); if (fx > .46f && fx < .97f && fy > .08f && fy < .92f) continue; paint.setColor(((int) (255 * (.15f + .45f * hash(i + 70))) << 24) | 0xffffff); c.drawCircle(w * fx, h * fy, h * (.0035f + .004f * hash(i + 50)), paint); }
            if (th == 4) { float bx = 0; int bi = 0; while (bx < w) { float bw = w * (.028f + .03f * hash(bi + 90)), bh = h * (.02f + .06f * hash(bi + 60)); paint.setColor(0xff180609); c.drawRect(bx, h - bh, bx + bw - w * .003f, h, paint); bx += bw; bi++; } }
        }
    }
    private int accentAlpha(int accent,float a){return (accent&0x00ffffff)|((int)(255*Math.max(0f,Math.min(1f,a)))<<24);}
    /** The contact: photo, initials or a silhouette on a coloured disc. */
    private void contactDisc(Canvas c,float ax,float ay,float ar,Call k,int disc){
        paint.setStyle(Paint.Style.FILL);paint.setColor(disc);c.drawCircle(ax,ay,ar,paint);
        if(k.photo!=null){c.save();Path clip=new Path();clip.addCircle(ax,ay,ar,Path.Direction.CW);c.clipPath(clip);float sc=Math.max(2*ar/k.photo.getWidth(),2*ar/k.photo.getHeight()),pw=k.photo.getWidth()*sc,ph=k.photo.getHeight()*sc;c.drawBitmap(k.photo,null,new RectF(ax-pw/2,ay-ph/2,ax+pw/2,ay+ph/2),paint);c.restore();}
        else if(k.name.matches("(?s).*\\p{L}.*"))ClockFrames.text(c,paint,initials(k.name),ax,ay,ar*.85f,0xffffffff,Fonts.UI_SEMI,Paint.Align.CENTER,ar*1.5f);
        else silhouette(c,ax,ay,ar);
    }
    private void nameLine(Canvas c,String name,float x,float y,float maxW,float h,float size,float floor){
        paint.setTypeface(Fonts.UI_SEMI);float nsz=size;paint.setTextSize(nsz);float nw=paint.measureText(name);if(nw>maxW)nsz=Math.max(floor,nsz*maxW/nw);paint.setTextSize(nsz);String shown=name;
        if(paint.measureText(shown)>maxW)shown=TextUtils.ellipsize(shown,new TextPaint(paint),maxW,TextUtils.TruncateAt.END).toString();
        ClockFrames.text(c,paint,shown,x,y,nsz,0xffffffff,Fonts.UI_SEMI,Paint.Align.LEFT,maxW);
    }
    /** Draws the call screen. Ringing: "Orbit", the contact inside pulsing rings with Decline and a slide-to-answer track. On a call: "Voice", a live waveform beside the timer and one grouped row of controls with a separate End pill. */
    void render(Canvas c,float w,float h,Call k,PendingIntent endI,PendingIntent acceptI,PendingIntent openI,boolean canControl){
        boolean incoming=k.state==INCOMING;ringing=incoming;live=!incoming;long now=System.currentTimeMillis();openIntent=openI;acceptIntent=acceptI;
        for(int i=0;i<kinds.length;i++){targets[i].setEmpty();intents[i]=null;kinds[i]=OTHER;}
        int accent=incoming?0xff64d2ff:k.state==HOLD?0xffff9f0a:k.state==ONGOING?0xff30d158:0xff8e8e93,disc=DISCS[Math.floorMod(k.name.hashCode(),DISCS.length)];
        int tint=incoming?0xff10233a:k.state==HOLD?0xff2e2108:k.state==ONGOING?0xff0b2418:0xff1a1a1e;
        callBackground(c,w,h,tint);
        float ax=w*.235f,ay=h*.50f,x=w*.49f,maxW=w*.44f;
        if(incoming){
            float ar=h*.22f;float[] rr={1.26f,1.52f,1.80f},al={.50f,.26f,.12f};paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(h*.004f);
            for(int i=0;i<3;i++){paint.setColor(accentAlpha(accent,al[i]));c.drawCircle(ax,ay,ar*rr[i],paint);}
            float t=(now%2400)/2400f;paint.setColor(accentAlpha(accent,.6f*(1-t)));c.drawCircle(ax,ay,ar*(1.05f+.75f*t),paint);
            paint.setStyle(Paint.Style.FILL);paint.setColor(accent);double a1=now/3200.0,a2=-now/5200.0+2;
            c.drawCircle(ax+(float)Math.cos(a1)*ar*1.52f,ay+(float)Math.sin(a1)*ar*1.52f,h*.011f,paint);paint.setColor(accentAlpha(accent,.6f));c.drawCircle(ax+(float)Math.cos(a2)*ar*1.80f,ay+(float)Math.sin(a2)*ar*1.80f,h*.008f,paint);
            contactDisc(c,ax,ay,ar,k,disc);
            String eyebrow="INCOMING"+(k.typeLabel!=null&&!k.typeLabel.isEmpty()?"  ·  "+k.typeLabel.toUpperCase(java.util.Locale.ROOT):"");
            paint.setLetterSpacing(.14f);ClockFrames.text(c,paint,eyebrow,x,h*.17f,h*.04f,accent,Fonts.UI_SEMI,Paint.Align.LEFT,maxW);paint.setLetterSpacing(0f);
            nameLine(c,k.name,x,h*.30f,maxW,h,h*.105f,h*.072f);
            StringBuilder line=new StringBuilder(k.number.equals(k.name)?"":k.number);if(k.sub!=null&&!k.sub.isEmpty()){if(line.length()>0)line.append("  ·  ");line.append(k.sub);}
            if(line.length()>0)ClockFrames.text(c,paint,line.toString(),x,h*.41f,h*.048f,0xccffffff,Fonts.UI_MED,Paint.Align.LEFT,maxW);
            float cy=h*.70f,r=h*.078f;targets[0].set(x,cy-r,x+2*r,cy+r);kinds[0]=END;intents[0]=endI;
            paint.setStyle(Paint.Style.FILL);paint.setColor(0xffff3b30);c.drawCircle(x+r,cy,r,paint);glyph(c,R.drawable.ic_call_end,x+r,cy,r*1.05f,Color.WHITE);
            ClockFrames.text(c,paint,"Decline",x+r,cy+r+h*.05f,h*.036f,0xd9ffffff,Fonts.UI_MED,Paint.Align.CENTER,r*3);
            float tl=x+2*r+w*.014f,tr=x+maxW;trackRect.set(tl,cy-r,tr,cy+r);targets[1].set(trackRect);kinds[1]=ACCEPT;intents[1]=acceptI;
            paint.setColor(0x2630d158);c.drawRoundRect(trackRect,r,r,paint);
            float kr=r*.82f,kx=tl+r+(tr-tl-2*r)*slide;
            ClockFrames.text(c,paint,"Slide to answer",(tl+tr)/2f+kr*.5f,cy,h*.045f,accentAlpha(0xff30d158,1f-slide),Fonts.UI_MED,Paint.Align.CENTER,(tr-tl)-kr*3);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(h*.006f);paint.setStrokeCap(Paint.Cap.ROUND);for(int i=0;i<3;i++){float cx=tr-r*1.05f-i*h*.03f;Path ch=new Path();ch.moveTo(cx-h*.007f,cy-h*.016f);ch.lineTo(cx+h*.01f,cy);ch.lineTo(cx-h*.007f,cy+h*.016f);paint.setColor(accentAlpha(0xff30d158,(.9f-i*.28f)*(1f-slide)));c.drawPath(ch,paint);}
            paint.setStyle(Paint.Style.FILL);paint.setColor(0xff30d158);c.drawCircle(kx,cy,kr,paint);glyph(c,R.drawable.ic_call,kx,cy,kr*1.05f,Color.WHITE);
        }else{
            slide=0;float ar=h*.27f,br=(float)Math.sin(now/650.0)*.012f;
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(h*.008f);paint.setColor(accentAlpha(accent,.9f));c.drawCircle(ax,ay,ar*(1.10f+br),paint);
            contactDisc(c,ax,ay,ar,k,disc);
            String state=k.state==HOLD?"ON HOLD":k.state==ONGOING?"ON CALL":"CALL";
            paint.setLetterSpacing(.14f);ClockFrames.text(c,paint,state,x,h*.165f,h*.042f,accent,Fonts.UI_SEMI,Paint.Align.LEFT,maxW);paint.setLetterSpacing(0f);
            nameLine(c,k.name,x,h*.285f,maxW,h,h*.11f,h*.075f);
            StringBuilder line=new StringBuilder();if(k.typeLabel!=null&&!k.typeLabel.isEmpty())line.append(k.typeLabel);
            if(!k.number.isEmpty()&&!k.number.equals(k.name)){if(line.length()>0)line.append("  ·  ");line.append(k.number);}
            float y=h*.385f;if(line.length()>0){ClockFrames.text(c,paint,line.toString(),x,y,h*.048f,0xd9ffffff,Fonts.UI_MED,Paint.Align.LEFT,maxW);y+=h*.065f;}
            if(k.sub!=null&&!k.sub.isEmpty())ClockFrames.text(c,paint,k.sub,x,y,h*.038f,0x8cffffff,Fonts.UI_MED,Paint.Align.LEFT,maxW);
            float ty=h*.545f;float tw=0;if(k.since>0)tw=ClockFrames.tabular(c,paint,Timers.format(now-k.since),x,ty,h*.105f,0xf2ffffff,Fonts.ROUNDED_BOLD,maxW*.5f);
            // A waveform that stays alive while the call runs (decorative: Android doesn't let an app hear the call).
            float wx=x+Math.max(tw,maxW*.36f)+w*.014f,wr=x+maxW,n=20,step=(wr-wx)/n,bw=step*.55f;
            for(int i=0;i<n;i++){float amp=k.state==HOLD?h*.008f:h*.010f+h*.07f*Math.abs((float)(Math.sin(now/210.0+i*1.3)*Math.cos(now/530.0+i*.71)));
                paint.setStyle(Paint.Style.FILL);paint.setColor(accentAlpha(accent,k.state==HOLD?.35f:.9f));c.drawRoundRect(wx+i*step,ty-amp,wx+i*step+bw,ty+amp,bw/2,bw/2,paint);}
            float cy=h*.745f,rr=h*.062f,pad=h*.012f,gap=h*.008f;float gl=x,gr=gl+2*pad+3*2*rr+2*gap,gt=cy-rr-pad,gb=cy+rr+pad;
            paint.setStyle(Paint.Style.FILL);paint.setColor(0x17ffffff);c.drawRoundRect(gl,gt,gr,gb,(gb-gt)/2,(gb-gt)/2,paint);
            int[] slots={MUTE,SPEAKER,BLUETOOTH};
            for(int i=0;i<3;i++){int kd=slots[i];float bx=gl+pad+rr+i*(2*rr+gap);targets[i].set(bx-rr,cy-rr,bx+rr,cy+rr);kinds[i]=kd;
                boolean on=kd==MUTE?k.muted:kd==SPEAKER?k.speaker:k.bluetooth,off=kd==BLUETOOTH&&!k.btAvailable;
                paint.setStyle(Paint.Style.FILL);paint.setColor(on?0xfff2f2f7:off?0x0cffffff:0x14ffffff);c.drawCircle(bx,cy,rr,paint);
                int ink=on?0xff111114:off?0x59ffffff:Color.WHITE;glyph(c,kd==MUTE?(k.muted?R.drawable.ic_mic_off:R.drawable.ic_mic):kd==SPEAKER?R.drawable.ic_volume_up:R.drawable.ic_bluetooth,bx,cy,rr*1.0f,ink);}
            if(k.canEnd){float el=gr+w*.012f,er=x+maxW;targets[3].set(el,gt,er,gb);kinds[3]=END;intents[3]=endI;paint.setStyle(Paint.Style.FILL);paint.setColor(0xffff3b30);c.drawRoundRect(targets[3],(gb-gt)/2,(gb-gt)/2,paint);
                float mid=(el+er)/2f;glyph(c,R.drawable.ic_call_end,mid-h*.075f,cy,h*.062f,Color.WHITE);ClockFrames.text(c,paint,"End",mid+h*.035f,cy,h*.05f,Color.WHITE,Fonts.UI_SEMI,Paint.Align.CENTER,(er-el)*.4f);}
            String cap=k.speaker?"Speaker on":k.bluetooth?(k.btName!=null?"Bluetooth  ·  "+k.btName:"Bluetooth"):k.muted?"Microphone is muted":"";
            if(!cap.isEmpty())ClockFrames.text(c,paint,cap,x,h*.895f,h*.04f,0x99ffffff,Fonts.UI_MED,Paint.Align.LEFT,maxW);
        }
    }
    /** Touch for the call and alert frames. The ringing screen is dragged (slide to answer); everything else is a tap. */
    boolean onTouch(int action,float x,float y,StatusBarNotification sbn){
        if(sbn==null||!sbn.getKey().equals(renderedKey))return false;
        boolean call=Notification.CATEGORY_CALL.equals(sbn.getNotification().category)&&!MediaListener.voicemail(sbn.getNotification());
        if(call&&ringing){
            float r=trackRect.height()/2f,kr=r*.82f,kx=trackRect.left+r+(trackRect.width()-2*r)*slide;
            if(action==android.view.MotionEvent.ACTION_DOWN){dragging=Math.hypot(x-kx,y-trackRect.centerY())<=kr*1.5f;return true;}
            if(action==android.view.MotionEvent.ACTION_MOVE){if(dragging)slide=Math.max(0f,Math.min(1f,(x-trackRect.left-r)/(trackRect.width()-2*r)));return true;}
            if(action==android.view.MotionEvent.ACTION_CANCEL){dragging=false;slide=0;return true;}
            if(action==android.view.MotionEvent.ACTION_UP){
                if(dragging){dragging=false;boolean done=slide>.85f;slide=0;if(done&&!telecom(true)&&acceptIntent!=null)try{acceptIntent.send();}catch(PendingIntent.CanceledException ignored){}return true;}
                if(targets[0].contains(x,y))return touch(x,y,sbn);return true;
            }
            return true;
        }
        if(action==android.view.MotionEvent.ACTION_UP)return touch(x,y,sbn);return true;
    }
    // ───────── audio state and controls (best effort: Android lets the phone app own call audio) ─────────
    private AudioManager audio(){return (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);}
    private static boolean isBt(AudioDeviceInfo d){int t=d.getType();return t==AudioDeviceInfo.TYPE_BLUETOOTH_SCO||t==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP||t==AudioDeviceInfo.TYPE_HEARING_AID||(android.os.Build.VERSION.SDK_INT>=31&&t==AudioDeviceInfo.TYPE_BLE_HEADSET);}
    void readAudio(Call k){
        try{AudioManager am=audio();if(am==null)return;k.muted=am.isMicrophoneMute();
            if(android.os.Build.VERSION.SDK_INT>=31){AudioDeviceInfo d=am.getCommunicationDevice();k.speaker=(d!=null&&d.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)||(d==null&&am.isSpeakerphoneOn());k.bluetooth=d!=null&&isBt(d);if(k.bluetooth&&d.getProductName()!=null)k.btName=d.getProductName().toString();}
            else{k.speaker=am.isSpeakerphoneOn();k.bluetooth=am.isBluetoothScoOn();}
            for(AudioDeviceInfo d:am.getDevices(AudioManager.GET_DEVICES_OUTPUTS))if(isBt(d)){k.btAvailable=true;if(k.btName==null&&k.bluetooth&&d.getProductName()!=null)k.btName=d.getProductName().toString();}
            if(k.bluetooth)k.btAvailable=true;
        }catch(RuntimeException ignored){}
    }
    void toggleAudio(int kind){
        try{AudioManager am=audio();if(am==null)return;
            if(kind==MUTE){am.setMicrophoneMute(!am.isMicrophoneMute());return;}
            if(android.os.Build.VERSION.SDK_INT>=31){
                AudioDeviceInfo cur=am.getCommunicationDevice();boolean on=cur!=null&&(kind==SPEAKER?cur.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:isBt(cur));
                if(on){am.clearCommunicationDevice();if(kind==SPEAKER)am.setSpeakerphoneOn(false);return;}
                for(AudioDeviceInfo d:am.getAvailableCommunicationDevices())if(kind==SPEAKER?d.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:isBt(d)){if(am.setCommunicationDevice(d))return;break;}
                if(kind==SPEAKER){am.setSpeakerphoneOn(!am.isSpeakerphoneOn());return;}   // the phone app set speaker the old way, or refused the new way: use the old switch
            }else if(kind==SPEAKER)am.setSpeakerphoneOn(!am.isSpeakerphoneOn());
            else{if(am.isBluetoothScoOn()){am.stopBluetoothSco();am.setBluetoothScoOn(false);}else{am.startBluetoothSco();am.setBluetoothScoOn(true);}}
        }catch(RuntimeException ignored){}
    }
    /** Asks Android to switch the audio. Many phones (Samsung included) only let their own call app do that; when nothing changes, open that app so the real button is one tap away. */
    private void tryAudio(int kind){
        final Call before=new Call();readAudio(before);toggleAudio(kind);final PendingIntent open=openIntent;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{Call after=new Call();readAudio(after);
            if(before.speaker==after.speaker&&before.bluetooth==after.bluetooth&&before.muted==after.muted&&open!=null){
                try{open.send();}catch(PendingIntent.CanceledException ignored){}
                if(kind==SPEAKER)CallGuardService.tapAudio("Speaker");else if(kind==BLUETOOTH)CallGuardService.tapAudio("Bluetooth");}},500);
    }
    // ───────── icons, drawn on the canvas ─────────
    private void silhouette(Canvas c,float x,float y,float r){
        c.save();Path clip=new Path();clip.addCircle(x,y,r,Path.Direction.CW);c.clipPath(clip);paint.setStyle(Paint.Style.FILL);paint.setColor(0xe6ffffff);
        c.drawCircle(x,y-r*.16f,r*.27f,paint);c.drawOval(new RectF(x-r*.62f,y+r*.22f,x+r*.62f,y+r*1.25f),paint);c.restore();
    }
    /** Draws one of the vector icons (res/drawable/ic_*.xml) centred on x,y, tinted to the given colour. */
    private void glyph(Canvas c,int res,float x,float y,float size,int ink){
        Drawable d=context.getDrawable(res);if(d==null)return;d=d.mutate();d.setTint(ink);d.setBounds(Math.round(x-size/2),Math.round(y-size/2),Math.round(x+size/2),Math.round(y+size/2));d.draw(c);
    }
    /** Voicemail: its own frame. Violet disc with the voicemail icon, who it is from, when, and Listen / Call back / Dismiss. */
    private void drawVoicemail(Canvas c,float w,float h,Notification n,String title,String detail,int count){
        PendingIntent listen=n.contentIntent,call=null;String callLabel="Call back";
        for(int i=0;i<count;i++){String l=labels[i]==null?"":labels[i].toLowerCase(java.util.Locale.ROOT);if(l.contains("call")&&call==null){call=intents[i];callLabel=labels[i];}else if((l.contains("play")||l.contains("listen"))&&intents[i]!=null)listen=intents[i];}
        renderVoicemail(c,w,h,n.when,title,detail,listen,call,callLabel,count>0&&call!=null);
    }
    /** A call that rang out unanswered: the phone sent it to voicemail. Same frame, with a button to the call log. */
    void drawMissed(Canvas c,float w,float h,MediaListener.Missed m){
        resolve("missed"+m.when,m.title);String who=contactName!=null?contactName:m.title;
        PendingIntent log=null;try{android.content.Intent i=new android.content.Intent(android.content.Intent.ACTION_VIEW).setType("vnd.android.cursor.dir/calls").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);log=PendingIntent.getActivity(context,7,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);}catch(RuntimeException ignored){}
        ringing=false;listenLabel="Call log";renderVoicemail(c,w,h,m.when,who,"Call went to voicemail",log,null,"Call back",false);listenLabel="Listen";
    }
    boolean touchMissed(float x,float y){
        for(int i=0;i<targets.length;i++)if(targets[i].contains(x,y)){if(intents[i]!=null)try{intents[i].send();}catch(PendingIntent.CanceledException ignored){}if(MediaListener.instance!=null)MediaListener.instance.dismissMissed();return true;}
        return false;
    }
    void renderVoicemail(Canvas c,float w,float h,long when,String title,String detail,PendingIntent listen,PendingIntent call,String callLabel,boolean hasCall){
        for(int i=0;i<kinds.length;i++){targets[i].setEmpty();intents[i]=null;kinds[i]=OTHER;labels[i]=null;}
        live=false;boolean missedMode=!listenLabel.equals("Listen");int accent=0xffbf5af2;callBackground(c,w,h,0xff2a1840);
        float ax=w*.235f,ay=h*.50f,ar=h*.27f;
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(h*.008f);paint.setColor(accentAlpha(accent,.9f));c.drawCircle(ax,ay,ar*1.10f,paint);
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xff5e3bb5);c.drawCircle(ax,ay,ar,paint);glyph(c,R.drawable.ic_voicemail,ax,ay,ar*1.05f,Color.WHITE);
        float x=w*.49f,maxW=w*.44f;boolean generic=title.toLowerCase(java.util.Locale.ROOT).contains("voice");
        String who=generic&&!detail.isEmpty()?detail:title,sub=generic?(detail.isEmpty()||who.equals(detail)?"":detail):detail;if(who.isEmpty())who="New voicemail";
        { String[] parts = who.split("\\s+[\u00b7\u2022\u2013-]\\s+", 2); if (parts.length == 2 && who.length() > 16) { who = parts[0]; sub = sub.isEmpty() ? parts[1] : parts[1] + "  \u00b7  " + sub; } }
        String at=when>0?android.text.format.DateFormat.getTimeFormat(context).format(new java.util.Date(when)):"";
        paint.setLetterSpacing(.14f);ClockFrames.text(c,paint,"VOICEMAIL"+(at.isEmpty()?"":"  ·  "+at.toUpperCase(java.util.Locale.ROOT)),x,h*.165f,h*.042f,accent,Fonts.UI_SEMI,Paint.Align.LEFT,maxW);paint.setLetterSpacing(0f);
        nameLine(c,who,x,h*.29f,maxW,h,h*.105f,h*.07f);
        float py;
        if(!missedMode){
            float cy=h*.50f,r=h*.058f;targets[0].set(x,cy-r,x+2*r,cy+r);kinds[0]=LISTEN;intents[0]=listen;
            paint.setStyle(Paint.Style.FILL);paint.setColor(0xfff2f2f7);c.drawCircle(x+r,cy,r,paint);
            Path tri=new Path();tri.moveTo(x+r-r*.22f,cy-r*.38f);tri.lineTo(x+r+r*.42f,cy);tri.lineTo(x+r-r*.22f,cy+r*.38f);tri.close();paint.setColor(0xff111114);c.drawPath(tri,paint);
            float wx=x+2*r+w*.012f,wr=x+maxW,n=34,step=(wr-wx)/n,bw=step*.55f;
            for(int i=0;i<n;i++){float amp=h*.012f+h*.05f*Math.abs((float)(Math.sin(i*1.7)*Math.cos(i*.53)));paint.setColor(i<n*.3f?accent:0x40ffffff);c.drawRoundRect(wx+i*step,cy-amp,wx+i*step+bw,cy+amp,bw/2,bw/2,paint);}
            py=h*.665f;
        }else py=h*.50f;
        if(!sub.isEmpty())ClockFrames.text(c,paint,sub,x,py-(missedMode?0:h*.0f),h*.045f,0xb3ffffff,Fonts.UI_MED,Paint.Align.LEFT,maxW);
        int[] slots=missedMode?(hasCall?new int[]{LISTEN,OTHER,DISMISS}:new int[]{LISTEN,DISMISS}):(hasCall?new int[]{OTHER,DISMISS}:new int[]{DISMISS});
        int m=slots.length;float gap=w*.012f,bh=h*.12f,top=h*.77f,bwid=(maxW-gap*(m-1))/m;int base=missedMode?0:1;
        for(int i=0;i<m;i++){int ti=missedMode?i:(hasCall&&i==0?1:(hasCall?2:1));if(!missedMode&&slots[i]==OTHER)ti=1;if(!missedMode&&slots[i]==DISMISS)ti=hasCall?2:1;
            if(missedMode)ti=i;kinds[ti]=slots[i];intents[ti]=slots[i]==LISTEN?listen:slots[i]==OTHER?call:null;
            RectF r=targets[ti];r.set(x+i*(bwid+gap),top,x+i*(bwid+gap)+bwid,top+bh);boolean primary=slots[i]==LISTEN||(slots[i]==OTHER&&!missedMode);
            paint.setStyle(Paint.Style.FILL);paint.setColor(primary?accent:0x2effffff);c.drawRoundRect(r,bh/2,bh/2,paint);
            String label=slots[i]==LISTEN?listenLabel:slots[i]==DISMISS?"Dismiss":callLabel;
            ClockFrames.text(c,paint,label,r.centerX(),r.centerY(),h*.048f,primary?0xff12091c:0xffffffff,Fonts.UI_SEMI,Paint.Align.CENTER,bwid*.86f);}
    }
    /** Which alarm look to use: the setting, or by the hour (Morning 5-11, Sunset 12-18, Evening 19-21, Night 22-23, Midnight 0-4). */
    static int alarmTheme(int pref,int hour){if(pref>0)return pref-1;return hour>=5&&hour<12?0:hour>=12&&hour<19?1:hour>=19&&hour<22?2:hour>=22?3:4;}
    private void drawAlarm(Canvas c,float w,float h,Notification n,String title,CharSequence detail,int count){
        String ch=n.getChannelId()==null?"":n.getChannelId().toLowerCase(java.util.Locale.ROOT);String said=(title==null?"":title)+" "+(detail==null?"":detail);said=said.toLowerCase(java.util.Locale.ROOT);
        // Samsung's Clock rings alarms and timers on one channel ("alarm_and_timer"), so the channel name alone cannot tell them apart.
        boolean timer=ch.contains("timer")&&(!ch.contains("alarm")||said.contains("timer"));
        int primary=-1,snooze=-1;for(int i=0;i<count;i++){String l=labels[i]==null?"":labels[i].toLowerCase(java.util.Locale.ROOT);
            if(l.contains("snooze")&&snooze<0)snooze=i;else if((l.contains("dismiss")||l.contains("stop")||l.contains("off")||l.contains("done")||l.contains("cancel"))&&primary<0)primary=i;}
        if(primary<0)for(int i=count-1;i>=0;i--)if(i!=snooze){primary=i;break;}
        PendingIntent pi=primary>=0?intents[primary]:n.contentIntent,si=snooze>=0?intents[snooze]:null;
        String pl=primary>=0?labels[primary]:"Open",sl=snooze>=0?labels[snooze]:null;
        if(timer){renderTimerDone(c,w,h,title,pl);intents[0]=pi;return;}
        Context ctx=context;long now=System.currentTimeMillis();boolean h24=Prefs.is24(ctx);
        String time=new java.text.SimpleDateFormat("HH:mm",java.util.Locale.getDefault()).format(new java.util.Date(now)),date=new java.text.SimpleDateFormat("EEEE  ·  d MMM",java.util.Locale.getDefault()).format(new java.util.Date(now));
        int hour=java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        renderAlarm(c,w,h,alarmTheme(Prefs.alarmTheme(ctx),hour),title.isEmpty()||title.matches("[\\d:\\sAPMapm.]+")?"Alarm":title,time,date,pl,sl);
        intents[0]=pi;intents[1]=si;
    }
    /** Stars stay out of the time and the buttons so they never read as specks on the text. */
    private static boolean starOk(float x,float y){return !(x>.05f&&x<.64f&&y>.28f&&y<.80f)&&!(x>.64f&&x<.97f&&y>.26f&&y<.82f);}
    private static float hash(int i){float v=(float)Math.sin(i*12.9898)*43758.545f;return v-(float)Math.floor(v);}
    /** The alarm screen. Themes: 0 Morning, 1 Sunset, 2 Evening, 3 Night, 4 Midnight. The label sits above a very large time; Dismiss is the one big button. */
    void renderAlarm(Canvas c,float w,float h,int theme,String label,String time,String date,String primary,String snooze){
        ringing=false;live=true;for(int i=0;i<kinds.length;i++){targets[i].setEmpty();kinds[i]=OTHER;}
        int[][] pal={{0xff1c1207,0xff080604,0xffff9f0a,0xff1a0f00,0xffffffff},{0xff1b0a14,0xff4a1a10,0xffff7a45,0xff2a0d05,0xffffffff},{0xff0c0a1f,0xff2b1745,0xffb18cff,0xff14082b,0xffffffff},{0xff03060f,0xff0a1730,0xff7fb2ff,0xff04132e,0xffffffff},{0xff1a050b,0xff040102,0xffb3261e,0xff0a0000,0xffc4c4c4}};
        int[] pl=pal[Math.max(0,Math.min(4,theme))];int accent=pl[2],onAccent=pl[3],ink=pl[4];float o=BurnIn.OVERSCAN;
        paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(0,0,0,h,pl[0],pl[1],Shader.TileMode.CLAMP));c.drawRect(-w*o,-h*o,w*(1+o),h*(1+o),paint);paint.setShader(null);
        long now=System.currentTimeMillis();float pulse=.5f+.5f*(float)Math.sin(now/900.0);
        if(theme==0){float sx=w*.42f,sy=h*1.12f;float[] rad={.66f,.54f,.42f};float[] al={.05f,.09f,.18f};for(int i=0;i<3;i++){paint.setColor(accentAlpha(accent,al[i]+.03f*pulse));c.drawCircle(sx,sy,h*rad[i],paint);}paint.setColor(accent);c.drawCircle(sx,sy,h*.27f,paint);}
        else if(theme==1){float hy=h*.86f,sx=w*.42f;c.save();c.clipRect(0,0,w,hy);float[] rad={.60f,.44f,.30f};float[] al={.06f,.12f,.2f};for(int i=0;i<3;i++){paint.setColor(accentAlpha(0xffff6a3d,al[i]));c.drawCircle(sx,hy,h*rad[i],paint);}paint.setColor(0xffff6a3d);c.drawCircle(sx,hy,h*.19f,paint);c.restore();
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(h*.004f);paint.setColor(0x40ffffff);c.drawLine(w*.02f,hy,w*.98f,hy,paint);paint.setStyle(Paint.Style.FILL);for(int i=0;i<3;i++){paint.setColor(accentAlpha(0xffff6a3d,.5f-i*.14f));float half=h*(.18f-i*.05f);c.drawRoundRect(sx-half,hy+h*(.02f+i*.035f),sx+half,hy+h*(.033f+i*.035f),h*.01f,h*.01f,paint);}}
        else if(theme==2){paint.setShader(new RadialGradient(w*.5f,h*1.1f,h*.9f,0x33b18cff,0x00b18cff,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,paint);paint.setShader(null);
            float mx=w*.585f,my=h*.22f,mr=h*.075f;Path a=new Path(),b2=new Path();a.addCircle(mx,my,mr,Path.Direction.CW);b2.addCircle(mx+mr*.5f,my-mr*.2f,mr*.92f,Path.Direction.CW);a.op(b2,Path.Op.DIFFERENCE);paint.setColor(0xffe8e0ff);c.drawPath(a,paint);
            for(int i=0;i<16;i++){float fx=.03f+.94f*hash(i),fy=.04f+.5f*hash(i+9);if(!starOk(fx,fy))continue;paint.setColor(accentAlpha(0xffffffff,.25f+.35f*hash(i+40)));c.drawCircle(w*fx,h*fy,h*(.0035f+.004f*hash(i+20)),paint);}}
        else if(theme==3){float mx=w*.585f,my=h*.24f,mr=h*.085f;paint.setColor(0x14dfe8ff);c.drawCircle(mx,my,mr*1.9f,paint);paint.setColor(0xffdfe8ff);c.drawCircle(mx,my,mr,paint);paint.setColor(0x22304060);c.drawCircle(mx-mr*.3f,my-mr*.2f,mr*.22f,paint);c.drawCircle(mx+mr*.35f,my+mr*.3f,mr*.15f,paint);c.drawCircle(mx+mr*.15f,my-mr*.5f,mr*.1f,paint);
            for(int i=0;i<34;i++){float fx=.03f+.94f*hash(i+3),fy=.04f+.8f*hash(i+31);if(!starOk(fx,fy))continue;paint.setColor(accentAlpha(0xffffffff,.2f+.5f*hash(i+70)));c.drawCircle(w*fx,h*fy,h*(.0035f+.004f*hash(i+50)),paint);}}
        else{paint.setShader(new RadialGradient(w*.585f,h*.22f,h*.5f,0x30b3261e,0x00b3261e,Shader.TileMode.CLAMP));c.drawCircle(w*.585f,h*.22f,h*.5f,paint);paint.setShader(null);
            paint.setColor(0x24b3261e);c.drawCircle(w*.585f,h*.22f,h*.11f,paint);paint.setColor(0xff3a1216);c.drawCircle(w*.585f,h*.22f,h*.06f,paint);
            for(int i=0;i<14;i++){float fx=.03f+.94f*hash(i+5),fy=.04f+.55f*hash(i+15);if(!starOk(fx,fy))continue;paint.setColor(0x33ffffff);c.drawCircle(w*fx,h*fy,h*.003f,paint);}
            // A dim city at the bottom, so the screen has a scene but almost no light.
            float bx=0;int bi=0;while(bx<w){float bw=w*(.028f+.03f*hash(bi+90)),bh=h*(.05f+.12f*hash(bi+60));paint.setColor(0xff180609);c.drawRect(bx,h-bh,bx+bw-w*.003f,h,paint);
                for(int k=0;k<3;k++){if(hash(bi*7+k)>.55f){paint.setColor(0x55b3261e);c.drawRect(bx+bw*.2f+k*bw*.25f,h-bh+h*(.02f+.03f*hash(bi+k)),bx+bw*.2f+k*bw*.25f+w*.004f,h-bh+h*(.03f+.03f*hash(bi+k)),paint);}}
                bx+=bw;bi++;}}
        float x=w*.07f;glyph(c,R.drawable.ic_alarm,x+h*.025f,h*.19f,h*.05f,accent);
        paint.setLetterSpacing(.14f);ClockFrames.text(c,paint,label.toUpperCase(java.util.Locale.ROOT),x+h*.065f,h*.19f,h*.042f,accent,Fonts.UI_SEMI,Paint.Align.LEFT,w*.4f);paint.setLetterSpacing(0f);
        ClockFrames.tabular(c,paint,time,x,h*.50f,h*.34f,ink,Fonts.ROUNDED_BOLD,w*.55f);
        ClockFrames.text(c,paint,date,x,h*.76f,h*.05f,accentAlpha(ink,.72f),Fonts.UI_MED,Paint.Align.LEFT,w*.5f);
        float rx=w*.68f,rr=w*.27f;RectF d=targets[0];d.set(rx,h*.30f,rr+rx,h*.54f);paint.setStyle(Paint.Style.FILL);paint.setColor(accent);c.drawRoundRect(d,d.height()/2,d.height()/2,paint);
        ClockFrames.text(c,paint,primary==null?"Dismiss":primary,d.centerX(),d.centerY(),h*.078f,onAccent,Fonts.UI_SEMI,Paint.Align.CENTER,d.width()*.8f);
        if(snooze!=null){RectF s2=targets[1];s2.set(rx,h*.62f,rr+rx,h*.78f);paint.setColor(0x1fffffff);c.drawRoundRect(s2,s2.height()/2,s2.height()/2,paint);ClockFrames.text(c,paint,snooze,s2.centerX(),s2.centerY(),h*.055f,ink,Fonts.UI_MED,Paint.Align.CENTER,s2.width()*.8f);}
    }
    /** The timer finished: one pulse, then a single giant Stop button. */
    void renderTimerDone(Canvas c,float w,float h,String label,String stopLabel){
        ringing=false;live=true;for(int i=0;i<kinds.length;i++){targets[i].setEmpty();kinds[i]=OTHER;}
        int accent=0xff64d2ff;callBackground(c,w,h,0xff0a2a3a);
        long now=System.currentTimeMillis();float cx=w*.26f,cy=h*.5f,r=h*.33f,t=(now%2200)/2200f;
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(h*.058f);paint.setColor(accent);c.drawCircle(cx,cy,r,paint);
        paint.setStrokeWidth(h*.004f);paint.setColor(accentAlpha(accent,.6f*(1-t)));c.drawCircle(cx,cy,r*(1.1f+.28f*t),paint);
        ClockFrames.text(c,paint,"00:00",cx,cy,h*.16f,0xffffffff,Fonts.ROUNDED_BOLD,Paint.Align.CENTER,r*1.5f);
        float x=w*.54f,maxW=w*.38f;String l=label==null||label.isEmpty()||label.toLowerCase(java.util.Locale.ROOT).startsWith("timer")?"":"  ·  "+label.toUpperCase(java.util.Locale.ROOT);
        paint.setLetterSpacing(.14f);ClockFrames.text(c,paint,"TIME'S UP"+l,x,h*.28f,h*.042f,accent,Fonts.UI_SEMI,Paint.Align.LEFT,maxW);paint.setLetterSpacing(0f);
        ClockFrames.text(c,paint,"Timer finished",x,h*.40f,h*.095f,0xffffffff,Fonts.UI_SEMI,Paint.Align.LEFT,maxW);
        RectF d=targets[0];d.set(x,h*.56f,x+maxW,h*.56f+h*.22f);paint.setStyle(Paint.Style.FILL);paint.setColor(accent);c.drawRoundRect(d,d.height()/2,d.height()/2,paint);
        ClockFrames.text(c,paint,stopLabel==null?"Stop":stopLabel,d.centerX(),d.centerY(),h*.078f,0xff00212f,Fonts.UI_SEMI,Paint.Align.CENTER,d.width()*.8f);
    }
    private Bitmap avatar(Notification n){
        try{Icon icon=n.getLargeIcon();if(icon==null)return null;Drawable d=icon.loadDrawable(context);if(d==null)return null;int s=512;Bitmap b=Bitmap.createBitmap(s,s,Bitmap.Config.ARGB_8888);Canvas cv=new Canvas(b);d.setBounds(0,0,s,s);d.draw(cv);return b;}catch(RuntimeException e){return null;}
    }
    private static String initials(String name){String[] parts=name.trim().split("\\s+");String r="";for(int i=0;i<parts.length&&i<2;i++)if(!parts[i].isEmpty())r+=Character.toUpperCase(parts[i].charAt(0));return r.isEmpty()?"?":r;}
    boolean touch(float x,float y,StatusBarNotification sbn){
        if(sbn==null||!sbn.getKey().equals(renderedKey))return false;
        for(int i=0;i<targets.length;i++)if(targets[i].contains(x,y)){
            if(MediaListener.voicemail(sbn.getNotification())){if(intents[i]!=null)try{intents[i].send();}catch(PendingIntent.CanceledException ignored){}if(MediaListener.instance!=null)MediaListener.instance.dismiss(sbn);return true;}
            if(Notification.CATEGORY_CALL.equals(sbn.getNotification().category)){if(kinds[i]==MUTE||kinds[i]==SPEAKER||kinds[i]==BLUETOOTH){tryAudio(kinds[i]);return true;}if((kinds[i]==END||kinds[i]==ACCEPT)&&telecom(kinds[i]==ACCEPT))return true;}
            if(intents[i]!=null){try{intents[i].send();}catch(PendingIntent.CanceledException ignored){}return true;}
        }
        return false;
    }
    /** Answers or ends the call through the phone itself, which needs the Answer calls permission. */
    private boolean telecom(boolean accept){
        if(context.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return false;
        try{android.telecom.TelecomManager tm=(android.telecom.TelecomManager)context.getSystemService(Context.TELECOM_SERVICE);if(tm==null)return false;if(accept){tm.acceptRingingCall();return true;}if(MediaListener.instance!=null)MediaListener.instance.userEnded=true;return tm.endCall();}catch(SecurityException e){return false;}
    }
}
