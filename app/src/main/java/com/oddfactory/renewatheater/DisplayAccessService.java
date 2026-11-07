package com.oddfactory.renewatheater;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.res.Configuration;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.Map;

public final class DisplayAccessService extends AccessibilityService {
 private static DisplayAccessService current;
 private final DisplayLease lease=new DisplayLease();
 private final Map<View,TheaterService> owned=new IdentityHashMap<>();
 private WindowManager windows;
 static DisplayAccessService current(){return current!=null&&current.lease.ready()?current:null;}
 static boolean ready(){return current()!=null;}
 WindowManager windows(){return windows;}
 boolean claim(TheaterService owner){return current==this&&owner!=null&&owner==TheaterService.live&&!owner.destroyed&&lease.claim(owner);}
 boolean allows(TheaterService owner){return current==this&&owner!=null&&owner==TheaterService.live&&!owner.destroyed&&lease.allows(owner);}
 @Override protected void onServiceConnected(){
  super.onServiceConnected();AccessibilityServiceInfo info=getServiceInfo();
  if(info==null||info.getCapabilities()!=0)return;
  info.eventTypes=0;info.flags=0;setServiceInfo(info);
  if(current!=null)current.disconnect();
  windows=getSystemService(WindowManager.class);lease.connect();current=this;
 }
 void attach(TheaterService owner,View view,WindowManager.LayoutParams params){
  require(owner,params);if(owned.containsKey(view))throw new IllegalStateException("Window already attached");
  windows.addView(view,params);owned.put(view,owner);
 }
 void update(TheaterService owner,View view,WindowManager.LayoutParams params){
  require(owner,params);if(owned.get(view)!=owner)throw new IllegalStateException("Window owner missing");windows.updateViewLayout(view,params);
 }
 private void require(TheaterService owner,WindowManager.LayoutParams params){
  if(!allows(owner)||params.type!=WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY||params.alpha!=1f)throw new IllegalStateException("Display unavailable");
 }
 void remove(TheaterService owner,View view){if(owned.get(view)!=owner)return;owned.remove(view);try{windows.removeViewImmediate(view);}catch(IllegalArgumentException ignored){}}
 void release(TheaterService owner){lease.release(owner);for(View view:new ArrayList<>(owned.keySet()))if(owned.get(view)==owner)remove(owner,view);}
 private void disconnect(){
  TheaterService owner=(TheaterService)lease.disconnect();if(current==this)current=null;
  if(owner!=null)owner.displayLost(this);
  for(View view:new ArrayList<>(owned.keySet()))remove(owned.get(view),view);
 }
 @Override public void onAccessibilityEvent(AccessibilityEvent event){}
 @Override public void onInterrupt(){TheaterService owner=TheaterService.live;if(owner!=null&&lease.allows(owner))owner.displayLost(this);}
 @Override public void onConfigurationChanged(Configuration config){super.onConfigurationChanged(config);TheaterService owner=TheaterService.live;if(owner!=null&&allows(owner))owner.onConfigurationChanged(config);}
 @Override public boolean onUnbind(android.content.Intent intent){disconnect();return super.onUnbind(intent);}
 @Override public void onDestroy(){disconnect();super.onDestroy();}
}
