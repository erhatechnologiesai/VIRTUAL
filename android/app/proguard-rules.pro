# ProGuard rules for VIRTUAL Android Application

# Keep WebRTC Native & Java bindings
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Keep CameraX internals
-keep class androidx.camera.core.** { *; }
-keep class androidx.camera.camera2.** { *; }

# Keep Models for Gson serialization
-keep class com.virtual.core.models.** { *; }

# Keep Socket.IO & OkHttp
-keep class io.socket.** { *; }
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep ZXing
-keep class com.google.zxing.** { *; }
