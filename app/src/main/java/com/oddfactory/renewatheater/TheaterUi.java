package com.oddfactory.renewatheater;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

final class TheaterUi {
 private static Typeface light,bold;
 static synchronized Typeface font(Context c,boolean strong){if(strong){if(bold==null)bold=Typeface.createFromAsset(c.getAssets(),"fonts/Maplestory-Bold.ttf");return bold;}if(light==null)light=Typeface.createFromAsset(c.getAssets(),"fonts/Maplestory-Light.ttf");return light;}
 static void apply(View v){if(v instanceof TextView){TextView t=(TextView)v;t.setTypeface(font(v.getContext(),t.getTypeface()!=null&&t.getTypeface().isBold()));}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)apply(g.getChildAt(i));}}
 static void dialog(android.app.AlertDialog dialog){if(dialog.getWindow()!=null)apply(dialog.getWindow().getDecorView());}
 static GradientDrawable panel(Context c,int color){float d=c.getResources().getDisplayMetrics().density;GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(18*d);g.setStroke(Math.max(1,Math.round(2*d)),0xff39282e);return g;}
}
