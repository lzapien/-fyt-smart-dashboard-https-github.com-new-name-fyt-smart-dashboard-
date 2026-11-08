package com.fytsmart.dashboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadata;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bounded bitmap/content/resource/HTTPS artwork decoding and sampling off the main thread. */
final class MediaArtwork implements AutoCloseable {
    interface Callback { void onArtwork(Bitmap bitmap, int tint); }
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile int generation;
    private volatile boolean closed;
    MediaArtwork(Context context) { this.context=context.getApplicationContext(); }
    void clear() { generation++; }
    void load(MediaMetadata metadata, Callback callback) {
        int request=++generation;
        worker.execute(() -> {
            Bitmap bitmap=null;
            try {
                if(closed || request!=generation) return;
                if(metadata!=null) {
                    bitmap=metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
                    if(bitmap==null)bitmap=metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                    if(bitmap==null)bitmap=metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
                    if(bitmap==null) {
                        String value=metadata.getString(MediaMetadata.METADATA_KEY_ART_URI);
                        if(value==null)value=metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI);
                        if(value==null)value=metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI);
                        if(value!=null)bitmap=decode(Uri.parse(value));
                    }
                }
                if(bitmap!=null) {
                    float scale=Math.min(1f,384f/Math.max(bitmap.getWidth(),bitmap.getHeight()));
                    // Always copy even small images: hardware bitmaps may not support getPixel.
                    Bitmap scaled=Bitmap.createScaledBitmap(bitmap,Math.max(1,Math.round(bitmap.getWidth()*scale)),
                            Math.max(1,Math.round(bitmap.getHeight()*scale)),true);
                    bitmap=scaled.copy(Bitmap.Config.ARGB_8888,false);
                    if(!hasVisibleArtwork(bitmap))bitmap=null;
                }
            } catch (Exception | OutOfMemoryError ignored) { bitmap=null; }
            if((context.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0)
                android.util.Log.d("FYT_Artwork",bitmap==null?"No usable artwork; show source icon":
                        "Artwork decoded "+bitmap.getWidth()+"x"+bitmap.getHeight());
            int tint=0xff174356;
            if(bitmap!=null) {
                long r=0,g=0,b=0;int count=0;
                for(int y=1;y<8;y++)for(int x=1;x<8;x++) {
                    int pixel=bitmap.getPixel(bitmap.getWidth()*x/8,bitmap.getHeight()*y/8);
                    r+=Color.red(pixel);g+=Color.green(pixel);b+=Color.blue(pixel);count++;
                }
                tint=Color.rgb((int)(r/count),(int)(g/count),(int)(b/count));
            }
            Bitmap result=bitmap;int color=tint;
            if(!closed && request==generation)main.post(() -> {
                if(!closed && request==generation)callback.onArtwork(result,color);
            });
        });
    }
    private static boolean hasVisibleArtwork(Bitmap bitmap) {
        // Some session providers publish a tiny/transparent placeholder as a non-null bitmap.
        if(bitmap==null||bitmap.getWidth()<8||bitmap.getHeight()<8)return false;
        if(!bitmap.hasAlpha())return true;
        int[] row=new int[bitmap.getWidth()];
        for(int y=0;y<bitmap.getHeight();y++) {
            bitmap.getPixels(row,0,row.length,0,y,row.length,1);
            for(int pixel:row)if(Color.alpha(pixel)>8)return true;
        }
        return false;
    }
    private Bitmap decode(Uri uri) throws Exception {
        if("https".equals(uri.getScheme()))return decodeHttps(uri);
        if(!"content".equals(uri.getScheme()) && !"android.resource".equals(uri.getScheme()))return null;
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
        try(InputStream stream=context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(stream,null,options);
        }
        if(options.outWidth<=0 || options.outHeight<=0)return null;
        options.inSampleSize=1;
        while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>768)options.inSampleSize*=2;
        options.inJustDecodeBounds=false;
        try(InputStream stream=context.getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(stream,null,options);
        }
    }
    private Bitmap decodeHttps(Uri uri) throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL(uri.toString()).openConnection();
        connection.setConnectTimeout(4000);connection.setReadTimeout(4000);connection.setInstanceFollowRedirects(false);
        try {
            if(connection.getResponseCode()!=200 || connection.getContentLengthLong()>2*1024*1024)return null;
            byte[] data;
            long deadline=android.os.SystemClock.elapsedRealtime()+5000;
            try(InputStream input=connection.getInputStream();ByteArrayOutputStream output=new ByteArrayOutputStream()) {
                byte[] buffer=new byte[8192];int size;
                while((size=input.read(buffer))!=-1) {
                    if(closed || android.os.SystemClock.elapsedRealtime()>deadline || output.size()+size>2*1024*1024)return null;
                    output.write(buffer,0,size);
                }
                data=output.toByteArray();
            }
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
            BitmapFactory.decodeByteArray(data,0,data.length,options);
            if(options.outWidth<=0 || options.outHeight<=0)return null;
            options.inSampleSize=1;
            while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>768)options.inSampleSize*=2;
            options.inJustDecodeBounds=false;
            return BitmapFactory.decodeByteArray(data,0,data.length,options);
        } finally {connection.disconnect();}
    }
    @Override public void close() { closed=true;generation++;main.removeCallbacksAndMessages(null);worker.shutdown(); }
}
