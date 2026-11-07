package com.oddfactory.renewatheater;

import android.content.Context;
import android.graphics.*;
import android.util.LruCache;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Original PNGs only. Density resampling, bitmap filters and tint are disabled. */
final class ArtStore {
 static ArtStore shared;
 static synchronized ArtStore get(Context c){if(shared==null)shared=new ArtStore(c.getApplicationContext());return shared;}
 final Context context; final JSONObject data;
 final LinkedHashMap<String,Clip> motions=new LinkedHashMap<>();
 final HashMap<String,Clip> flows=new HashMap<>();
 final Bitmap booth,shutter;
 final String[] paths;
 final LruCache<Integer,Bitmap> cache;
 final LruCache<String,List<Rect>> touchCache=new LruCache<>(24);
 static final class Clip {
  final String id; final int[] frames,ms; final int duration; final float[] shutters;
  Clip(String id,int[] frames,int[] ms){this.id=id;this.frames=frames;this.ms=ms;int sum=0;for(int v:ms)sum+=v;duration=sum;shutters=null;}
  Clip(JSONObject j)throws JSONException{ id=j.getString("id");frames=ints(j.getJSONArray("indices"));ms=ints(j.getJSONArray("durations"));duration=j.getInt("duration");
   JSONArray a=j.optJSONArray("shutter");shutters=a==null?null:new float[a.length()];if(a!=null)for(int i=0;i<a.length();i++)shutters[i]=(float)a.optDouble(i); }
  int index(long time){int n=0;for(int i=0;i<ms.length;i++){n+=ms[i];if(time<n)return i;}return frames.length-1;}
  int frame(long time){return frames[index(time)];}
 }
 static int[] ints(JSONArray a)throws JSONException{int[] out=new int[a.length()];for(int i=0;i<out.length;i++)out[i]=a.getInt(i);return out;}
 private ArtStore(Context c){context=c;try{
  byte[] json;try(InputStream in=c.getAssets().open("motion-data.json");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);json=out.toByteArray();}
  data=new JSONObject(new String(json,StandardCharsets.UTF_8));
  JSONArray p=data.getJSONArray("frames");paths=new String[p.length()];for(int i=0;i<p.length();i++)paths[i]=p.getString(i);
  JSONArray clips=data.getJSONArray("motions");for(int i=0;i<clips.length();i++){Clip clip=new Clip(clips.getJSONObject(i));motions.put(clip.id,clip);}
  JSONObject all=data.getJSONObject("flows");Iterator<String> keys=all.keys();while(keys.hasNext()){String key=keys.next();flows.put(key,new Clip(all.getJSONObject(key)));}
  int budget=(int)Math.min(128L*1024*1024,Math.max(48L*1024*1024,Runtime.getRuntime().maxMemory()/3));
  cache=new LruCache<Integer,Bitmap>(budget){@Override protected int sizeOf(Integer key,Bitmap b){return b.getAllocationByteCount();}};
  booth=decode("ui/booth.png");shutter=decode("ui/shutter.png");
 }catch(Exception e){throw new IllegalStateException("원본 그림을 읽지 못했어요",e);}}
 Bitmap decode(String path)throws IOException {BitmapFactory.Options o=new BitmapFactory.Options();o.inScaled=false;o.inPreferredConfig=Bitmap.Config.ARGB_8888;
  try(InputStream in=context.getAssets().open(path)){Bitmap b=BitmapFactory.decodeStream(in,null,o);if(b==null)throw new IOException(path);b.setDensity(Bitmap.DENSITY_NONE);return b;}}
 Bitmap frame(int index){Bitmap b=cache.get(index);if(b!=null)return b;try{b=decode(paths[index]);cache.put(index,b);return b;}catch(IOException e){throw new IllegalStateException(paths[index],e);}}
 void warm(){Set<Integer> indices=new LinkedHashSet<>();for(Clip c:flows.values())for(int i:c.frames)indices.add(i);
  for(int i:motions.get("00-idle-cycle").frames)indices.add(i);Clip close=motions.get("07-close");if(close!=null)for(int i:close.frames)indices.add(i);for(int i:indices){if(Thread.currentThread().isInterrupted())return;frame(i);}}
 int base(){return motions.get("10-neutral").frames[0];}
 void drawBooth(Canvas c,int frame,RectF r,float closed){
  drawBooth(c,frame(frame),r,closed);
 }
 void drawBooth(Canvas c,Bitmap actor,RectF r,float closed){
  float s=r.width()/1254f;Paint p=new Paint();p.setFilterBitmap(false);p.setDither(false);
  c.drawBitmap(actor,null,new RectF(r.left+147*s,r.top+208*s,r.left+1107*s,r.top+1008*s),p);
  if(closed>0){c.save();c.clipRect(r.left+202*s,r.top+284*s,r.left+1052*s,r.top+782*s);
   c.drawBitmap(shutter,null,new RectF(r.left+202*s,r.top+(284-498*(1-closed))*s,r.left+1052*s,r.top+(782-498*(1-closed))*s),p);c.restore();}
  c.drawBitmap(booth,null,r,p);
 }
 List<Rect> touchRects(int frame,int size,float closed){
  String key=frame+":"+size+":"+Math.round(closed*1000);List<Rect> found=touchCache.get(key);if(found!=null)return found;
  Bitmap body=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888),front=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);Paint p=new Paint();p.setFilterBitmap(false);float s=size/1254f;
  new Canvas(body).drawBitmap(frame(frame),null,new RectF(147*s,208*s,1107*s,1008*s),p);
  Canvas fc=new Canvas(front);if(closed>0){fc.save();fc.clipRect(202*s,284*s,1052*s,782*s);fc.drawBitmap(shutter,null,new RectF(202*s,(284-498*(1-closed))*s,1052*s,(782-498*(1-closed))*s),p);fc.restore();}fc.drawBitmap(booth,null,new RectF(0,0,size,size),p);
  int[] actor=new int[size*size],prop=new int[size*size];body.getPixels(actor,0,size,0,0,size,size);front.getPixels(prop,0,size,0,0,size,size);body.recycle();front.recycle();
  boolean[] opaque=new boolean[size*size],bodyOnly=new boolean[size*size];
  for(int i=0;i<opaque.length;i++){int a=actor[i]>>>24,f=prop[i]>>>24;opaque[i]=a>=248||f>=248;bodyOnly[i]=a>=248&&f<=8;}
  boolean[] safe=new boolean[opaque.length],safeBody=new boolean[opaque.length];for(int y=1;y<size-1;y++)for(int x=1;x<size-1;x++){int i=y*size+x;safe[i]=opaque[i]&&opaque[i-1]&&opaque[i+1]&&opaque[i-size]&&opaque[i+size];safeBody[i]=safe[i]&&bodyOnly[i];}
  List<int[]> chosen=OpaqueRegions.largest(safeBody,size,size,2,4);for(int[] r:chosen)for(int y=r[1];y<r[1]+r[3];y++)Arrays.fill(safe,y*size+r[0],y*size+r[0]+r[2],false);chosen.addAll(OpaqueRegions.largest(safe,size,size,8-chosen.size(),4));
  List<Rect> result=new ArrayList<>();for(int[] r:chosen)result.add(new Rect(r[0],r[1],r[0]+r[2],r[1]+r[3]));touchCache.put(key,result);return result;
 }
}
