# WorkManager (lo trae Glance para el widget) crea su base de datos Room por
# reflexión al arrancar; sin estas reglas R8 borra la clase y la app se cierra.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
