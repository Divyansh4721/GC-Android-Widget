package com.example.rateswidget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

public class ProfileImageGenerator {
    
    public static Bitmap generateCircularProfileImage(String name, int width, int height) {
        // Ensure non-zero dimensions
        if (width <= 0) width = 100;
        if (height <= 0) height = 100;
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        Paint backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(generateColorFromName(name));
        
        canvas.drawCircle(width / 2f, height / 2f, width / 2f, backgroundPaint);
        
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        
        textPaint.setTextSize(width * 0.6f);
        
        String initials = getInitials(name);
        
        Rect textBounds = new Rect();
        textPaint.getTextBounds(initials, 0, initials.length(), textBounds);
        
        canvas.drawText(
            initials, 
            width / 2f, 
            height / 2f + textBounds.height() / 2f, 
            textPaint
        );
        
        return bitmap;
    }
    
    private static String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        
        String[] nameParts = name.trim().split("\\s+");
        
        if (nameParts.length == 1) {
            return nameParts[0].substring(0, 1).toUpperCase();
        }
        
        return (nameParts[0].substring(0, 1) + 
                nameParts[nameParts.length - 1].substring(0, 1))
                .toUpperCase();
    }
    
    private static int generateColorFromName(String name) {
        if (name == null || name.isEmpty()) {
            return Color.GRAY;
        }
        
        int[] goldenColors = {
            Color.rgb(255, 215, 0),   // Gold
            Color.rgb(218, 165, 32),  // Goldenrod
            Color.rgb(238, 232, 170), // Pale Goldenrod
            Color.rgb(189, 183, 107), // Dark Khaki
            Color.rgb(240, 230, 140)  // Khaki
        };
        
        return goldenColors[Math.abs(name.hashCode()) % goldenColors.length];
    }
}