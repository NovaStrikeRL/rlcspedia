# WorkManager (lo usa Glance para el widget) crea su base de datos Room por reflexión.
# Sin esto, R8 elimina el constructor de WorkDatabase_Impl y la app se cierra al abrir.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
