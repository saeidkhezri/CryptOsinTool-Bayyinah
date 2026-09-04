# Proguard / R8 Configuration Rules for Bayyinah
# Ensures absolute forensic data integrity by protecting serializations, reflection, and database mapping.

# Preserve all app classes from being obfuscated (Base broad keep to guarantee stability)
-keep class com.aistudio.orbit.** { *; }

# Keep common reflection and debugging attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# -------------------------------------------------------------------------
# AndroidX Room Database Keep Rules
# -------------------------------------------------------------------------
# Keep Room database and DAO annotations and structures
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep generated Room classes
-keep class com.aistudio.orbit.db.*_Impl { *; }
-keep class com.aistudio.orbit.db.*Dao { *; }

# Keep all Entity classes to preserve table mappings and property names
-keep class com.aistudio.orbit.db.*Entity { *; }
-keep class com.aistudio.orbit.model.** { *; }

# -------------------------------------------------------------------------
# Retrofit and OkHttp Rules
# -------------------------------------------------------------------------
# Retain Retrofit annotations and types
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# -------------------------------------------------------------------------
# Kotlin Serialization / JSON Rules
# -------------------------------------------------------------------------
# Keep kotlinx.serialization helper classes and annotations
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
}
-keep class * implements kotlinx.serialization.KSerializer { *; }
-keep class kotlinx.serialization.** { *; }

# -------------------------------------------------------------------------
# Gson / JSON DTO Keep Rules
# -------------------------------------------------------------------------
# Ensure GSON serialized fields are preserved for correct external API responses
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }

# -------------------------------------------------------------------------
# Log and Diagnostics Safety
# -------------------------------------------------------------------------
# Preserve stacktrace line numbers for detailed forensic error reporting
-keepattributes SourceFile,LineNumberTable
