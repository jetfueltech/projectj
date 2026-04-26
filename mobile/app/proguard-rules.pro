# DJI MSDK keeps reflection-heavy code; keep the public surface.
-keep class dji.** { *; }
-keep class com.dji.** { *; }
-dontwarn dji.**
