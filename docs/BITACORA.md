# Bitácora de desarrollo

## 1. Alcance y experiencia

Se redujo el producto a dos objetos centrales: tareas y hábitos. La pantalla «Hoy» evita que la persona tenga que decidir primero dónde buscar; presenta pendientes y rituales programados en un solo lugar. «Tareas» y «Hábitos» sirven para administrar cada colección completa.

Principios aplicados:

- Captura en pocos toques mediante un único botón contextual.
- Completar un elemento toca toda la tarjeta, no solo un control pequeño.
- Progreso visible sin castigos ni mensajes negativos.
- Estados vacíos que explican el siguiente paso.
- Eliminación disponible, pero visualmente secundaria y protegida por confirmación.

## 2. Sistema visual

Se creó una estética de cuaderno enteramente con Compose: fondo crema, renglones suaves, bordes dibujados, esquinas desiguales, tipografía cursiva en títulos y paleta coral/mostaza/verde/tinta. Al no depender de imágenes externas, el resultado escala bien, mantiene poco peso y funciona sin conexión.

## 3. Modelo y persistencia

`Task` conserva título, nota, prioridad, estado y fecha de creación. `Habit` conserva días activos y fechas completadas. `HabitProgress` calcula la racha recorriendo solo los días programados; el día actual incompleto no rompe prematuramente la racha anterior.

`LocalStore` serializa un estado inmutable a JSON con `version = 1` y lo guarda en las preferencias privadas de la aplicación. Una lectura dañada vuelve a un estado vacío seguro. Cada mutación del ViewModel se persiste inmediatamente.

## 4. Implementación de interfaz

Se implementaron:

- Navegación inferior: Hoy, Tareas y Hábitos.
- Compositores en panel inferior para conservar contexto.
- Tarjetas con áreas táctiles de al menos 48 dp.
- Descripciones semánticas para acciones sin texto.
- Progreso del día y rachas de hábitos.
- Programación semanal con selectores simples.

## 5. Calidad

Se añadieron pruebas puras para los casos límite de rachas. La configuración `release` activa minificación y reducción de recursos. No se solicitaron permisos de red ni datos sensibles.

El proyecto queda configurado para JDK 17 y Android SDK 37.0. El toolchain portátil usado para verificarlo vive en `.tooling` y no forma parte del repositorio.

## 7. Calendario y enfoque — versión 1.1

El esquema local avanzó a v2 para añadir `dueDate` opcional a las tareas. La lectura sigue aceptando el esquema anterior y asigna `null` a las tareas históricas, por lo que la actualización no destruye datos.

Se incorporó una agenda unificada con tres escalas: día para ejecutar, planner semanal para equilibrar carga y mes para orientarse. Las tareas se pueden fechar al crearlas o reprogramar después; los hábitos aparecen automáticamente según sus días activos.

El modo Enfoque ofrece ciclos Pomodoro 25/5 y 50/10. Puede asociarse a una tarea pendiente, calcula el tiempo restante usando reloj real incluso después de pasar a segundo plano y permite completar la tarea desde la misma sesión.

## 8. Notificaciones — versión 1.2

Se añadieron canales separados para agenda y enfoque. El resumen diario se puede activar a las 08:00, 12:00 o 18:00 y utiliza alarmas inexactas para respetar la batería. Se reprograma tras reinicios y cambios de hora.

El Pomodoro inicia un servicio foreground de uso especial solicitado directamente por la persona. Su notificación muestra la cuenta regresiva, la tarea asociada y una acción para detenerla. Al finalizar genera un aviso normal. El encabezado de Enfoque incorpora un tomate dibujado con `Canvas`, sin recursos gráficos externos.

## 9. Edición y Pomodoro — versión 1.3

Se corrigió la máquina de estados: Reiniciar siempre vuelve a Enfoque, incluso durante una pausa. El servicio comunica su detención a la interfaz mediante un broadcast interno, por lo que la acción de la notificación y la pantalla quedan sincronizadas.

Se añadieron tiempos personalizados, truncado elegante para tareas extensas y un temporizador-tomate dibujado que ahora contiene el progreso, rostro, hojas y cuenta regresiva. Tareas y hábitos se pueden editar desde un lápiz contextual sin perder completados, rachas o identificadores.

## 6. Validación en dispositivo

Se instaló el APK debug en un Samsung SM-S936B mediante ADB. Se comprobó el arranque en frío, renderizado de la pantalla principal y los flujos completos de tareas y hábitos: creación, marcado, persistencia tras reinicio y borrado con confirmación. La aplicación permaneció activa y el búfer de crashes quedó vacío durante el recorrido.

## 10. Widget de inicio — versión 1.4

Se incorporó un widget nativo y liviano, sin sumar dependencias. Resume pendientes y hábitos del día, muestra la próxima tarea y permite completarla desde la pantalla de inicio. Los accesos Planner y Enfoque abren directamente la sección correspondiente.

El diseño adapta el lenguaje de Trazo a las restricciones de Android: papel crema con doble borde irregular, nota punteada, botones asimétricos y un tomate ilustrado. Cada guardado local refresca las instancias activas y Android realiza además una actualización periódica para mantener correcta la fecha.

Durante la prueba del completado rápido se detectó que una actividad conservada en memoria podía mantener una copia anterior del estado. Se añadió una recarga local al reanudar la app, manteniendo sincronizados widget, ViewModel y almacenamiento sin introducir una base de datos ni procesos extra.

## 11. Identidad visual — versión 1.4.1

El ícono genérico de círculo y check fue reemplazado por una marca propia: un tomate coral orgánico, hojas verdes, doble trazo de tinta y una marca de tarea integrada. El fondo representa el papel rayado usado dentro de Trazo.

Se añadieron recursos adaptativos y variantes para dispositivos anteriores, de modo que el lanzador puede aplicar formas circulares, redondeadas o squircle sin cortar la ilustración. La identidad mantiene así el mismo lenguaje del widget y del modo Enfoque.

## 12. Reconstrucción de experiencia — versión 2.0

La interfaz se replanteó como un estudio personal y no como cinco listas aisladas. Se conservaron el esquema local y los identificadores existentes para que una actualización no elimine tareas, fechas, hábitos ni rachas.

Pasos desarrollados:

1. Se renovó el sistema visual con tipografía editorial, superficies elevadas de papel, nueva jerarquía cromática y dibujos ornamentales generados con `Canvas`.
2. La portada pasó a ser un panel de ritmo diario: saludo contextual, anillo de avance animado, mensaje según progreso y accesos directos a Capturar, Planner y Enfoque.
3. La navegación ahora comunica dirección mediante desplazamiento y fundido; el destino activo usa una cápsula suave y animación de color.
4. Tareas incorporó filtros Pendientes, Hoy, Hechas y Todas, además de reordenamiento animado al completar o cambiar de vista.
5. Hábitos muestra un resumen de jornada, mejor racha y siete puntos de historial por cada ritual.
6. Planner anima el cambio entre Día, Planner y Mes para conservar continuidad espacial.
7. El tomate y la taza del Pomodoro conservan la animación de inicio y ahora respiran y se balancean suavemente mientras el contador está activo.
8. Se añadieron dos widgets junto al resumen existente: Ritual permite completar el siguiente hábito desde inicio; Enfoque refleja una sesión activa con cronómetro real. Los tres leen el mismo almacenamiento y se actualizan tras cada cambio.
9. El servicio de enfoque publica una instantánea local de la sesión para sincronizar notificación y widget, y la elimina al detener o finalizar.

No se añadieron cuentas, red, analítica, base de datos ni bibliotecas de animación: Compose y `RemoteViews` cubren el alcance sin aumentar complejidad ni exposición de datos.

## 13. Widgets accionables y pantalla activa — versión 2.1

La colección creció de tres a cinco widgets y dejó de depender de accesos directos como interacción principal:

1. Resumen permite completar la próxima tarea y el siguiente hábito sin abandonar el inicio.
2. Ritual marca el siguiente hábito con un toque.
3. Enfoque inicia o detiene directamente una sesión de 25 minutos y mantiene un cronómetro real.
4. Mis próximos trazos presenta hasta tres tareas pendientes, cada una con su propio control de completado.
5. Jardín de rituales presenta hasta tres hábitos programados y permite marcarlos o desmarcarlos individualmente.

Todas las acciones escriben en el mismo `LocalStore`, actualizan simultáneamente las demás instancias y se reflejan al volver a la app. Un receptor de actualización y una recarga al abrir evitan vistas grises o antiguas después de reemplazar el APK en One UI.

El Pomodoro incorporó una superficie de pantalla siempre activa pensada para dejar el teléfono sobre el escritorio. Mantiene la ventana despierta solo mientras el contador corre, reduce el brillo de esa ventana, usa fondo casi negro y muestra una ilustración respirante: tomate durante enfoque y taza durante descanso. Pausar, continuar y salir siguen disponibles en esa misma vista. No intenta sustituir el Always On Display cerrado del fabricante, que Android no expone a aplicaciones normales.

## 14. Ilustración y widget principal — versión 2.2

Se sustituyeron los adornos procedurales principales por seis ilustraciones raster originales, generadas específicamente para Trazo con fondo transparente y acabado de lápiz, tinta y acuarela. El tomate y la taza conservan las animaciones Compose de respiración, balanceo y entrada; el dibujo de cuaderno reemplaza el adorno ambiguo que podía parecer una cara en los encabezados de Hoy, Tareas y Hábitos.

El widget 4×3 se rehízo para aprovechar el espacio con mejor jerarquía: fecha y saludo, progreso diario, una tarea accionable, un ritual accionable, Planner y control directo de Pomodoro. Tras probar una primera navegación con flechas, se retiraron esos botones para evitar ruido visual. Tareas y hábitos ahora son dos listas independientes de una sola fila visible: el usuario desliza la tarjeta y el contador de posición viaja dentro de cada elemento.

Se mantuvo `RemoteViews` nativo para evitar dependencias y procesos innecesarios. Las colecciones de widgets Android ofrecen gestos verticales —no un `ViewPager` horizontal—, por lo que la interacción final usa `ListView` y deslizamiento arriba/abajo compatible desde Android 8. Se descartó `StackView` durante la prueba física porque su perspectiva comprimía varias tarjetas en la altura disponible. Completar, desmarcar y abrir la sección se implementan mediante acciones por elemento del adaptador remoto.

La altura dejó de depender de dos tarjetas fijas. Ambas colecciones usan pesos equivalentes con mínimos seguros: absorben todo el espacio libre del widget, muestran más contenido cuando la instancia es alta y mantienen Planner/Enfoque alineados con el borde inferior.

Para hacer evidente que son dos zonas desplazables distintas, cada colección incorpora una banda compacta: coral para Tareas y verde para Hábitos, ambas con la indicación `DESLIZA ↕`. Es una señal visual y no un control adicional.

## 15. Widget adaptativo, categorías y respaldo — versión 2.3

La instancia principal dejó de tratar Tareas y Hábitos como dos mitades rígidas. Lee la altura concedida por el launcher y el contenido del día: en modo compacto conserva una sola sección prioritaria; en tamaños medianos y grandes muestra ambas; si una queda vacía, la otra absorbe la superficie libre. Cuando solo hay una colección, sus filas también redistribuyen la altura disponible hasta el borde inferior.

Cada widget guarda su propia configuración: secciones visibles, prioridad compacta, duración de enfoque (15/25/45/60), máximo de 2/4/6 elementos y tinta Coral/Hoja/Tinta. La pantalla de configuración usa Compose, sigue el estilo de papel de Trazo y se puede volver a abrir desde launchers compatibles.

Se eliminó la palabra `DESLIZA` repetida dentro de cada tarjeta; la pista permanece únicamente en la banda de sección y en el desvanecido vertical. Las filas de hábitos ahora eligen una de cinco ilustraciones raster transparentes de lápiz, tinta y acuarela.

Para que esa elección no dependa de adivinar el nombre, el modelo incorporó seis categorías: General, Hidratación, Autocuidado, Alimentación, Movimiento y Descanso. El editor permite elegirla explícitamente. Al cargar datos anteriores, Trazo realiza una inferencia inicial conservadora; desde ese momento la persona puede corregirla al editar. La categoría viaja también en el JSON versionado.

Finalmente, Hoy incluye una tarjeta de datos locales. Exportar crea un JSON legible con tareas, fechas, hábitos, categorías y cumplimientos; Importar valida completamente el archivo antes de sustituir el estado. Ambos flujos usan el Storage Access Framework de Android, sin permisos amplios de almacenamiento, cuentas ni conexión.

Durante la revisión visual se comprobó que recordatorios y copias competían con el contenido accionable de Hoy. Ambas tarjetas se trasladaron a una hoja de Ajustes; se abre tocando la insignia de engranaje integrada en la ilustración del encabezado. Hoy vuelve así a concentrarse en progreso, captura, siguiente tarea y rituales, mientras las opciones siguen a un solo toque.

También se eliminó el botón flotante `+` de Hoy porque duplicaba la acción Capturar. El botón se conserva en Tareas, Hábitos y Planner, donde actúa de forma contextual; Enfoque y Hoy mantienen una sola acción primaria clara.

Capturar dejó de asumir que todo apunte nuevo es una tarea. Ahora abre una hoja de decisión con dos destinos: Tarea para una acción puntual y Hábito para una práctica repetible; cada opción continúa en su compositor específico.

## 16. Sistema personal integrado — versión 3.0

Trazo 3.0 amplía el modelo local sin romper las copias ni los datos de 2.x. La versión de almacenamiento subió a 4 y conserva los campos antiguos de cumplimiento mientras migra cada hábito al nuevo progreso cuantificable.

Pasos desarrollados:

1. Tareas y hábitos admiten etiquetas, recordatorio propio, archivo y papelera. Borrar es reversible mediante Snackbar; desde Ajustes se puede restaurar o eliminar definitivamente.
2. Los recordatorios se programan con `AlarmManager`, se reconstruyen al reiniciar o cambiar la hora y muestran acciones para completar o posponer diez minutos sin abrir la app.
3. Los hábitos pueden ser binarios o medirse en veces, minutos y pasos. El editor incluye metas sugeridas y personalizadas; las tarjetas permiten sumar o restar progreso.
4. Capturar interpreta español sencillo —hoy, mañana, hora, importante, repetición diaria y `#etiquetas`— y abre el editor con un borrador revisable. «Dictar nota de voz» usa el reconocedor configurado en Android y no conserva grabaciones.
5. Tareas y Hábitos incorporan búsqueda por nombre, nota, categoría y etiquetas. Ajustes muestra estadísticas de siete días para tareas, cumplimiento de hábitos y sesiones/minutos de enfoque.
6. Se añadieron tema de sistema, claro y oscuro, texto ampliado, reducción de movimiento y respuesta háptica. Las superficies dejaron de usar crema fijo para mantener contraste correcto en modo oscuro.
7. Los widgets y resúmenes ignoran elementos archivados o eliminados y entienden las nuevas metas medibles.

No se incorporaron cuentas, red propia, publicidad, analítica ni dependencias nuevas. El reconocimiento de voz se delega a Android y puede funcionar offline si el idioma está descargado en el dispositivo.

### Corrección responsiva 3.0.1

La prueba con texto ampliado reveló que las acciones de Tareas y Hábitos competían horizontalmente con el título. En Hábitos, un nombre podía quedar reducido a una letra por línea y convertir la tarjeta en una columna de casi toda la pantalla.

Las tarjetas se reorganizaron en dos niveles: contenido y estado usan todo el ancho disponible; edición, fecha, archivo, borrado y progreso medible viven en una barra inferior independiente que puede desplazarse en pantallas muy estrechas. El resumen de racha también pasó a una columna compacta. La solución conserva objetivos táctiles amplios sin reducir artificialmente la tipografía elegida por la persona.

## 17. Intérprete de calendario y widget de voz — versión 3.1

El intérprete dejó de depender únicamente de “hoy”, “mañana” y “todos los días”. Ahora distingue recurrencia y fecha puntual según la frase:

- Varios días —`spinning lunes miércoles viernes`— crean un hábito con L–X–V.
- Un solo día —`entregar informe viernes`— crea una tarea fechada en la próxima ocurrencia.
- `cada martes`, `los martes`, `cada mañana`, `de lunes a viernes`, `entre semana` y `fines de semana` configuran calendarios recurrentes.
- `30 minutos`, `5 veces` y `8000 pasos` eligen automáticamente unidad y meta.
- Horas de 12/24 horas, mañana/tarde/noche, fechas numéricas y fechas como `20 de agosto` completan recordatorio y vencimiento.
- La categoría inferida asigna también su icono; spinning, bicicleta y ciclismo entran en Movimiento.

Se añadió el sexto widget, Captura inteligente. Su micrófono abre el reconocedor de Android, interpreta el resultado y guarda directamente la tarea o hábito; el widget muestra un mensaje de confirmación con el resultado. Tocar el resto de la tarjeta abre Trazo. `RemoteViews` no admite campos editables, por lo que la voz es la interacción directa y la captura dentro de la app conserva la revisión previa.
