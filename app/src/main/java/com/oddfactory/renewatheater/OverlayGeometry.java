package com.oddfactory.renewatheater;

final class OverlayGeometry {
 static float unit(float value){return Float.isFinite(value)?Math.max(0,Math.min(1,value)):0.5f;}
 static int position(int lo,int hi,int extent,float normalized){return lo+Math.round(Math.max(0,hi-lo-extent)*unit(normalized));}
 static float normalized(int position,int lo,int hi,int extent){return unit((position-lo)/(float)Math.max(1,hi-lo-extent));}
 static int clamp(int position,int lo,int hi,int extent){return Math.max(lo,Math.min(Math.max(lo,hi-extent),position));}
 static int size(int requested,int width,int height){return Math.max(1,Math.min(requested,Math.min(width,height)));}
}
