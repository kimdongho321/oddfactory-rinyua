package com.oddfactory.renewatheater;

import java.util.*;

/** Selects animation frames and runs the 1900 ms launch sequence. */
final class MotionPlayer {
 final ArtStore art; final Random random=new Random();
 ArtStore.Clip clip;long started=0;String idleKind="neutral",effect="film";boolean launching=false,closing=false;
 MotionPlayer(ArtStore a){art=a;}
 int current(long now){if(clip==null)return art.base();return clip.frame(Math.max(0,now-started));}
 boolean playing(long now){return clip!=null&&now-started<clip.duration;}
 void update(long now){
  if(launching||closing)return;
  if(clip==null||(!"00-idle-cycle".equals(clip.id)&&now-started>=clip.duration)){clip=art.motions.get("00-idle-cycle");started=now;}
  if("00-idle-cycle".equals(clip.id)){
   long elapsed=Math.max(0,now-started);if(elapsed>=clip.duration)started+=(elapsed/clip.duration)*clip.duration;
   idleKind=current(now)==7?"weight":"neutral";
  }
 }
 long nextFrameDelay(long now){if(clip==null)return 3000;int i=clip.index(elapsed(now)),end=0;for(int n=0;n<=i;n++)end+=clip.ms[n];return Math.max(16,end-elapsed(now));}
 void beginLaunch(long now){
  String[] effects={"film","slate","ticket"};effect=effects[random.nextInt(effects.length)];String ending="film".equals(effect)?"guide":"bow";
  ArtStore.Clip normal=art.flows.get("neutral-"+ending);int visible=current(now);
  if(visible==art.base()){clip=normal;}else{
   ArtStore.Clip active=clip;int idx=active==null?0:active.index(Math.max(0,now-started));
   ArrayList<Integer> entry=new ArrayList<>();entry.add(visible);
   if(active!=null)for(int i=idx+1;i<active.frames.length;i++)entry.add(active.frames[i]);
   // Keep current pose and continue its drawn return sequence before preparation.
   for(int i=1;i<6;i++)entry.add(normal.frames[i]);
   int count=entry.size(),tail=normal.frames.length-6;int[] frames=new int[count+tail],ms=new int[count+tail];
   for(int i=0;i<count;i++){frames[i]=entry.get(i);ms[i]=300/count+(i<300%count?1:0);}
   for(int i=0;i<tail;i++){frames[count+i]=normal.frames[6+i];ms[count+i]=normal.ms[6+i];}
   clip=new ArtStore.Clip("current-"+ending,frames,ms);
  }
  if(clip.duration!=1900)throw new IllegalStateException("launch must be1900ms");started=now;launching=true;
 }
 boolean beginClose(long now){
  ArtStore.Clip end=art.motions.get("07-close");
  if(end==null||end.shutters==null||end.shutters.length!=end.frames.length||end.shutters[end.shutters.length-1]!=1f)return false;
  clip=end;started=now;launching=false;closing=true;return true;
 }
 void finish(long now){clip=art.motions.get("00-idle-cycle");started=now;launching=false;closing=false;idleKind="neutral";}
 long elapsed(long now){return Math.max(0,now-started);}
 float shutter(long now){return clip!=null&&clip.shutters!=null?clip.shutters[clip.index(elapsed(now))]:0;}
}
