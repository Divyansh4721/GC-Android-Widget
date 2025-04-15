package com.gcjewellers.rateswidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileImageGenerator {
    private static final String TAG = "ProfileImageGenerator";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public interface ProfileImageCallback {
        void onImageGenerated(Bitmap bitmap);
        void onError(Exception e);
    }
    
    public static void generateProfileImageAsync(String name, ProfileImageCallback callback) {
        // Use a default size instead of trying to load from resources
        int size = 100;
        generateProfileImageAsync(name, callback, size, size);
    }
    
    public static void generateProfileImageAsync(String name, ProfileImageCallback callback, 
                                                int width, int height) {
        executor.execute(() -> {
            try {
                if (width <= 0 || height <= 0) {
                    throw new IllegalArgumentException("Width and height must be positive");
                }
                
                Bitmap bitmap = generateCircularProfileImage(name, width, height);
                
                mainHandler.post(() -> {
                    callback.onImageGenerated(bitmap);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error generating profile image", e);
                mainHandler.post(() -> {
                    callback.onError(e);
                });
            }
        });
    }
    
    public static Bitmap generateCircularProfileImage(String name, int width, int height) {
        if (width <= 0) width = 100;
        if (height <= 0) height = 100;
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // Draw background circle
        Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(generateColorFromName(name));
        canvas.drawCircle(width / 2f, height / 2f, Math.min(width, height) / 2f, backgroundPaint);
        
        // Draw text (initials)
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextSize(width * 0.4f);
        
        String initials = getInitials(name);
        Rect textBounds = new Rect();
        textPaint.getTextBounds(initials, 0, initials.length(), textBounds);
        
        float textX = width / 2f;
        float textY = height / 2f + (textBounds.height() / 2f) - textBounds.bottom;
        
        canvas.drawText(initials, textX, textY, textPaint);
        
        return bitmap;
    }

    private static String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        
        String[] nameParts = name.trim().split("\\s+");
        if (nameParts.length == 1) {
            return nameParts[0].substring(0, Math.min(1, nameParts[0].length())).toUpperCase();
        }
        
        return (nameParts[0].substring(0, Math.min(1, nameParts[0].length())) + 
                nameParts[nameParts.length - 1].substring(0, Math.min(1, nameParts[nameParts.length - 1].length())))
                .toUpperCase();
    }

    private static int generateColorFromName(String name) {
        if (name == null || name.isEmpty()) {
            return Color.GRAY;
        }
        
        int[] goldenColors = {
                Color.rgb(255, 215, 0),    // Gold
                Color.rgb(218, 165, 32),   // Golden Rod
                Color.rgb(238, 232, 170),  // Pale Goldenrod
                Color.rgb(189, 183, 107),  // Dark Khaki
                Color.rgb(240, 230, 140)   // Khaki
        };
        
        return goldenColors[Math.abs(name.hashCode()) % goldenColors.length];
    }
}