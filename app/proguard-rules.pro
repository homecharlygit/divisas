# WorkManager (lo trae Glance para el widget) crea su base de datos Room por
# reflexión al arrancar; sin estas reglas R8 borra la clase y la app se cierra.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }

# Glance usa WorkManager para las sesiones del widget y algunas clases se crean por
# nombre (workers, input mergers, receivers). Se conservan completas para evitar que
# el widget se quede en "Cargando" en la versión release.
-keep class androidx.work.** { *; }
-keep class androidx.glance.** { *; }
-keep class dev.charly.divisas.widget.** { *; }
