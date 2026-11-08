package com.fytsmart.dashboard;

import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.SystemClock;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

/** Optional test-APK suite. No GPS fixtures, OEM interaction or main appearance preference edits. */
final class AppearanceChecks {
    private final Instrumentation instrumentation;
    private int passed;
    AppearanceChecks(Instrumentation instrumentation){this.instrumentation=instrumentation;}
    private void check(boolean value,String message){if(!value)throw new AssertionError(message);passed++;}
    void run(){
        Bundle result=new Bundle();Context context=instrumentation.getTargetContext();
        SharedPreferences isolated=context.getSharedPreferences("appearance_instrumentation_only",Context.MODE_PRIVATE);
        AtomicReference<SuvRearView> view=new AtomicReference<>();AtomicReference<Throwable> failure=new AtomicReference<>();
        int resultCode=-1;
        try{
            isolated.edit().clear().commit();
            ThemeManager manager=new ThemeManager(new ThemeManager.PreferencesStorage(isolated));
            manager.save("TEST-123",ThemeConfig.CHRISTMAS,false);
            check(isolated.edit().putBoolean("flush_test",true).commit(),"Preferences flush succeeds");
            ThemeManager.Selection loaded=new ThemeManager(new ThemeManager.PreferencesStorage(isolated)).load();
            check(loaded.plate.equals("TEST-123")&&loaded.theme==ThemeConfig.CHRISTMAS&&!loaded.animations,"Private preferences roundtrip");
            String xml=new String(Files.readAllBytes(new File(context.getDataDir(),"shared_prefs/appearance_instrumentation_only.xml").toPath()),StandardCharsets.UTF_8);
            check(xml.contains("TEST-123")&&xml.contains("christmas"),"Selection exists in private XML on disk");
            instrumentation.runOnMainSync(()->view.set(new SuvRearView(context)));
            for(int i=0;i<100;i++){
                AtomicReference<Boolean> ready=new AtomicReference<>(false);
                instrumentation.runOnMainSync(()->ready.set(view.get().hasDetailedArtwork()));
                if(ready.get())break;SystemClock.sleep(30);
            }
            instrumentation.runOnMainSync(()->{
                try{
                    SuvRearView suv=view.get();check(suv.hasDetailedArtwork(),"Generic vector vehicle is ready without loading a bitmap");suv.layout(0,0,800,520);
                    suv.applyAppearance(ThemeManager.DEFAULT);Bitmap original=Bitmap.createBitmap(800,520,Bitmap.Config.ARGB_8888);suv.draw(new Canvas(original));
                    for(ThemeConfig theme:new ThemeConfig[]{ThemeConfig.CHRISTMAS,ThemeConfig.HALLOWEEN}){
                        suv.applyAppearance(new ThemeManager.Selection("TEST-123",theme,false));
                        Bitmap themed=Bitmap.createBitmap(800,520,Bitmap.Config.ARGB_8888);suv.draw(new Canvas(themed));
                        check(!original.sameAs(themed),"Theme changes vehicle-area graphics: "+theme.id);
                    }
                    suv.applyAppearance(ThemeManager.DEFAULT);Bitmap restored=Bitmap.createBitmap(800,520,Bitmap.Config.ARGB_8888);suv.draw(new Canvas(restored));
                    check(original.sameAs(restored),"Original returns to exactly the same pixels");
                    DecorationRenderer renderer=new DecorationRenderer();
                    for(ThemeConfig theme:ThemeConfig.values()){
                        Bitmap only=Bitmap.createBitmap(400,260,Bitmap.Config.ARGB_8888);renderer.draw(new Canvas(only),theme,.37f);int count=0;boolean outside=true;
                        for(int y=0;y<260;y++)for(int x=0;x<400;x++)if((only.getPixel(x,y)>>>24)!=0){
                            count++;if(x>=50&&x<350)outside=false;
                        }
                        check(outside,"Decoration stays outside the whole car rectangle");
                        check(theme==ThemeConfig.DEFAULT?count==0:count>0,"Original has zero decorations; seasons have artwork");
                    }
                    suv.applyAppearance(new ThemeManager.Selection("TEST-123",ThemeConfig.CHRISTMAS,true));suv.setRunning(true,true);
                    // Off-screen views have no window attachment and must not start a decoration clock.
                    java.lang.reflect.Field scheduled=SuvRearView.class.getDeclaredField("decorationScheduled");scheduled.setAccessible(true);
                    check(!scheduled.getBoolean(suv),"Unattached preview never schedules animation");suv.setRunning(false,false);
                    check(!scheduled.getBoolean(suv),"Paused view has no scheduled decoration frame");
                }catch(Throwable error){failure.set(error);}
            });
            if(failure.get()!=null)throw new RuntimeException(failure.get());
            result.putString("result","PASS: "+passed+" appearance checks; no main preferences or GPS changed");
        }catch(Throwable error){result.putString("error",error.toString());resultCode=0;}
        finally{isolated.edit().clear().commit();if(view.get()!=null)instrumentation.runOnMainSync(()->view.get().close());}
        instrumentation.finish(resultCode,result);
    }
}
