package com.oddfactory.renewatheater;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;

final class SceneView extends View {
 final TheaterService host;
 SceneView(TheaterService h){super(h.display);host=h;setLayerType(View.LAYER_TYPE_HARDWARE,null);setClickable(true);}
 @Override protected void onDraw(Canvas c){long now=SystemClock.uptimeMillis();host.player.update(now);int frame=host.player.current(now);float closed=host.player.shutter(now);host.art.drawBooth(c,frame,new RectF(0,0,getWidth(),getHeight()),closed);if(host.closing){host.onClosingDraw(closed,now);if(!host.closedFrameDrawn)postInvalidateDelayed(host.player.nextFrameDelay(now));return;}host.scheduleTargets(frame,closed);if(host.player.launching)postInvalidateOnAnimation();else postInvalidateDelayed(host.player.nextFrameDelay(now));}
 @Override public boolean performClick(){return super.performClick();}
}

final class AppCard extends View {
 final TheaterApps.App app;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);final Bitmap icon;
 AppCard(Context c,TheaterApps.App a){super(c);app=a;icon=TheaterApps.icon(c,a);setClickable(true);}
 @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight(),u=h/70f;
  p.setColor(0xff25423f);c.drawRect(0,3*u,w,h-3*u,p);p.setColor(0xfff5e4b8);c.drawRect(9*u,12*u,w-9*u,h-12*u,p);
  for(float x=8*u;x<w-4*u;x+=20*u){c.drawRect(x,6*u,x+9*u,10*u,p);c.drawRect(x,h-10*u,x+9*u,h-6*u,p);}
  if(icon!=null){p.setFilterBitmap(true);c.drawBitmap(icon,null,new RectF(15*u,13*u,60*u,58*u),p);p.setFilterBitmap(false);}
  p.setColor(0xff29413d);p.setTypeface(TheaterUi.font(getContext(),true));p.setTextSize(14*u);
  String name=TextUtils.ellipsize(app.name,new android.text.TextPaint(p),Math.max(10,w-76*u),TextUtils.TruncateAt.END).toString();c.drawText(name,67*u,h/2-(p.ascent()+p.descent())/2,p);
 }
}

/** Exactly the three selected launch effects. Character art is drawn separately. */
final class EffectView extends View {
 final TheaterService host;final TheaterApps.App app;final String effect;final Bitmap icon;
 final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 EffectView(TheaterService h,TheaterApps.App a,String e){super(h.display);host=h;app=a;effect=e;icon=TheaterApps.icon(h,a);setClickable(true);}
 float progress(long t,float start,float length){return Math.max(0,Math.min(1,(t-start)/length));}
 float ease(float u){return 1-(float)Math.pow(1-u,3);}
 void fill(Canvas c,int color,float x,float y,float w,float h){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRect(x,y,x+w,y+h,p);}
 void stroke(Canvas c,int color,float x,float y,float w,float h,float line){p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(line);c.drawRect(x,y,x+w,y+h,p);p.setStyle(Paint.Style.FILL);}
 void text(Canvas c,String value,float x,float y,float size,int color){p.setColor(color);p.setTypeface(TheaterUi.font(getContext(),true));p.setTextSize(size);c.drawText(value,x,y-(p.ascent()+p.descent())/2,p);}
 void icon(Canvas c,float x,float y,float size){if(icon!=null){p.setFilterBitmap(true);c.drawBitmap(icon,null,new RectF(x,y,x+size,y+size),p);p.setFilterBitmap(false);}}
 void card(Canvas c,float x,float y,float w,float h){fill(c,0xfff8edcf,x,y,w,h);stroke(c,0xffb58b48,x,y,w,h,3);float size=Math.min(120,h);icon(c,x+4,y+(h-size)/2,size);text(c,app.name,x+size+14,y+h/2,25,0xff29413d);}
 @Override protected void onDraw(Canvas canvas){Canvas c=canvas;c.save();c.scale(getWidth()/340f,getHeight()/350f);long t=Math.min(1900,SystemClock.uptimeMillis()-host.launchAt);
  fill(c,0xfffff5df,0,0,340,350);text(c,app.name,28,40,27,0xff394d45);
  if("film".equals(effect)){
   float u=ease(progress(t,840,650)),offset=250*(1-u);c.save();c.clipRect(28,128,310,267);fill(c,0xff203d3a,28,128,282,139);
   float holes=(t<1490?offset:0)%26;for(float x=8+holes;x<340;x+=26){fill(c,0xffefe6c9,x,135,12,11);fill(c,0xffefe6c9,x,249,12,11);}card(c,38+offset,153,262,88);c.restore();
  }else if("slate".equals(effect)){
   float x=44,y=140,w=255,h=127;fill(c,0xff293d3b,x,y,w,h);stroke(c,0xffc69e5d,x,y,w,h,3);icon(c,x+7,y+14,111);text(c,app.name,x+127,y+66,25,0xfffff1cb);
   float angle=-(1-ease(progress(t,870,350)))*.5f;c.save();c.translate(x,y);c.rotate((float)(angle*180/Math.PI));fill(c,0xffeee0b9,0,-25,w,25);
   for(int sx=5;sx<w;sx+=42){Path shape=new Path();shape.moveTo(sx,0);shape.lineTo(sx+23,0);shape.lineTo(sx+37,-25);shape.lineTo(sx+14,-25);shape.close();p.setColor(0xff293d3b);c.drawPath(shape,p);}c.restore();
  }else if("ticket".equals(effect)){
   float u=ease(progress(t,960,480));fill(c,0xfff7e4b6,29,140,220,128);stroke(c,0xffbd975a,29,140,220,128,3);icon(c,25,135,119);text(c,app.name,145,196,25,0xff29413d);
   c.save();c.translate(248+u*33,140+u*32);c.rotate((float)(u*.18*180/Math.PI));fill(c,0xffe2c58c,0,0,55,128);stroke(c,0xffbd975a,0,0,55,128,3);c.restore();
   p.setColor(0xffa37e4a);p.setStrokeWidth(2);p.setPathEffect(new DashPathEffect(new float[]{6,6},0));c.drawLine(245,146,245,262,p);p.setPathEffect(null);
   if(t>=960){p.setColor(0xffa43c48);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawCircle(202,244,16,p);p.setStyle(Paint.Style.FILL);text(c,"입장",181,243,20,0xffa43c48);}
  }
  if(t>=1490)text(c,t>=1700?"상영 시작":"검표 완료",99,302,23,0xff7d6036);
  text(c,"취소",280,330,18,0xff786b70);c.restore();if(host.launching&&t<1900)postInvalidateOnAnimation();
 }
 @Override public boolean performClick(){return super.performClick();}
}
