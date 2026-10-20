# Keep Room generated implementations.
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Room entities are reflected on by generated code.
-keepclassmembers class com.epicmems.stride.data.local.** { *; }
