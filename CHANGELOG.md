# Cambios

## 3.6.4

- El arrastre usa la posición final real al soltar, evitando movimientos que no se guardaban.
- Umbral horizontal reducido para cambiar de día con un gesto cómodo.
- La tarjeta acompaña el dedo con mayor claridad y conserva el ajuste vertical cada 15 minutos.
- Al cambiar de fecha, la agenda abre automáticamente el día de destino para mantener la tarea visible.

## 3.6.3

- Corrección responsive de los bloques horarios cortos en Agenda diaria.
- Rango horario legible con «inicio · hasta · fin», sin confundir la hora final con otra cita.
- Checklist compacto con progreso, siguiente paso y dependencia bloqueante, sin controles deformados.
- Altura mínima segura para conservar acciones de reprogramación incluso en tareas de 15 o 25 minutos.

## 3.6.2

- Nuevo centro de herramientas visible en Hoy con accesos directos a calendario, dependencias, categorías, plantillas y alarmas.
- La navegación ahora usa el nombre «Calendario» y expone claramente Agenda diaria, Semana y Mes.
- Cada vista del calendario explica qué muestra y cómo usar sus interacciones.
- El editor explica desde el inicio cómo crear plantillas y cuándo aparecen las dependencias.
- Pruebas instrumentadas específicas para descubrir el calendario y configurar dependencias desde Hoy.

## 3.6.1

- Editor visual de dependencias entre subtareas: cada paso puede depender de cualquiera de los pasos anteriores.
- Las tarjetas explican qué subtarea mantiene bloqueado un paso y evitan marcarlo antes de tiempo.
- Las dependencias se conservan correctamente al editar, guardar o reutilizar plantillas.

## 3.6.0

- Planner diario con bloques arrastrables entre horas y días, ajuste cada 15 minutos y vista previa antes de soltar.
- Subtareas con dependencias opcionales y sugerencia para completar la tarea principal al cerrar la lista.
- Categorías compartidas y editables para tareas, hábitos y plantillas.
- Plantillas de tareas que reutilizan duración, recurrencia, categoría, checklist, etiquetas y recordatorios sin copiar la fecha absoluta.
- Alarma crítica opcional por elemento, con pantalla completa y acciones para completar, posponer o abrir Trazo.
- Revisión nocturna configurable para resolver pendientes al final del día.
- Widget grande con la siguiente tarea horaria y progreso de subtareas.
- Compatibilidad de alarma crítica desde Android 8 y pruebas de layout, lint e instrumentación en Pixel API 35.

## 3.5.0

- Subtareas persistentes, editables y marcables desde las tarjetas de tareas.
- Progreso de subtareas en la agenda diaria y el planner semanal.
- Recordatorio individual por tarea o hábito: notificación, alarma previa, alarma a la hora o ambas.
- Captura inteligente ampliada para duraciones compuestas, fechas relativas, horarios expresados en palabras y tareas semanales explícitas.
- Prueba instrumentada de navegación Hoy → Enfoque para el emulador.
- Agenda diaria con bloques horarios, cálculo de duración y reprogramación rápida.
- Filtros combinables de estado, fecha y prioridad; métricas de planificación y avance global del checklist.
- Progreso de subtareas también en widgets de tareas.

## 3.4.1

- Base de tareas, hábitos, planner, Pomodoro, widgets y respaldo local.
