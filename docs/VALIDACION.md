# Validación de Trazo

Fecha: 11 de julio de 2026

## Entorno

- Microsoft OpenJDK 17.0.19 LTS.
- Android Gradle Plugin 9.2.1 y Gradle 9.4.1.
- Android SDK 37.0 y Build Tools 36.0.0.
- Dispositivo Samsung SM-S936B conectado mediante ADB.

## Resultados automatizados

- `testDebugUnitTest`: correcto; 5 pruebas de rachas y planificación por fecha.
- `assembleDebug`: correcto.
- `lintDebug`: correcto, sin errores.
- `assembleRelease`: correcto, incluida minificación R8 y Lint Vital.
- Firma APK debug: válida mediante APK Signature Scheme v2 y RSA de 2048 bits.

## Recorrido real en el teléfono

1. Instalación transmitida por ADB: `Success`.
2. Inicio en frío de `MainActivity`: `Status: ok`.
3. Captura e inspección visual a 1080 × 2340: interfaz completa y navegable.
4. Creación de una tarea desde el panel inferior.
5. Cierre forzado y nuevo inicio: la tarea siguió almacenada.
6. Marcado como completada, cierre y nuevo inicio: el estado siguió almacenado.
7. Eliminación mediante el diálogo de confirmación: la lista volvió al estado vacío.
8. Creación del hábito «Beber agua», marcado como realizado y reinicio: hábito y cumplimiento siguieron almacenados.
9. Eliminación confirmada del hábito: la colección volvió al estado vacío.
10. Búfer Android `crash`: vacío al terminar.

El dato de prueba fue eliminado y la aplicación quedó instalada y limpia en el dispositivo.

## Validación de la versión 1.1.0

- Actualización instalada sobre la versión anterior conservando tareas y hábitos existentes.
- Planner semanal, agenda diaria y navegación mensual renderizados en el Samsung SM-S936B.
- Tarea con fecha creada, persistida tras reinicio y mostrada en el día correcto del calendario.
- Pomodoro iniciado y enviado a segundo plano: avanzó de 24:56 a 24:49 usando tiempo real.
- Temporizador pausado y reiniciado después de la prueba.
- Dato de calendario usado para la prueba eliminado sin afectar las tareas del usuario.
- Búfer Android `crash`: vacío.

## Validación de la versión 1.3.0

- Editor de tareas abierto con contenido existente precargado.
- Botones de edición de hábitos presentes en cada tarjeta y formulario conectado al mismo modelo persistente.
- Diálogo de tiempos personalizados abierto con dos campos y límites de seguridad.
- Reiniciar fuerza estado Enfoque y duración principal.
- Acción Detener sincronizada mediante broadcast interno del servicio a la interfaz.
- Títulos extensos limitados a dos líneas en selección y elipsis dentro del temporizador.
- Temporizador rediseñado como tomate orgánico con doble trazo, hojas curvas, etiqueta de papel y progreso interior.

## Validación de la versión 1.2.0

- Permiso de notificaciones concedido en Android 17.
- Alarma diaria programada para el 13 de julio de 2026 a las 09:00 mediante `RTC_WAKEUP` inexacto.
- Canales «Agenda y hábitos» y «Temporizador de enfoque» creados correctamente.
- Pomodoro iniciado y enviado a segundo plano con servicio foreground `specialUse` activo.
- Notificación persistente verificada a 24:55, silenciosa, no descartable y con acción «Detener».
- Servicio y notificación detenidos al pausar desde la aplicación.
- Ilustración de tomate comprobada visualmente a 1080 × 2340.
- Búfer Android `crash`: vacío.

## Validación de la versión 1.4.0

- Proveedor de widget incluido en el manifiesto y reconocido por Android.
- Renderizado de fecha, pendientes, progreso de hábitos y próxima tarea desde el almacenamiento local.
- Acción rápida para completar la próxima tarea y actualización automática tras cualquier cambio.
- Accesos profundos a Hoy, Planner y Enfoque sin crear una segunda actividad.
- Widget redimensionable con actualización periódica y sin nuevas dependencias.
- Instalación sobre los datos existentes en Samsung SM-S936B y alta real del widget 4×3 desde One UI.
- Vista previa y widget final inspeccionados visualmente: papel, doble borde, tomate, nota y acciones sin recortes.
- Planner abrió `AGENDA VIVA` y Enfoque abrió `MODO ENFOQUE` desde la pantalla de inicio.
- Tarea temporal `PruebaWidget` creada, mostrada y completada con el botón del widget: el contador cambió de 1 a 0; luego se eliminó.
- La app recarga el estado al reanudarse para reflejar acciones realizadas desde el widget mientras estaba en memoria.
- Búfer Android `crash`: vacío al terminar.

## Validación de la versión 1.4.1

- Ícono adaptativo compilado con fondo de cuaderno y primer plano ilustrado.
- Variante clásica y variante redonda incluidas para Android 8.0+.
- Actualización instalada sobre Samsung SM-S936B conservando datos y widget.
- Ícono inspeccionado en la pantalla de información de la aplicación sin recortes ni deformaciones.
- Cinco pruebas unitarias correctas, Lint sin errores y compilaciones debug/release satisfactorias.

## Validación de la versión 2.0.0 — 16 de agosto de 2026

- `testDebugUnitTest` y `assembleDebug`: correctos.
- `lintDebug`: 0 errores; las advertencias restantes son informativas o heredadas de vistas previas de widgets y recursos adaptativos.
- `assembleRelease`: correcto con reducción de recursos y minificación R8.
- Los tres proveedores de widgets están declarados en el manifiesto y sus layouts pasan el procesamiento de recursos Android.
- El widget Enfoque usa `Chronometer` descendente y una instantánea local que el servicio crea y limpia en inicio, detención y finalización.
- APK instalado sobre los datos existentes en el Samsung SM-S936B (`R5CY3120BQR`) con resultado `Success`.
- Inicio, Tareas, Planner, Hábitos y Enfoque recorridos por ADB; los encabezados, filtros y resúmenes nuevos aparecen en la jerarquía accesible.
- Inicio y pausa del Pomodoro comprobados: cuenta regresiva, servicio foreground, notificación, instantánea local y limpieza al pausar.
- Salto a descanso comprobado visualmente: taza sin recortes, texto legible, duración 05:00, botón verde y notificación `Descanso`.
- Android reconoce los tres proveedores `TrazoWidget`, `HabitWidget` y `FocusWidget` para el paquete instalado.
- Siete pruebas unitarias correctas y 0 crashes de `com.trazo.app` durante el recorrido final.

## Validación de la versión 2.1.0 — 16 de agosto de 2026

- `testDebugUnitTest`, `assembleDebug`, `lintDebug` y `assembleRelease`: correctos; Lint terminó con 0 errores.
- Android registró cinco proveedores para Trazo: `TrazoWidget`, `HabitWidget`, `FocusWidget`, `TaskListWidget` y `HabitListWidget`.
- One UI mostró las cinco vistas previas; los dos widgets nuevos se añadieron realmente a una quinta página del inicio y se inspeccionaron a 1080 × 2340.
- Jardín de rituales desmarcó tres hábitos individualmente sin abrir la app; el progreso cambió de 4/4 a 1/4 permaneciendo en el lanzador.
- Mis próximos trazos recibió una tarea temporal, la completó desde el control de su fila y volvió al estado “Día despejado” sin abrir la app.
- La tarea temporal se eliminó y los cumplimientos usados en las pruebas se restauraron: 0/4 hábitos y ninguna tarea pendiente de prueba.
- El botón del widget Enfoque inició el servicio y cambió a «Detener»; un segundo toque detuvo el servicio y restauró «Iniciar 25 min».
- El modo de pantalla siempre activa se abrió durante una sesión real: reloj descendente, dibujo respirante, brillo de ventana reducido y `mHoldScreenWindow` apuntando a Trazo.
- Pausar y salir se mantuvieron accesibles dentro del modo tenue; al detener la prueba no quedaron servicio ni notificación activa.
- La instalación incremental en Samsung SM-S936B (`R5CY3120BQR`) devolvió `Success` y no se detectaron crashes de `com.trazo.app`.

## Validación de las versiones 2.2.0–2.2.2 — 16 de agosto de 2026

- Se generaron e integraron seis ilustraciones sketch transparentes: tomate y taza del Pomodoro, tomate de cabecera, nota/tarea, brote/ritual y cuaderno de encabezado.
- El encabezado de Hoy fue inspeccionado a 1080 × 2340: el antiguo adorno ambiguo quedó reemplazado por el cuaderno ilustrado, sin recortes.
- El widget principal 4×3 se instaló y renderizó completo en One UI con fecha, progreso, tarjetas y botones dentro de sus límites.
- El carrusel de hábitos se probó desde el lanzador: avanzó de `1/4 · beber 1L de agua` a `2/4 · skincare matutino` sin abrir Trazo.
- El control directo de enfoque del widget inició `FocusTimerService`, cambió a «Detener enfoque» y el segundo toque eliminó el servicio y restauró «Iniciar 25 min».
- El Pomodoro mostró el tomate IA en Enfoque y la taza IA en Descanso. Tras iniciar la pausa, el reloj avanzó de 05:00 a 04:56; Reiniciar detuvo el servicio y regresó a Enfoque.
- `testDebugUnitTest`, `assembleDebug`, `lintDebug` y `assembleRelease`: correctos después de añadir la navegación del widget; Lint terminó sin errores.
- La navegación por flechas fue sustituida por dos colecciones `ListView` de una fila visible. Un prototipo intermedio con `StackView` fue descartado en la prueba física porque su perspectiva superponía las tarjetas en la altura disponible.
- En el Samsung, el gesto cambió el hábito de `1/4 · beber 1L de agua` a `2/4 · skincare matutino` sin mover la página del launcher. Completar y desmarcar la tarjeta desplazada actualizó el resumen de 0/4 a 1/4 y luego lo restauró a 0/4.
- Se crearon dos tareas temporales: el gesto cambió de `1/2 · PruebaSwipeDos` a `2/2 · PruebaSwipeUno`; completar actuó sobre la visible y dejó `1/1 · PruebaSwipeDos`. Abrir la tarea desde el widget también llegó correctamente a la sección Tareas.
- Las dos tareas temporales se eliminaron y el dispositivo quedó nuevamente con 0 pendientes y 0/4 rituales, sin crashes registrados.
- En 2.2.2 las dos colecciones pasaron de alturas fijas a pesos equivalentes con mínimos de 62/54 dp. En el Samsung ocuparon toda la superficie disponible, mostraron varias tarjetas de hábitos y dejaron Planner/Enfoque alineados al borde inferior.
- Las bandas `TAREAS · DESLIZA ↕` en coral y `HÁBITOS · DESLIZA ↕` en verde se comprobaron sin recortes. El gesto dentro de la zona ampliada recorrió hasta los elementos 3/4 y 4/4 sin mover la página del launcher ni producir crashes.

## Validación de la versión 2.3.0 — 16 de agosto de 2026

- `testDebugUnitTest` y `assembleDebug` finalizaron correctamente tras integrar categorías, respaldo y configuración por instancia.
- La actualización se instaló sobre los datos existentes del Samsung SM-S936B con resultado `Success`; tareas, hábitos, rachas y fechas se conservaron.
- La migración en lectura clasificó hábitos anteriores sin editarlos: agua como Hidratación, ejercicio como Movimiento, skincare como Autocuidado y desayunar como Alimentación.
- El editor de hábitos mostró las seis categorías, mantuvo seleccionado el valor inferido y permitió cambiarlo sin afectar identificador, racha ni días programados.
- El widget 4×4 ocultó Tareas al no haber pendientes y distribuyó cuatro hábitos hasta el borde inferior. Mostró recursos distintos para vaso de agua, autocuidado y alimento, sin repetir `DESLIZA` dentro de las filas.
- La configuración de la instancia 24 se abrió y guardó valores no predeterminados. El botón cambió a `Iniciar 60 min` y a tinta oscura; después se restauró y se verificó en preferencias: 25 minutos, Coral, 4 elementos y ambas secciones visibles.
- Exportar abrió `DocumentsUI` en modo creación JSON e Importar abrió el selector de documentos. Ambos se cancelaron para no alterar los datos personales durante la prueba.
- La pantalla Hoy mostró la tarjeta `Tus datos, contigo` con ambos controles, sin solicitar permisos de almacenamiento ni agregar permisos de red.
- El pase final `lintDebug testDebugUnitTest assembleRelease assembleDebug` terminó con `BUILD SUCCESSFUL`; Lint registró 0 errores y solo advertencias informativas de recursos/textos existentes.
- El APK `Trazo-2.3.0-debug.apk` se instaló nuevamente con `Success`. Android confirmó `versionCode=13`, `versionName=2.3.0` y el búfer `crash` no contenía fallos de `com.trazo.app`.
- Tras la revisión de jerarquía, Notificaciones y Datos locales se movieron desde el flujo de Hoy a una hoja de Ajustes accesible desde el engranaje del encabezado. La compilación y las pruebas unitarias posteriores volvieron a terminar correctamente.
- El botón flotante `+` quedó oculto únicamente en Hoy y Enfoque; Capturar es ahora la única entrada de creación en Hoy, sin perder la creación contextual del resto de secciones.
- Capturar abre un selector Tarea/Hábito y deriva al compositor correspondiente, evitando que la acción principal de Hoy favorezca un solo tipo de contenido.

## Validación de la versión 3.0.0 — 16 de agosto de 2026

- Migración local compatible: la instalación incremental conservó las tareas, hábitos, fechas, categorías y rachas de 2.3.
- Nueve pruebas unitarias correctas, incluidas frases de captura inteligente con fecha, hora, prioridad, etiqueta, repetición y categoría inferida.
- `lintDebug`, `testDebugUnitTest`, `assembleRelease` y `assembleDebug` finalizaron correctamente; Lint registró 0 errores.
- El arranque en frío en Samsung SM-S936B terminó en 429 ms, mostró Hoy con 13/13 trazos y mantuvo el proceso activo.
- Android confirmó `versionCode=14` y `versionName=3.0.0`; el búfer de crashes permaneció vacío.
- Capturar mostró el campo inteligente, revisión previa, destinos Tarea/Hábito y el control «Dictar nota de voz».
- Android resolvió el dictado mediante la actividad de reconocimiento instalada en el teléfono; Trazo no solicita almacenamiento ni conserva una grabación.
- Ajustes mostró notificaciones, tema/accesibilidad, estadísticas semanales, archivo/papelera y datos locales dentro de una hoja desplazable.
- Las superficies de Tareas, Hábitos, Planner y selección de Pomodoro usan la paleta activa; el texto principal y secundario aumenta contraste en tema oscuro.

## Validación de la versión 3.0.1 — 16 de agosto de 2026

- El defecto responsivo se reprodujo físicamente con tema oscuro y texto ampliado: `comprar ropa` se partía en tres líneas estrechas y `beber 1L de agua` quedaba en una letra por línea.
- Tras separar contenido y acciones, ambos títulos se muestran completos; la pantalla vuelve a presentar varias tarjetas de hábitos simultáneamente.
- Las barras de acciones conservan edición, fecha, archivo y papelera, y admiten desplazamiento horizontal en anchos menores.
- El resumen de mejor racha ya no compite en una misma línea entre número, unidad y etiqueta.

## Validación de la versión 3.1.0 — 16 de agosto de 2026

- El intérprete cuenta con once casos propios: prioridad/etiqueta, hábito diario, L–X–V, rango laboral, fin de semana, pasos con abreviaturas, tarea de un solo día, recurrencia con `cada`, recurrencia con `los`, momento diario y fecha española.
- `Spinning lunes miércoles viernes` devuelve Hábito, título limpio `Spinning`, categoría/icono Movimiento y días MONDAY/WEDNESDAY/FRIDAY.
- `Entregar informe viernes a las 16 #trabajo` devuelve una tarea con próximo viernes, recordatorio 16:00 y etiqueta trabajo.
- El manifiesto incluye el sexto proveedor `SmartCaptureWidget` y la actividad de voz translúcida, privada y excluida de recientes.
- El dictado del widget no conserva audio: recibe únicamente la transcripción devuelta por el reconocedor configurado en Android.
- El pase final `lintDebug testDebugUnitTest assembleRelease assembleDebug` terminó con `BUILD SUCCESSFUL`: 18 pruebas, 0 fallos y 0 errores de Lint.
- `Trazo-3.1.0-debug.apk` se instaló incrementalmente con `Success`; Android confirmó `versionCode=16`, `versionName=3.1.0`, arranque correcto en 393 ms y búfer de fallos vacío.

## Validación de la versión 3.2.0

- La actividad de captura por voz compila con confirmación previa para tarea o hábito y cierre seguro al cancelar.
- El checkpoint previo está guardado en Git como `7a49f0b`.

## Validación de la versión 3.3.0

- Se añadieron pruebas para recurrencia quincenal, excepciones de calendario, revisión diaria y nuevas frases del intérprete.
- El almacenamiento subió a versión 5 conservando valores predeterminados para copias anteriores.
- La configuración de cada widget se mantiene aislada por identificador y los filtros no modifican los datos originales.
- El pase final `lintDebug testDebugUnitTest assembleRelease assembleDebug` terminó con `BUILD SUCCESSFUL`: 22 pruebas, 0 fallos y 0 errores de Lint.
- La revisión visual a 1080 × 2340 comprobó la tarjeta de revisión en Hoy, el modo Enfoque con ilustración y ciclo automático, y Ajustes con texto ampliado sin desbordes.
- `Trazo-3.3.0-debug.apk` se instaló incrementalmente con `Success`; Android confirmó `versionCode=19`, `versionName=3.3.0` y un arranque correcto en 346 ms.
- El búfer de crashes permaneció vacío y el APK compartible se copió a `/sdcard/Download/Trazo-3.3.0.apk`.
- Corrección posterior: las barras normal y siempre activa representan avance transcurrido (vacía al inicio y llena al terminar), se limitan a 0–100 % y se validan con casos de restauración inválida. La foto física reveló además una discordancia cuando había un tiempo personalizado: el reloj nacía fijo en 25:00 y la barra usaba el valor guardado; ahora ambos nacen del mismo ajuste.
- Una segunda auditoría contrastó individualmente los puntos 3, 4, 5, 6, 7, 10, 18, 19 y 20. Se completaron el selector de recurrencia de 1–12 semanas, las excepciones mediante calendario, sugerencias con minutos de enfoque, tres artes contextuales por horario y microanimaciones de finalización.
- El estilo minimalista del widget usa ahora una superficie propia y oculta todas sus imágenes decorativas. La privacidad discreta también neutraliza emoji, prioridad, racha, metadatos y descripciones de accesibilidad.
- El pase posterior `testDebugUnitTest lintDebug assembleRelease assembleDebug` terminó con `BUILD SUCCESSFUL`: 25 pruebas, 0 fallos y 0 errores de Lint.
- El APK actualizado se instaló incrementalmente con `Success`; la inspección física confirmó que la barra comienza dentro de su pista y el arranque cálido terminó en 283 ms.
- La demostración nocturna detectó que 00:00–04:59 compartía por error la ilustración matinal; el rango se alineó con el saludo: mañana 05–11, tarde 12–19 y noche 20–04.
- Tras precisar el alcance de los puntos 6/7, el Pomodoro pasó de dos recursos a cuatro estados ilustrados: preparado, enfoque activo con lápiz, pausa con ojos cerrados y taza de descanso. La selección de estado tiene prueba unitaria propia.
- El pase final `testDebugUnitTest lintDebug assembleDebug` terminó con `BUILD SUCCESSFUL`: 26 pruebas, 0 fallos y 0 errores de Lint.
- La compilación final se instaló con `Success` y sustituyó `/sdcard/Download/Trazo-3.3.0.apk`. Por indicación del usuario, la comprobación visual de los cuatro estados queda a cargo de su prueba manual; no se realizaron más interacciones automatizadas con la interfaz.

## Validación de la versión 3.3.1

- La auditoría de consistencia encontró y eliminó color, ilustraciones, textura, sombras, bordes sketch, tipografía decorativa, emojis y animaciones que aún aparecían en la vista minimalista.
- App, Pomodoro, pantalla siempre activa y estilo minimalista del widget comparten ahora la misma regla monocromática y mantienen visibles solo datos y acciones funcionales.
- `testDebugUnitTest lintDebug assembleDebug` terminó con `BUILD SUCCESSFUL`: 26 pruebas, 0 fallos y 0 errores de Lint.
- El APK generado corresponde a `versionCode=20`, `versionName=3.3.1`. La inspección visual queda expresamente para la prueba manual del usuario; la instalación no automatiza interacciones con la interfaz.

## Validación de la versión 3.3.2

- La búsqueda de interfaz ya no encuentra los antiguos caracteres ambiguos de editar, fecha, archivo, borrado, navegación, creación, voz, notificaciones ni flechas.
- El nuevo componente contiene veinte símbolos trazados, semántica accesible cuando funciona como control y una variante limpia automática para el modo minimalista.
- `testDebugUnitTest lintDebug assembleDebug` terminó con `BUILD SUCCESSFUL`: 26 pruebas, 0 fallos y 0 errores de Lint.
- El APK corresponde a `versionCode=21`, `versionName=3.3.2`; la validación visual en el teléfono queda a cargo del usuario según lo acordado.

## Validación de la versión 3.3.3

- Archivadas y Papelera usan encabezados, superficies, explicaciones e iconos distintos.
- Los elementos archivados solo ofrecen Restaurar; cada elemento de Papelera ofrece Restaurar o Eliminar para siempre dentro de su propia tarjeta.
- Archivar dispone de Deshacer y el borrado irreversible exige confirmación nominal.
- `testDebugUnitTest lintDebug assembleDebug` terminó con `BUILD SUCCESSFUL`: 26 pruebas, 0 fallos y 0 errores de Lint.
- El APK corresponde a `versionCode=22`, `versionName=3.3.3`.

## Validación de la versión 3.3.4

- Se añadieron cinco pruebas de política de recordatorios para tareas y hábitos activos, completados, archivados y enviados a Papelera.
- `testDebugUnitTest lintDebug assembleDebug` terminó con `BUILD SUCCESSFUL`: 31 pruebas, 0 fallos y 0 errores de Lint.
- La sincronización posterior al guardado cancela alarmas pendientes y notificaciones ya visibles; una segunda lectura evita que un aviso obsoleto reaparezca durante una edición concurrente.
- El APK se instaló con `Success` en el teléfono conectado. Android confirmó `versionCode=23`, `versionName=3.3.4`; también se copió a `/sdcard/Download/Trazo-3.3.4.apk`.
- Según lo acordado, no se abrió ni se manipuló la interfaz: la comprobación funcional en el teléfono queda a cargo del usuario.

## Validación de publicación GitHub

- El primer runner detectó que API 37 no estaba disponible en el canal estable de `sdkmanager`; la configuración reproducible se alineó con API 36 y con las últimas líneas compatibles de Core y Lifecycle.
- El pase local posterior `testDebugUnitTest lintDebug assembleDebug` terminó con `BUILD SUCCESSFUL`: 31 pruebas, 0 fallos y 0 errores de Lint.
- Los tres archivos YAML de GitHub se validaron sintácticamente y el APK 3.3.4 se regeneró desde la misma fuente que se publicará.

## Validación de la versión 3.4.0

- Se añadieron cinco casos unitarios para agenda diaria y recuperación de avisos: mismo día, salto al día siguiente, ventana reciente, aviso antiguo y evento futuro.
- `testDebugUnitTest`, `lintDebug`, `assembleDebug` y `assembleRelease` finalizaron con `BUILD SUCCESSFUL`: 36 pruebas, 0 fallos y 0 errores de Lint.
- La nueva configuración migra el antiguo resumen diario sin borrar la hora elegida y mantiene activos por defecto los recordatorios puntuales ya creados.
- La política cancela avisos visibles cuando se pausa todo o se desactiva la categoría de tareas o hábitos.
- Los avisos puntuales usan alarma exacta únicamente si Android concede el acceso especial; en caso contrario recurren a `setAndAllowWhileIdle`.
- Los canales quedan separados en «Tareas y hábitos a su hora» (alta importancia) y «Resumen y cierre del día» (importancia normal), mientras Pomodoro conserva su canal silencioso propio.
- `Trazo-3.4.0-debug.apk` se instaló incrementalmente con `Success` en el Samsung SM-S936B; Android confirmó `versionCode=24`, `versionName=3.4.0`. El mismo APK quedó en `/sdcard/Download/Trazo-3.4.0.apk` con SHA-256 `36E726F9D009CB60324EF614A3F7FAFC90B932501D542DE45F9DB5C34A2DD09C`.
- No se abrió ni se manipuló la interfaz: la revisión visual y la recepción de alarmas quedan a cargo de la prueba manual acordada con el usuario.
