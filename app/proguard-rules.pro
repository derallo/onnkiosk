# Keep Android entry points and reflection-accessed ViewModel constructors
-keep class com.nernal.frame.MainActivity { *; }
-keep class com.nernal.frame.BootReceiver { *; }
-keep class com.nernal.frame.NernalViewModel { <init>(android.app.Application); }

# OkHttp / Conscrypt / JSR305 optional annotations not bundled on Android
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
