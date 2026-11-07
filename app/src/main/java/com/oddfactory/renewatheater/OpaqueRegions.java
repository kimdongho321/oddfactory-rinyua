package com.oddfactory.renewatheater;

import java.util.ArrayList;
import java.util.List;

final class OpaqueRegions {
 static List<int[]> largest(boolean[] mask,int width,int height,int limit,int minimum){
  boolean[] work=mask.clone();List<int[]> out=new ArrayList<>();int[] heights=new int[width],stack=new int[width+1];
  for(int n=0;n<limit;n++){
   java.util.Arrays.fill(heights,0);int leftBest=0,topBest=0,widthBest=0,heightBest=0,area=0;
   for(int y=0;y<height;y++){
    for(int x=0;x<width;x++)heights[x]=work[y*width+x]?heights[x]+1:0;
    int top=0;for(int x=0;x<=width;x++){int h=x==width?0:heights[x];while(top>0&&heights[stack[top-1]]>h){int hh=heights[stack[--top]],left=top>0?stack[top-1]+1:0,ww=x-left;if(ww>=minimum&&hh>=minimum&&ww*hh>area){area=ww*hh;leftBest=left;topBest=y-hh+1;widthBest=ww;heightBest=hh;}}stack[top++]=x;}
   }
   if(area==0)break;int[] best={leftBest,topBest,widthBest,heightBest};out.add(best);for(int y=best[1];y<best[1]+best[3];y++)java.util.Arrays.fill(work,y*width+best[0],y*width+best[0]+best[2],false);
  }
  return out;
 }
}
