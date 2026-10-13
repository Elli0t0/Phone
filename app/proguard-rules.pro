# Project specific ProGuard / R8 rules for Phone
# Release builds run with minifyEnabled + shrinkResources. The libraries used
# (Compose, Room, DataStore, Coil) ship their own consumer rules; the rules
# below keep the app code the system or reflection reaches directly.

# Attributes needed by libraries, enums, and stack-trace deobfuscation.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes SourceFile, LineNumberTable
-dontwarn java.lang.invoke.**

# Manifest components: instantiated by the system, never referenced by app code.
-keep public class io.github.elli0t0.phone.PhoneApplication { *; }
-keep public class io.github.elli0t0.phone.MainActivity { *; }
-keep public class io.github.elli0t0.phone.feature.call.CallActivity { *; }
-keep public class io.github.elli0t0.phone.core.telecom.PhoneInCallService { *; }
-keep public class io.github.elli0t0.phone.core.telecom.CallActionReceiver { *; }

# Room: database implementation, entities, and DAOs are reached via generated
# code and reflection. Keep entity/DAO members used by the schema.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.PrimaryKey <fields>;
    @androidx.room.ColumnInfo <fields>;
    @androidx.room.Embedded <fields>;
    @androidx.room.Relation <fields>;
}

# DataStore: settings and setup repositories hold Preferences keys looked up by
# name; keep them and their key holders.
-keep class io.github.elli0t0.phone.data.settings.** { *; }

# Coil: keep the Compose image-loading API used by the contact avatar UI.
-keep class coil.compose.** { *; }
-keep class coil.request.ImageRequest { *; }
-keep class coil.request.ImageRequest$Builder { *; }

# Reflection-safe members: enum values, Parcelable creators, native methods.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclasseswithmembernames class * {
    native <methods>;
}
