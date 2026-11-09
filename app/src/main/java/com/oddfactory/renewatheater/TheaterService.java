package com.oddfactory.renewatheater;

import android.app.*;
import android.app.usage.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** User-started home-screen overlay with bounded windows and no network access. */
public final class TheaterService extends Service {
 static TheaterService live;
 final Handler main=new Handler(Looper.getMainLooper());
 final ExecutorService worker=Executors.newSingleThreadExecutor();
 ArtStore art;MotionPlayer player;WindowManager windows;DisplayAccessService display;SceneView scene;View panel;OpaqueTouchTargets targets;
 boolean targetUpdateQueued;int lastTargetFrame=-1,lastTargetSize,lastTargetX,lastTargetY;float lastTargetClosed=-1;
 WindowManager.LayoutParams boothLayout,panelLayout;
 boolean loaded=false,attached=false,launching=false,destroyed=false,homeOnly=true,closing=false,closedFrameDrawn=false;
 final Runnable closeEnd=this::finishStop;
 String foreground="",homePackage="com.sec.android.app.launcher";long lastUsageRead=0,lastForegroundEvent=0;
 TheaterApps.App pendingApp;Runnable launchTask;
 long launchAt;float downX,downY;int originX,originY;boolean dragged;
 Rect dragBounds;
 boolean receiverRegistered;
 final BroadcastReceiver screen=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent intent){updateVisibility();}};
 int dp(float n){return Math.round(n*(display!=null?display:this).getResources().getDisplayMetrics().density);}
 @Override public void onCreate(){super.onCreate();live=this;
  Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);ResolveInfo r=getPackageManager().resolveActivity(home,PackageManager.MATCH_DEFAULT_ONLY);if(r!=null)homePackage=r.activityInfo.packageName;
  NotificationManager nm=getSystemService(NotificationManager.class);NotificationChannel channel=new NotificationChannel("theater","매표소 실행",NotificationManager.IMPORTANCE_LOW);channel.setSound(null,null);channel.enableVibration(false);nm.createNotificationChannel(channel);
  PendingIntent settings=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,TheaterService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Notification n=new Notification.Builder(this,"theater").setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("리뉴아 극장 알바").setContentText("한 번 눌러 앱 선택 · 두 번 눌러 메뉴 · 길게 눌러 이동").setContentIntent(settings).setOngoing(true).addAction(new Notification.Action.Builder(null,"설정",settings).build()).addAction(new Notification.Action.Builder(null,"종료",stop).build()).build();
  if(Build.VERSION.SDK_INT>=34)startForeground(13,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(13,n);
  display=DisplayAccessService.current();if(display==null||!display.claim(this)){stopSelf();return;}windows=display.windows();
  IntentFilter screenEvents=new IntentFilter(Intent.ACTION_SCREEN_OFF);screenEvents.addAction(Intent.ACTION_SCREEN_ON);screenEvents.addAction(Intent.ACTION_USER_PRESENT);
  if(Build.VERSION.SDK_INT>=33)registerReceiver(screen,screenEvents,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(screen,screenEvents);receiverRegistered=true;
  targets=new OpaqueTouchTargets(display,this,new OpaqueTouchTargets.Callback(){public void single(){if(!attached)return;if(launching)cancelLaunch();else openPicker();}public void twice(){if(attached)openMenu();}public void dragStart(float x,float y){if(!attached)return;cancelLaunch();downX=x;downY=y;originX=boothLayout.x;originY=boothLayout.y;dragBounds=safe();dragged=true;}public void dragMove(float x,float y){if(!attached||!dragged)return;Rect b=safe();boothLayout.x=OverlayGeometry.clamp(originX+Math.round(x-downX),b.left,b.right,boothLayout.width);boothLayout.y=OverlayGeometry.clamp(originY+Math.round(y-downY),b.top,b.bottom,boothLayout.height);display.update(TheaterService.this,scene,boothLayout);scheduleTargets(player.current(SystemClock.uptimeMillis()),player.shutter(SystemClock.uptimeMillis()));}public void dragEnd(float x,float y,boolean cancel){if(dragged&&!destroyed)savePosition(cancel&&dragBounds!=null?dragBounds:safe());dragged=false;dragBounds=null;}});
  worker.execute(()->{try{ArtStore a=ArtStore.get(this);a.warm();main.post(()->{if(destroyed)return;art=a;player=new MotionPlayer(a);scene=new SceneView(this);scene.setContentDescription("리뉴아 매표소");loaded=true;reload();main.post(poll);});}
   catch(Exception e){Log.e("RenewaTheater","asset load",e);main.post(()->{Toast.makeText(this,"그림을 준비하지 못했어요.",Toast.LENGTH_LONG).show();stopSelf();});}});
 }
 @Override public int onStartCommand(Intent i,int flags,int id){
  if(i!=null&&"STOP".equals(i.getAction()))requestStop();
  else if(i!=null&&closing&&!destroyed){main.removeCallbacks(closeEnd);closing=closedFrameDrawn=false;player.finish(SystemClock.uptimeMillis());reload();main.post(poll);}
  return START_NOT_STICKY;
 }
 @Override public IBinder onBind(Intent i){return null;}
 Rect safe(){WindowMetrics m=windows.getCurrentWindowMetrics();Rect b=new Rect(m.getBounds());Insets i=m.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());return new Rect(b.left+i.left,b.top+i.top,b.right-i.right,b.bottom-i.bottom);}
 WindowManager.LayoutParams params(int w,int h){WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,PixelFormat.TRANSLUCENT);
  p.gravity=Gravity.TOP|Gravity.LEFT;p.windowAnimations=0;p.setFitInsetsTypes(0);p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;if(Build.VERSION.SDK_INT>=34)p.setCanPlayMoveAnimation(false);return p;}
 void reload(){if(!loaded||destroyed||closing)return;if(launching)cancelLaunch();homeOnly=TheaterApps.prefs(this).getBoolean("homeOnly",true);removePanel();targets.invalidateMask();lastTargetFrame=-1;
  Rect b=safe();int size=OverlayGeometry.size(dp(TheaterApps.prefs(this).getInt("size",180)),b.width(),b.height());
  if(boothLayout==null){boothLayout=params(size,size);boothLayout.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;boothLayout.alpha=1f;}boothLayout.width=size;boothLayout.height=size;
  float x=TheaterApps.prefs(this).getFloat("x",.72f),y=TheaterApps.prefs(this).getFloat("y",.40f);
  boothLayout.x=OverlayGeometry.position(b.left,b.right,size,x);boothLayout.y=OverlayGeometry.position(b.top,b.bottom,size,y);
  if(attached)display.update(this,scene,boothLayout);updateVisibility();}
 @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);if(closing){finishStop();return;}if(targets!=null)targets.setVisible(false);dragged=false;if(launching)cancelLaunch();reload();}
 boolean screenReady(){return getSystemService(PowerManager.class).isInteractive()&&!getSystemService(KeyguardManager.class).isKeyguardLocked();}
 boolean shouldShow(){if(!screenReady())return false;
  return !homeOnly||(MainActivity.usageAllowed(this)&&homePackage.equals(foreground));}
 void readForeground(){if(!MainActivity.usageAllowed(this))return;long now=System.currentTimeMillis();UsageStatsManager manager=getSystemService(UsageStatsManager.class);
  UsageEvents events=manager.queryEvents(Math.max(now-60000,lastUsageRead-500),now);UsageEvents.Event event=new UsageEvents.Event();
  if(events==null)return;while(events.hasNextEvent()){events.getNextEvent(event);if(event.getEventType()==UsageEvents.Event.ACTIVITY_RESUMED&&event.getTimeStamp()>=lastForegroundEvent){lastForegroundEvent=event.getTimeStamp();foreground=event.getPackageName();}}
  lastUsageRead=now;
 }
 void updateVisibility(){if(!loaded||destroyed)return;if(!display.allows(this)){displayLost(display);return;}if(closing){if(!screenReady())finishStop();return;}readForeground();boolean show=shouldShow();
  if(show&&!attached){try{display.attach(this,scene,boothLayout);attached=true;scene.invalidate();}
   catch(RuntimeException e){Log.e("RenewaTheater","overlay",e);stopSelf();}}
  if(!show&&attached){targets.invalidateMask();cancelLaunch();removePanel();display.remove(this,scene);attached=false;lastTargetFrame=-1;Log.i("RenewaTheater","HIDE");}
  if(show&&attached)targets.setVisible(true);
 }
 final Runnable poll=new Runnable(){public void run(){if(destroyed)return;updateVisibility();if(destroyed)return;if(attached&&scene!=null)scene.invalidate();main.postDelayed(this,attached?650:1100);}};
 void savePosition(Rect b){TheaterApps.prefs(this).edit().putFloat("x",OverlayGeometry.normalized(boothLayout.x,b.left,b.right,boothLayout.width)).putFloat("y",OverlayGeometry.normalized(boothLayout.y,b.top,b.bottom,boothLayout.height)).apply();}
 void scheduleTargets(int frame,float closed){if(!attached||destroyed||closing||targetUpdateQueued)return;if(lastTargetFrame==frame&&lastTargetSize==boothLayout.width&&lastTargetX==boothLayout.x&&lastTargetY==boothLayout.y&&lastTargetClosed==closed)return;targetUpdateQueued=true;main.post(()->{targetUpdateQueued=false;if(!attached||destroyed||closing)return;int current=player.current(SystemClock.uptimeMillis());float cover=player.shutter(SystemClock.uptimeMillis());List<Rect> rects=new ArrayList<>();for(Rect r:art.touchRects(current,boothLayout.width,cover)){Rect shifted=new Rect(r);shifted.offset(boothLayout.x,boothLayout.y);rects.add(shifted);}targets.update(rects);targets.setVisible(true);lastTargetFrame=current;lastTargetSize=boothLayout.width;lastTargetX=boothLayout.x;lastTargetY=boothLayout.y;lastTargetClosed=cover;});}
 void openMenu(){cancelLaunch();LinearLayout box=new LinearLayout(display);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(12),dp(10),dp(12),dp(10));box.setBackground(TheaterUi.panel(display,0xfffff5df));for(String title:new String[]{"앱 선택","설정","종료"}){Button b=new Button(display);b.setText(title);b.setAllCaps(false);box.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));b.setOnClickListener(v->{if(title.equals("종료"))requestStop();else if(title.equals("설정"))openSettings();else{removePanel();openPicker();}});}TheaterUi.apply(box);ScrollView scroll=new ScrollView(display);scroll.addView(box);showPanel(scroll,dp(220),dp(178));}
 void removePanel(){if(panel!=null){display.remove(this,panel);panel=null;panelLayout=null;}}
 void placePanel(){if(panel==null)return;Rect b=safe();int gap=dp(8);
  if(boothLayout.x-b.left>=panelLayout.width+gap){panelLayout.x=boothLayout.x-panelLayout.width-gap;panelLayout.y=boothLayout.y+boothLayout.height-panelLayout.height;}
  else if(b.right-(boothLayout.x+boothLayout.width)>=panelLayout.width+gap){panelLayout.x=boothLayout.x+boothLayout.width+gap;panelLayout.y=boothLayout.y+boothLayout.height-panelLayout.height;}
  else {panelLayout.x=boothLayout.x+boothLayout.width/2-panelLayout.width/2;panelLayout.y=boothLayout.y-panelLayout.height-gap;if(panelLayout.y<b.top)panelLayout.y=boothLayout.y+boothLayout.height+gap;}
  panelLayout.x=Math.max(b.left,Math.min(b.right-panelLayout.width,panelLayout.x));panelLayout.y=Math.max(b.top,Math.min(b.bottom-panelLayout.height,panelLayout.y));
  if(panel.isAttachedToWindow())display.update(this,panel,panelLayout);
 }
 void showPanel(View view,int w,int h){if(destroyed||!display.allows(this))return;removePanel();TheaterUi.apply(view);panel=view;Rect b=safe();panelLayout=params(Math.min(w,b.width()),Math.min(h,b.height()));placePanel();display.attach(this,panel,panelLayout);}
 void openPicker(){if(!attached)return;if(panel!=null){removePanel();return;}List<TheaterApps.App> apps=TheaterApps.chosen(this);
  LinearLayout box=new LinearLayout(display);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(8),dp(8),dp(8),dp(8));box.setBackgroundColor(0xfffff5df);
  LinearLayout header=new LinearLayout(display);TextView title=new TextView(display);title.setText("상영할 앱");title.setTextColor(0xff39282e);title.setTextSize(15);title.setGravity(Gravity.CENTER_VERTICAL);header.addView(title,new LinearLayout.LayoutParams(0,dp(40),1));
  Button settings=new Button(display);settings.setText("설정");settings.setTextSize(12);settings.setMinWidth(0);settings.setMinimumWidth(0);header.addView(settings,new LinearLayout.LayoutParams(dp(70),dp(40)));settings.setOnClickListener(v->openSettings());box.addView(header);
  ScrollView scroll=new ScrollView(display);LinearLayout rows=new LinearLayout(display);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);box.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  for(TheaterApps.App app:apps){AppCard card=new AppCard(display,app);rows.addView(card,new LinearLayout.LayoutParams(-1,dp(70)));card.setContentDescription(app.name+" 실행");card.setOnClickListener(v->selectApp(app));}
  if(apps.isEmpty()){TextView empty=new TextView(display);empty.setText("설정에서 앱을 골라 주세요.");empty.setTextColor(0xff39282e);rows.addView(empty);}
  showPanel(box,dp(226),dp(Math.min(330,56+Math.max(1,apps.size())*70)));Log.i("RenewaTheater","PICKER apps="+apps.size());
 }
 void openSettings(){cancelLaunch();removePanel();startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
 void selectApp(TheaterApps.App app){if(launching||!attached)return;long now=SystemClock.uptimeMillis();pendingApp=app;player.beginLaunch(now);launching=true;launchAt=now;
  EffectView effect=new EffectView(this,app,player.effect);effect.setContentDescription(app.name+" 상영 준비, 누르면 취소");effect.setOnClickListener(v->cancelLaunch());showPanel(effect,dp(226),dp(235));scene.invalidate();
  launchTask=()->{readForeground();if(!launching||!attached||!TheaterApps.selected(this).contains(app.pkg)||!shouldShow()){cancelLaunch();return;}
   Intent intent=getPackageManager().getLaunchIntentForPackage(app.pkg);if(intent==null){Toast.makeText(this,"이 앱을 실행할 수 없어요.",Toast.LENGTH_SHORT).show();cancelLaunch();return;}
   try{intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(intent);}
   catch(RuntimeException e){Log.e("RenewaTheater","App launch failed");Toast.makeText(this,"앱 실행을 다시 눌러 주세요.",Toast.LENGTH_LONG).show();}
   launching=false;player.finish(SystemClock.uptimeMillis());removePanel();scene.invalidate();main.postDelayed(this::updateVisibility,180);
  };main.postAtTime(launchTask,now+1900);
 }
 void cancelLaunch(){if(launchTask!=null){main.removeCallbacks(launchTask);launchTask=null;}if(launching){Log.i("RenewaTheater","LAUNCH_CANCEL");launching=false;if(player!=null)player.launching=false;}removePanel();if(scene!=null)scene.invalidate();}
 void displayLost(DisplayAccessService host){if(display!=host)return;cleanup();stopSelf();}
 void requestStop(){requestStop(false);}
 void requestStop(boolean fromSettings){
  if(destroyed||closing)return;
  if(!loaded||boothLayout==null||!screenReady()||!display.allows(this)||(!attached&&!fromSettings)){finishStop();return;}
  cancelLaunch();closing=true;targets.setVisible(false);targets.invalidateMask();
  if(!player.beginClose(SystemClock.uptimeMillis())){finishStop();return;}
  if(!attached){try{display.attach(this,scene,boothLayout);attached=true;}catch(RuntimeException error){finishStop();return;}}
  main.removeCallbacks(poll);main.postDelayed(closeEnd,player.clip.duration+3000);scene.invalidate();
 }
 void onClosingDraw(float shutter,long now){
  if(!closing||closedFrameDrawn||shutter<1f)return;
  closedFrameDrawn=true;main.removeCallbacks(closeEnd);main.postAtTime(closeEnd,Math.max(now+160,player.started+player.clip.duration));
 }
 private void finishStop(){cleanup();stopSelf();}
 private void cleanup(){if(destroyed)return;destroyed=true;if(targets!=null)targets.close();cancelLaunch();main.removeCallbacksAndMessages(null);if(receiverRegistered){unregisterReceiver(screen);receiverRegistered=false;}if(display!=null)display.release(this);attached=false;worker.shutdownNow();stopForeground(STOP_FOREGROUND_REMOVE);}
 @Override public void onDestroy(){cleanup();if(live==this)live=null;super.onDestroy();}
}
