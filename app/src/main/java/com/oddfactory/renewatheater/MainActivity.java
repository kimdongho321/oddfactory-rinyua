package com.oddfactory.renewatheater;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity {
 private final com.oddfactory.update.UpdateCheck appUpdates =
         new com.oddfactory.update.UpdateCheck(this, TheaterUi::apply);
 boolean pendingStart=false; boolean notificationAsked=false;boolean details=false;boolean foreground=false,guideShowing=false;
 final Handler main=new Handler(Looper.getMainLooper());long consentDeadline;
 ArtStore previewArt;Bitmap previewActor;View artPreview;Thread previewLoader;boolean previewDestroyed;
 final Runnable resumeStart=new Runnable(){public void run(){if(!foreground||!pendingStart)return;if(DisplayAccessService.ready()){render();startTheater();}else if(SystemClock.uptimeMillis()<consentDeadline)main.postDelayed(this,250);}};
 static final int CREAM=0xfffff7e9,INK=0xff39282e,RED=0xff8d303e,TEAL=0xff25423f;
 int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
 static boolean usageAllowed(Context c){AppOpsManager a=(AppOpsManager)c.getSystemService(APP_OPS_SERVICE);return a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),c.getPackageName())==AppOpsManager.MODE_ALLOWED;}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(CREAM);getWindow().setNavigationBarColor(CREAM);}
 @Override public void onResume(){super.onResume(); appUpdates.resume();foreground=true;render();if(pendingStart){consentDeadline=SystemClock.uptimeMillis()+10000;main.post(resumeStart);}}
 @Override public void onPause(){foreground=false;main.removeCallbacks(resumeStart);appUpdates.pause(); super.onPause();}
 @Override public void onDestroy(){previewDestroyed=true;if(previewLoader!=null)previewLoader.interrupt();artPreview=null;previewArt=null;previewActor=null;pendingStart=false;main.removeCallbacksAndMessages(null);super.onDestroy();}
 @Override public void onBackPressed(){pendingStart=false;main.removeCallbacks(resumeStart);super.onBackPressed();}
 TextView label(String text,int size){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(INK);t.setTypeface(TheaterUi.font(this,false));t.setPadding(0,dp(5),0,dp(5));return t;}
 Button button(String text,Runnable r){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextColor(INK);b.setTypeface(TheaterUi.font(this,true));b.setOnClickListener(v->r.run());return b;}
 void render(){
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(CREAM);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(20),dp(22),dp(24));scroll.addView(root);
  scroll.setOnApplyWindowInsetsListener((view,insets)->{Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());root.setPadding(dp(22)+bars.left,dp(20)+bars.top,dp(22)+bars.right,dp(24)+bars.bottom);return insets;});
  root.addView(label("리뉴아 극장 알바",26));root.addView(label("매표소에서 앱을 고르면 검표 후 상영해요.",15));
  Button execute=button("매표소 실행",()->{pendingStart=true;startTheater();});execute.setTextColor(CREAM);execute.setTextSize(22);execute.setBackground(TheaterUi.panel(this,TEAL));LinearLayout.LayoutParams primary=new LinearLayout.LayoutParams(-1,dp(70));primary.setMargins(0,dp(12),0,dp(8));root.addView(execute,primary);
  root.addView(label(DisplayAccessService.ready()&&(!TheaterApps.prefs(this).getBoolean("homeOnly",true)||usageAllowed(this))?"홈 화면에서 바로 시작할 수 있어요.":"처음 한 번만 아래 실행 권한을 설정해 주세요.",13));
  View preview=new View(this){@Override protected void onDraw(Canvas c){if(previewArt==null||previewActor==null)return;float s=Math.min(getWidth(),getHeight());float x=(getWidth()-s)/2;previewArt.drawBooth(c,previewActor,new RectF(x,0,x+s,s),0);}};
  artPreview=preview;preparePreview();
  preview.setContentDescription("리뉴아 매표소 미리보기");root.addView(preview,new LinearLayout.LayoutParams(-1,dp(182)));
  List<TheaterApps.App> selected=TheaterApps.chosen(this);StringBuilder names=new StringBuilder();for(TheaterApps.App app:selected){if(names.length()>0)names.append(" · ");names.append(app.name);}
  root.addView(button("상영할 앱 선택 ("+selected.size()+")",this::chooseApps));root.addView(label(names.length()>0?names.toString():"상영할 앱을 골라 주세요.",14));
  root.addView(button(details?"실행 권한 · 설정 접기":"실행 권한 · 설정 펼치기",()->{details=!details;render();}));
  LinearLayout settings=new LinearLayout(this);settings.setOrientation(LinearLayout.VERTICAL);settings.setVisibility(details?View.VISIBLE:View.GONE);root.addView(settings);
  settings.addView(button((DisplayAccessService.ready()?"✓ ":"1. ")+"리뉴아 캐릭터 표시",this::displayGuide));
  settings.addView(button((usageAllowed(this)?"✓ ":"2. ")+"홈 화면 감지 허용",()->{Intent i=new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS,Uri.parse("package:"+getPackageName()));try{startActivity(i);}catch(ActivityNotFoundException e){startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));}}));
  settings.addView(label("홈 감지는 화면에 매표소를 표시할 때만 사용해요. 앱 사용 기록을 저장하거나 전송하지 않아요.",13));
  Switch home=new Switch(this);home.setText("홈 화면에서만 표시");home.setTextColor(INK);home.setPadding(0,dp(8),0,dp(8));home.setChecked(TheaterApps.prefs(this).getBoolean("homeOnly",true));home.setOnCheckedChangeListener((b,on)->{TheaterApps.prefs(this).edit().putBoolean("homeOnly",on).apply();reload();});settings.addView(home);
  settings.addView(label("실행 연출: 필름 · 슬레이트 · 입장권 중 무작위",14));
  TextView size=label("매표소 크기",15);settings.addView(size);SeekBar seek=new SeekBar(this);seek.setMax(120);int value=Math.max(140,Math.min(260,TheaterApps.prefs(this).getInt("size",180)));seek.setProgress(value-140);size.setText("매표소 크기: "+value);
  seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean user){size.setText("매표소 크기: "+(p+140));}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){TheaterApps.prefs(MainActivity.this).edit().putInt("size",b.getProgress()+140).apply();reload();}});settings.addView(seek);
  root.addView(button("매표소 끄기",()->{pendingStart=false;main.removeCallbacks(resumeStart);if(TheaterService.live!=null)TheaterService.live.requestStop(true);else stopService(new Intent(this,TheaterService.class));}));
  root.addView(label("한 번 눌러 앱 선택 · 두 번 눌러 종료·설정\n길게 눌러 위치를 옮겨요.",13));
  root.addView(button("업데이트 확인", appUpdates::checkNow));
  TheaterUi.apply(root);setContentView(scroll);
 }
 void preparePreview(){
  if(previewLoader!=null||previewDestroyed)return;Context assets=getApplicationContext();
  previewLoader=new Thread(()->{try{ArtStore a=ArtStore.get(assets);Bitmap actor=a.frame(a.base());if(Thread.currentThread().isInterrupted())return;main.post(()->{if(previewDestroyed)return;previewArt=a;previewActor=actor;if(artPreview!=null)artPreview.invalidate();});}catch(Exception ignored){}},"theater-preview");previewLoader.start();
 }
 void reload(){if(TheaterService.live!=null)TheaterService.live.reload();}
 void displayGuide(){
  if(guideShowing)return;guideShowing=true;
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("리뉴아 캐릭터 표시")
   .setMessage("리뉴아 그림과 몸 안쪽 조작 영역을 표시하는 권한이에요. 화면 내용 읽기·터치 탐색·제스처 대신 실행은 사용하지 않아요.\n\n접근성 설정에서 설치된 앱 → 리뉴아 캐릭터 표시 → 사용을 직접 켠 뒤 돌아와 주세요. 종료하면 그림과 조작 영역을 함께 정리해요.")
   .setPositiveButton("접근성 설정 열기",(d,w)->{try{startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}catch(ActivityNotFoundException e){pendingStart=false;Toast.makeText(this,"기기 설정에서 접근성 항목을 열어 주세요.",Toast.LENGTH_LONG).show();}})
   .setNegativeButton("나중에",(d,w)->pendingStart=false).create();
  dialog.setOnCancelListener(d->pendingStart=false);dialog.setOnDismissListener(d->guideShowing=false);
  dialog.setOnShowListener(d->TheaterUi.dialog(dialog));dialog.show();
 }
 void startTheater(){
  if(!foreground||!pendingStart)return;
  if(!DisplayAccessService.ready()){details=true;render();displayGuide();return;}
  if(TheaterApps.prefs(this).getBoolean("homeOnly",true)&&!usageAllowed(this)){details=true;render();Toast.makeText(this,"홈 화면 감지를 먼저 허용해 주세요.",Toast.LENGTH_LONG).show();return;}
  if(TheaterApps.chosen(this).isEmpty()){chooseApps();return;}
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED&&!notificationAsked){notificationAsked=true;requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},40);return;}
  pendingStart=false;startForegroundService(new Intent(this,TheaterService.class));startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
 }
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==40&&pendingStart&&foreground)startTheater();}
 void chooseApps(){
  List<TheaterApps.App> all=TheaterApps.available(this),shown=new ArrayList<>(all);LinkedHashSet<String> chosen=new LinkedHashSet<>(TheaterApps.selected(this));
  LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),0,dp(14),0);
  EditText search=new EditText(this);search.setSingleLine(true);search.setHint("앱 이름 검색");box.addView(search);
  ListView list=new ListView(this);box.addView(list,new LinearLayout.LayoutParams(-1,Math.min(dp(370),Math.round(getWindowManager().getCurrentWindowMetrics().getBounds().height()*.45f))));
  BaseAdapter adapter=new BaseAdapter(){public int getCount(){return shown.size();}public Object getItem(int p){return shown.get(p);}public long getItemId(int p){return p;}
   public View getView(int p,View convert,ViewGroup parent){TheaterApps.App app=shown.get(p);CheckBox check=new CheckBox(MainActivity.this);check.setPadding(dp(8),dp(8),dp(8),dp(8));check.setText(app.name);check.setTextColor(INK);check.setTypeface(TheaterUi.font(MainActivity.this,false));check.setChecked(chosen.contains(app.pkg));check.setClickable(false);check.setFocusable(false);return check;}};
  list.setAdapter(adapter);list.setOnItemClickListener((p,v,pos,id)->{String pkg=shown.get(pos).pkg;if(!chosen.remove(pkg))chosen.add(pkg);adapter.notifyDataSetChanged();});
  search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){shown.clear();String q=s.toString().toLowerCase(Locale.ROOT);for(TheaterApps.App app:all)if(app.name.toLowerCase(Locale.ROOT).contains(q))shown.add(app);adapter.notifyDataSetChanged();}public void afterTextChanged(Editable e){}});
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("매표소에 놓을 앱").setView(box).setNegativeButton("취소",(d,w)->pendingStart=false).setPositiveButton("저장",(d,w)->{TheaterApps.save(this,new ArrayList<>(chosen));reload();render();if(pendingStart&&!chosen.isEmpty())startTheater();else pendingStart=false;}).create();dialog.setOnCancelListener(d->pendingStart=false);dialog.setOnShowListener(d->TheaterUi.dialog(dialog));dialog.show();
 }
}
