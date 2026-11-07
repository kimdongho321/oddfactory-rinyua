package com.oddfactory.renewatheater;

import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import org.json.*;
import java.util.*;

final class TheaterApps {
 static final String PREF="renewa";
 static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
 static final String[] DEFAULTS={"com.sec.android.app.camera","com.sec.android.gallery3d","com.sec.android.app.clockpackage"};
 static final class App {final String pkg,name;Bitmap icon;App(String p,String n){pkg=p;name=n;}}
 static List<String> selected(Context c){List<String> out=new ArrayList<>();String saved=prefs(c).getString("apps",null);
  try{if(saved!=null){JSONArray a=new JSONArray(saved);for(int i=0;i<a.length();i++)out.add(a.getString(i));return out;}}catch(JSONException ignored){}
  return new ArrayList<>(Arrays.asList(DEFAULTS));}
 static void save(Context c,List<String> packages){prefs(c).edit().putString("apps",new JSONArray(packages).toString()).apply();}
 static List<App> chosen(Context c){List<App> out=new ArrayList<>();PackageManager pm=c.getPackageManager();for(String pkg:selected(c))try{
  if(pm.getLaunchIntentForPackage(pkg)!=null)out.add(new App(pkg,pm.getApplicationLabel(pm.getApplicationInfo(pkg,0)).toString()));
 }catch(PackageManager.NameNotFoundException ignored){}return out;}
 static List<App> available(Context c){PackageManager pm=c.getPackageManager();Map<String,App> unique=new HashMap<>();Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
  for(ResolveInfo r:pm.queryIntentActivities(i,0)){String p=r.activityInfo.packageName;if(!p.equals(c.getPackageName()))unique.putIfAbsent(p,new App(p,r.loadLabel(pm).toString()));}
  List<App> out=new ArrayList<>(unique.values());out.sort(Comparator.comparing(a->a.name,java.text.Collator.getInstance(java.util.Locale.KOREA)));return out;}
 static Bitmap icon(Context c,App app){if(app.icon!=null)return app.icon;
  try{Drawable d=c.getPackageManager().getApplicationIcon(app.pkg);Bitmap b=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);Canvas cv=new Canvas(b);d.setBounds(0,0,128,128);d.draw(cv);app.icon=b;return b;}catch(Exception e){return null;}}
}
