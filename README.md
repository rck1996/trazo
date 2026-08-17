# Trazo

Trazo es una aplicación Android offline para gestionar tareas y hábitos sin cuentas, anuncios ni conexión. Su interfaz mezcla un cuaderno de papel con trazos imperfectos, colores cálidos y microinteracciones discretas.

## Funciones incluidas

- Crear, completar y eliminar tareas, con nota y marca de importancia.
- Crear hábitos con símbolo, categoría visual y días personalizados.
- Marcar el cumplimiento diario y calcular rachas según los días programados.
- Vista «Hoy» que reúne lo accionable y muestra progreso inmediato.
- Persistencia local privada en JSON versionado mediante `SharedPreferences`.
- Estados vacíos accionables, navegación clara y controles accesibles.
- Fechas opcionales y reprogramables para cada tarea.
- Calendario con agenda diaria, planner semanal y vista mensual.
- Pomodoro 25/5 o 50/10 asociado a una tarea pendiente.
- Resumen diario configurable mediante notificaciones locales.
- Notificación persistente mientras el Pomodoro está activo.
- Ilustraciones originales estilo sketch para tomate, taza, encabezados y widgets, integradas sin conexión.
- Edición completa de tareas y hábitos conservando su historial.
- Duraciones Pomodoro personalizadas entre 1–180 minutos de enfoque y 1–60 de pausa.
- Widget de inicio con agenda del día, pilas deslizables de tareas y hábitos, acciones rápidas y accesos a Planner/Enfoque.
- Inicio reconstruido como un estudio diario: progreso circular animado, acciones rápidas y mensajes adaptativos.
- Filtros de tareas, historial visual de siete días y resumen de rachas para hábitos.
- Transiciones direccionales entre secciones, cambio animado del calendario y respiración orgánica del Pomodoro.
- Seis widgets independientes e interactivos: resumen del día, hábito rápido, temporizador, tres próximas tareas, jardín de rituales y captura inteligente por voz.
- Modo Pomodoro de pantalla siempre activa, con brillo tenue, reloj OLED y dibujo animado adaptado a enfoque o descanso.
- Widget principal adaptativo: reparte su altura según el contenido y ofrece configuración independiente de secciones, prioridad compacta, Pomodoro, color y cantidad de tarjetas.
- Ilustraciones sketch por categoría de hábito: Hidratación, Autocuidado, Alimentación, Movimiento y Descanso.
- Copia de seguridad JSON local mediante los selectores de exportación e importación de Android.
- Recordatorios por tarea o hábito con hora personalizada y acciones «Hecho» y «Posponer 10 min».
- Hábitos medibles por veces, minutos o pasos, con meta personalizada y progreso diario.
- Captura inteligente en español con fechas, horas, prioridad y etiquetas, además de dictado por voz.
- Búsqueda por texto y etiquetas, archivo, papelera recuperable y acción Deshacer.
- Estadísticas semanales de tareas, hábitos y Pomodoro.
- Tema claro/oscuro/de sistema con contraste adaptado, texto ampliado, movimiento reducido y respuesta háptica configurable.

## Ejecutar

1. Abre esta carpeta con Android Studio Quail 1 (2026.1.1) o compatible.
2. Usa JDK 17 y permite que Android Studio instale Android SDK 37.0 si lo solicita.
3. Sincroniza Gradle y ejecuta el módulo `app` en un emulador o dispositivo Android 8.0+.

El proyecto fija AGP 9.2.1, Gradle 9.4.1, Kotlin/Compose Compiler 2.3.21, Android SDK 37.0 y Compose BOM 2026.06.00.

## Arquitectura mínima

```text
UI Compose → TrazoViewModel → LocalStore → SharedPreferences / JSON
                      ↓
               modelos inmutables
```

No se añadieron cuentas, red, analítica, inyección de dependencias ni base de datos: el tamaño actual no los justifica. `LocalStore` mantiene un campo de versión para permitir migraciones y copias portables.

## Verificación

Desde Android Studio o con el Gradle Wrapper incluido:

```bash
./gradlew test
./gradlew assembleDebug
```

El APK instalable más reciente también se copia a `artifacts/Trazo-3.1.0-debug.apk`.

Para instalarlo por USB con el teléfono autorizado:

```powershell
adb install -r -t artifacts\Trazo-3.1.0-debug.apk
```

Las pruebas unitarias cubren el cálculo de rachas, incluidos días no programados y días omitidos.

Consulta [docs/BITACORA.md](docs/BITACORA.md) para ver el desarrollo y las decisiones paso a paso.

Consulta [docs/VALIDACION.md](docs/VALIDACION.md) para ver la prueba automatizada y el recorrido realizado en el teléfono.
