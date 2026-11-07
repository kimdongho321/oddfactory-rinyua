package com.oddfactory.renewatheater;

import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.List;

final class OpaqueTouchTargets {
 interface Callback {void single();void twice();void dragStart(float x,float y);void dragMove(float x,float y);void dragEnd(float x,float y,boolean cancel);}
 private final DisplayAccessService display;private final TheaterService owner;private final Callback callback;private final View[] views=new View[8];private final WindowManager.LayoutParams[] params=new WindowManager.LayoutParams[8];private final boolean[] attached=new boolean[8];
 private final GestureDetector detector;private List<Rect> rects=new ArrayList<>();private boolean visible,dragging,closed,down;private int owned=-1,tapTarget;private float x,y;
 OpaqueTouchTargets(DisplayAccessService c,TheaterService controller,Callback cb){display=c;owner=controller;callback=cb;
  detector=new GestureDetector(c,new GestureDetector.SimpleOnGestureListener(){@Override public boolean onDown(MotionEvent e){return true;}@Override public boolean onSingleTapConfirmed(MotionEvent e){if(!closed&&!dragging&&visible&&display.allows(owner))views[tapTarget].performClick();return true;}@Override public boolean onDoubleTap(MotionEvent e){if(!closed&&!dragging&&visible&&display.allows(owner))callback.twice();return true;}@Override public void onLongPress(MotionEvent e){if(down&&!closed&&visible&&display.allows(owner)){dragging=true;callback.dragStart(x,y);}}});
  for(int i=0;i<8;i++){final int index=i;views[i]=new View(c);views[i].setBackgroundColor(android.graphics.Color.TRANSPARENT);views[i].setContentDescription("매표소 조작 영역");views[i].setOnClickListener(v->{if(!closed&&visible&&display.allows(owner))callback.single();});views[i].setOnTouchListener((v,e)->touch(index,e));params[i]=new WindowManager.LayoutParams(1,1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);params[i].alpha=1f;params[i].gravity=Gravity.TOP|Gravity.LEFT;params[i].setFitInsetsTypes(0);params[i].layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;}
 }
 private boolean touch(int index,MotionEvent event){
  if(closed||!visible||!display.allows(owner))return false;int action=event.getActionMasked();
  if(action==MotionEvent.ACTION_POINTER_DOWN||event.getPointerCount()>1){cancel();apply();return true;}
  if(action==MotionEvent.ACTION_DOWN){if(down)return true;owned=index;tapTarget=index;down=true;}else if(!down||owned!=index)return true;
  x=event.getRawX();y=event.getRawY();MotionEvent global=MotionEvent.obtain(event);global.offsetLocation(x-event.getX(),y-event.getY());
  try{detector.onTouchEvent(global);}finally{global.recycle();}
  if(dragging&&action==MotionEvent.ACTION_MOVE)callback.dragMove(x,y);
  if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL||event.getPointerCount()>1){if(dragging){dragging=false;callback.dragEnd(x,y,action!=MotionEvent.ACTION_UP);}down=false;owned=-1;apply();}
  return true;
 }
 void update(List<Rect> latest){rects=new ArrayList<>();for(Rect r:latest)if(rects.size()<8&&!r.isEmpty())rects.add(new Rect(r));apply();}
 void setVisible(boolean value){if(visible==value)return;visible=value;if(!value)cancel();apply();}
 void invalidateMask(){if(closed)return;visible=false;cancel();rects.clear();apply();}
 private void cancel(){if(down){MotionEvent e=MotionEvent.obtain(0,0,MotionEvent.ACTION_CANCEL,x,y,0);detector.onTouchEvent(e);e.recycle();if(dragging){dragging=false;callback.dragEnd(x,y,true);}down=false;owned=-1;}}
 private void apply(){if(closed)return;for(int i=0;i<views.length;i++){if(down&&owned==i&&visible)continue;boolean show=visible&&display.allows(owner)&&i<rects.size();if(!show){if(attached[i]){display.remove(owner,views[i]);attached[i]=false;}continue;}Rect r=rects.get(i);WindowManager.LayoutParams p=params[i];p.x=r.left;p.y=r.top;p.width=r.width();p.height=r.height();if(attached[i])display.update(owner,views[i],p);else{display.attach(owner,views[i],p);attached[i]=true;}}}
 void close(){if(closed)return;closed=true;visible=false;cancel();for(int i=0;i<views.length;i++){if(attached[i])display.remove(owner,views[i]);attached[i]=false;}rects.clear();}
}
