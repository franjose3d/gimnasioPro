package com.example.gimnasiopro.presentation.rutinaspreestablecidas

/**
 * Modelo de datos para una rutina preestablecida.
 * Contiene una lista de IDs de ejercicios que se cargarán en Room.
 */
data class RutinaPreestablecida(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val nivel: Nivel,
    val objetivo: Objetivo,
    val diasSemana: Int,            // cuántos días/semana se entrena
    val ejercicioIds: List<Int>     // IDs de EjerciciosIniciales.kt
)

enum class Nivel(val etiqueta: String) {
    PRINCIPIANTE("Principiante"),
    INTERMEDIO("Intermedio"),
    AVANZADO("Avanzado"),
    ESPECIAL("Especiales")
}

enum class Objetivo(val etiqueta: String) {
    FUERZA("Fuerza"),
    HIPERTROFIA("Hipertrofia"),
    DEFINICION("Definición"),
    RESISTENCIA("Resistencia"),
    FULLBODY("Full Body")
}

/**
 * Catálogo de rutinas preestablecidas.
 * Cubre todas las combinaciones de Nivel × Objetivo.
 * Los IDs deben existir en EjerciciosIniciales.kt.
 */
object CatalogoRutinas {

    val todas: List<RutinaPreestablecida> = listOf(

        // ═══════════════════════════════════════════════════════
        // PRINCIPIANTE
        // ═══════════════════════════════════════════════════════

        RutinaPreestablecida(
            id = 1,
            nombre = "Full Body Principiante",
            descripcion = "Rutina completa de 3 días para empezar. Trabaja todos los grupos musculares con ejercicios básicos y seguros.",
            nivel = Nivel.PRINCIPIANTE,
            objetivo = Objetivo.FULLBODY,
            diasSemana = 3,
            ejercicioIds = listOf(12, 119, 131, 93, 62, 79, 97, 159)
        ),

        RutinaPreestablecida(
            id = 2,
            nombre = "Fuerza Base Principiante",
            descripcion = "Los 3 movimientos fundamentales del gimnasio: sentadilla, press banca y peso muerto. Ideal para construir fuerza desde cero.",
            nivel = Nivel.PRINCIPIANTE,
            objetivo = Objetivo.FUERZA,
            diasSemana = 3,
            ejercicioIds = listOf(1, 105, 32, 50, 16, 62)
        ),

        RutinaPreestablecida(
            id = 3,
            nombre = "Hipertrofia Inicial",
            descripcion = "Introducción al entrenamiento de volumen. 3 días con ejercicios compuestos y algunos de aislamiento para empezar a ganar masa.",
            nivel = Nivel.PRINCIPIANTE,
            objetivo = Objetivo.HIPERTROFIA,
            diasSemana = 3,
            ejercicioIds = listOf(1, 16, 41, 62, 79, 105, 131, 93)
        ),

        RutinaPreestablecida(
            id = 4,
            nombre = "Definición para Empezar",
            descripcion = "Combinación de ejercicios con tu propio peso y máquinas de bajo impacto para quemar grasa y tonificar sin riesgo de lesión.",
            nivel = Nivel.PRINCIPIANTE,
            objetivo = Objetivo.DEFINICION,
            diasSemana = 3,
            ejercicioIds = listOf(12, 119, 97, 93, 98, 131, 159, 118)
        ),

        RutinaPreestablecida(
            id = 5,
            nombre = "Resistencia Funcional",
            descripcion = "Circuito de 3 días con ejercicios de peso corporal para mejorar la resistencia muscular y cardiovascular desde cero.",
            nivel = Nivel.PRINCIPIANTE,
            objetivo = Objetivo.RESISTENCIA,
            diasSemana = 3,
            ejercicioIds = listOf(12, 119, 97, 93, 98, 117, 118, 101)
        ),

        // ═══════════════════════════════════════════════════════
        // INTERMEDIO
        // ═══════════════════════════════════════════════════════

        RutinaPreestablecida(
            id = 6,
            nombre = "Full Body Intermedio",
            descripcion = "Rutina de cuerpo completo en 4 días con mayor volumen y ejercicios compuestos más exigentes para seguir progresando.",
            nivel = Nivel.INTERMEDIO,
            objetivo = Objetivo.FULLBODY,
            diasSemana = 4,
            ejercicioIds = listOf(1, 19, 50, 62, 79, 105, 139, 93, 159, 42)
        ),

        RutinaPreestablecida(
            id = 7,
            nombre = "Fuerza Intermedia 5×5",
            descripcion = "Protocolo clásico de 5 series × 5 repeticiones. Sentadilla, press banca, peso muerto, press militar y remo. 3 días alternos.",
            nivel = Nivel.INTERMEDIO,
            objetivo = Objetivo.FUERZA,
            diasSemana = 3,
            ejercicioIds = listOf(105, 1, 32, 50, 19)
        ),

        RutinaPreestablecida(
            id = 8,
            nombre = "Torso / Pierna",
            descripcion = "División clásica en 4 días: 2 sesiones de torso (empuje + tirón) y 2 de piernas. Equilibrado y muy efectivo.",
            nivel = Nivel.INTERMEDIO,
            objetivo = Objetivo.HIPERTROFIA,
            diasSemana = 4,
            ejercicioIds = listOf(1, 6, 3, 19, 16, 41, 42, 105, 106, 109, 139, 42)
        ),

        RutinaPreestablecida(
            id = 9,
            nombre = "Push / Pull / Legs",
            descripcion = "PPL clásico en 6 días. Día de empuje (pecho/hombros/tríceps), día de tirón (espalda/bíceps) y día de piernas.",
            nivel = Nivel.INTERMEDIO,
            objetivo = Objetivo.HIPERTROFIA,
            diasSemana = 6,
            ejercicioIds = listOf(1, 5, 50, 79, 80, 19, 16, 62, 63, 105, 106, 109, 139, 42)
        ),

        RutinaPreestablecida(
            id = 10,
            nombre = "Definición Intermedia",
            descripcion = "4 días de entrenamiento con descansos cortos y superse­ries para maximizar el gasto calórico y preservar la masa muscular.",
            nivel = Nivel.INTERMEDIO,
            objetivo = Objetivo.DEFINICION,
            diasSemana = 4,
            ejercicioIds = listOf(1, 19, 50, 105, 97, 93, 98, 42, 79, 62)
        ),

        RutinaPreestablecida(
            id = 11,
            nombre = "Resistencia Muscular Intermedia",
            descripcion = "Circuito de 4 días con altas repeticiones y poco descanso. Mejora la capacidad aeróbica y la resistencia de todos los grupos.",
            nivel = Nivel.INTERMEDIO,
            objetivo = Objetivo.RESISTENCIA,
            diasSemana = 4,
            ejercicioIds = listOf(12, 119, 97, 93, 98, 117, 118, 101, 25, 159, 160)
        ),

        // ═══════════════════════════════════════════════════════
        // AVANZADO
        // ═══════════════════════════════════════════════════════

        RutinaPreestablecida(
            id = 12,
            nombre = "Full Body Avanzado",
            descripcion = "Cuerpo completo de alta intensidad en 4 días con movimientos olímpicos y compuestos pesados. Para atletas con base sólida.",
            nivel = Nivel.AVANZADO,
            objetivo = Objetivo.FULLBODY,
            diasSemana = 4,
            ejercicioIds = listOf(1, 32, 50, 25, 105, 139, 91, 104, 160, 38)
        ),

        RutinaPreestablecida(
            id = 13,
            nombre = "Fuerza Avanzada (4 días)",
            descripcion = "Programa de fuerza con periodización lineal en 4 días. Énfasis en sentadilla, press banca, peso muerto y press militar.",
            nivel = Nivel.AVANZADO,
            objetivo = Objetivo.FUERZA,
            diasSemana = 4,
            ejercicioIds = listOf(105, 113, 1, 10, 32, 34, 50, 19, 82)
        ),

        RutinaPreestablecida(
            id = 14,
            nombre = "Fuerza Powerlifting",
            descripcion = "Especialización en los 3 levantamientos de powerlifting: sentadilla, press banca y peso muerto. Periodización por bloques.",
            nivel = Nivel.AVANZADO,
            objetivo = Objetivo.FUERZA,
            diasSemana = 4,
            ejercicioIds = listOf(105, 113, 122, 1, 10, 82, 32, 34, 39, 50)
        ),

        RutinaPreestablecida(
            id = 15,
            nombre = "Weider 5 días",
            descripcion = "La clásica rutina de los culturistas: un grupo muscular grande por día durante 5 días. Máximo volumen por músculo.",
            nivel = Nivel.AVANZADO,
            objetivo = Objetivo.HIPERTROFIA,
            diasSemana = 5,
            ejercicioIds = listOf(1, 5, 7, 10, 19, 32, 36, 50, 54, 62, 75, 79, 91, 105, 122, 139, 143)
        ),

        RutinaPreestablecida(
            id = 16,
            nombre = "PPL Avanzado 6 días",
            descripcion = "Push/Pull/Legs repetido 2 veces por semana con alta carga de volumen. Para atletas que pueden recuperarse rápidamente.",
            nivel = Nivel.AVANZADO,
            objetivo = Objetivo.HIPERTROFIA,
            diasSemana = 6,
            ejercicioIds = listOf(1, 6, 5, 50, 54, 79, 80, 91, 19, 25, 16, 62, 65, 75, 105, 110, 109, 115, 139, 42)
        ),

        RutinaPreestablecida(
            id = 17,
            nombre = "Definición Avanzada",
            descripcion = "5 días de entrenamiento con técnicas avanzadas (drop sets, supersets) y cardio integrado para definición máxima.",
            nivel = Nivel.AVANZADO,
            objetivo = Objetivo.DEFINICION,
            diasSemana = 5,
            ejercicioIds = listOf(1, 19, 50, 105, 32, 97, 93, 98, 104, 118, 42, 79, 62, 159, 162)
        ),

        RutinaPreestablecida(
            id = 18,
            nombre = "Resistencia de Alto Rendimiento",
            descripcion = "6 días de entrenamiento de resistencia muscular y cardiovascular. Circuitos metabólicos con ejercicios compuestos.",
            nivel = Nivel.AVANZADO,
            objetivo = Objetivo.RESISTENCIA,
            diasSemana = 6,
            ejercicioIds = listOf(25, 12, 119, 105, 32, 97, 93, 98, 117, 118, 101, 104, 159, 161, 162, 163)
        ),

        // Refuerzo de catalogo en niveles base
        RutinaPreestablecida(
            id = 31,
            nombre = "Empuje y Tiron Inicial",
            descripcion = "Rutina principiante de 4 dias para mejorar tecnica en patrones de empuje y tiron sin sobrecargar articulaciones.",
            nivel = Nivel.PRINCIPIANTE,
            objetivo = Objetivo.HIPERTROFIA,
            diasSemana = 4,
            ejercicioIds = listOf(12, 1, 16, 15, 41, 42, 62, 79, 119, 131)
        ),

        RutinaPreestablecida(
            id = 32,
            nombre = "Fuerza y Core Intermedio",
            descripcion = "Bloque intermedio con foco en fuerza general y estabilidad del core para progresar sin estancarse.",
            nivel = Nivel.INTERMEDIO,
            objetivo = Objetivo.FUERZA,
            diasSemana = 4,
            ejercicioIds = listOf(105, 1, 32, 50, 19, 82, 97, 102, 104)
        ),

        RutinaPreestablecida(
            id = 33,
            nombre = "Upper Lower Avanzado",
            descripcion = "Division avanzada de frecuencia 2 para tren superior e inferior, ideal para seguir progresando en fuerza y masa.",
            nivel = Nivel.AVANZADO,
            objetivo = Objetivo.HIPERTROFIA,
            diasSemana = 4,
            ejercicioIds = listOf(1, 5, 19, 25, 50, 62, 79, 105, 113, 115, 139, 159)
        ),

        // ═══════════════════════════════════════════════════════
        // ESPECIALES / TEMATICAS
        // ═══════════════════════════════════════════════════════

        RutinaPreestablecida(
            id = 19,
            nombre = "Solo Piernas y Gluteos",
            descripcion = "Rutina dedicada exclusivamente al tren inferior. 4 días con enfoque en cuádriceps, isquios y glúteos.",
            nivel = Nivel.ESPECIAL,
            objetivo = Objetivo.HIPERTROFIA,
            diasSemana = 4,
            ejercicioIds = listOf(105, 106, 107, 109, 110, 115, 122, 139, 140, 141, 143, 156, 159, 160)
        ),

        RutinaPreestablecida(
            id = 20,
            nombre = "Solo Tren Superior",
            descripcion = "Trabajo exclusivo de pecho, espalda, hombros y brazos en 4 días. Ideal para complementar con deportes de piernas.",
            nivel = Nivel.ESPECIAL,
            objetivo = Objetivo.HIPERTROFIA,
            diasSemana = 4,
            ejercicioIds = listOf(1, 6, 7, 19, 16, 25, 41, 42, 50, 62, 63, 79, 80, 88)
        ),

        RutinaPreestablecida(
            id = 21,
            nombre = "Cardio y Tonificación",
            descripcion = "Rutina de 5 días mezclando cardio en máquinas y ejercicios de tonificación muscular. Perfecta para perder grasa.",
            nivel = Nivel.ESPECIAL,
            objetivo = Objetivo.DEFINICION,
            diasSemana = 5,
            ejercicioIds = listOf(161, 162, 163, 164, 119, 131, 97, 93, 12, 98)
        ),

        RutinaPreestablecida(
            id = 22,
            nombre = "Fuerza en Casa",
            descripcion = "Rutina de fuerza sin máquinas. Solo ejercicios con peso corporal y mancuernas para entrenar en casa.",
            nivel = Nivel.ESPECIAL,
            objetivo = Objetivo.FUERZA,
            diasSemana = 3,
            ejercicioIds = listOf(12, 119, 131, 62, 79, 97, 98, 93)
        ),

        RutinaPreestablecida(
            id = 23,
            nombre = "Hipertrofia en Casa",
            descripcion = "Máxima ganancia de músculo sin máquinas. Ejercicios con mancuernas y peso corporal para los que entrenan en casa.",
            nivel = Nivel.ESPECIAL,
            objetivo = Objetivo.HIPERTROFIA,
            diasSemana = 4,
            ejercicioIds = listOf(6, 7, 15, 35, 41, 43, 62, 67, 81, 83, 114, 116, 131, 132)
        ),

        RutinaPreestablecida(
            id = 24,
            nombre = "Resistencia Cardio Total",
            descripcion = "5 días combinando máquinas de cardio y ejercicios funcionales de resistencia para mejorar la capacidad aeróbica global.",
            nivel = Nivel.ESPECIAL,
            objetivo = Objetivo.RESISTENCIA,
            diasSemana = 5,
            ejercicioIds = listOf(161, 162, 163, 164, 117, 118, 97, 101, 104, 98)
        ),

        RutinaPreestablecida(
            id = 25,
            nombre = "Core y Abdomen Total",
            descripcion = "Programa especializado en core de 4 días. Crunches, planchas, rueda, oblicuos y anti-rotación para un abdomen de acero.",
            nivel = Nivel.ESPECIAL,
            objetivo = Objetivo.DEFINICION,
            diasSemana = 4,
            ejercicioIds = listOf(93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104)
        ),

        RutinaPreestablecida(
            id = 26,
            nombre = "Gluteos Prioritarios",
            descripcion = "Rutina de 4 días con foco total en glúteos. Combina hip thrust, sentadillas, abductores y puentes con distintas variaciones.",
            nivel = Nivel.ESPECIAL,
            objetivo = Objetivo.HIPERTROFIA,
            diasSemana = 4,
            ejercicioIds = listOf(131, 132, 133, 138, 139, 140, 141, 142, 143, 145, 149, 152, 153)
        ),

        RutinaPreestablecida(
            id = 27,
            nombre = "Gluteos Avanzados",
            descripcion = "Programa avanzado de 5 días centrado en glúteos y piernas. Altas cargas en hip thrust, sentadilla búlgara y peso muerto.",
            nivel = Nivel.ESPECIAL,
            objetivo = Objetivo.HIPERTROFIA,
            diasSemana = 5,
            ejercicioIds = listOf(105, 115, 122, 139, 138, 140, 141, 143, 145, 148, 149, 151, 152, 156)
        ),

        RutinaPreestablecida(
            id = 28,
            nombre = "Full Body Fuerza-Resistencia",
            descripcion = "Combina series de fuerza pesada con finales de resistencia. 3 días para quienes quieren lo mejor de los dos mundos.",
            nivel = Nivel.ESPECIAL,
            objetivo = Objetivo.RESISTENCIA,
            diasSemana = 3,
            ejercicioIds = listOf(1, 32, 105, 50, 19, 97, 118, 159)
        ),

        RutinaPreestablecida(
            id = 29,
            nombre = "Hombros y Brazos Prioritarios",
            descripcion = "Programa de 4 días para desarrollar hombros anchos y brazos voluminosos. Ideal si quieres ganar presencia en el tren superior.",
            nivel = Nivel.ESPECIAL,
            objetivo = Objetivo.HIPERTROFIA,
            diasSemana = 4,
            ejercicioIds = listOf(41, 42, 43, 44, 47, 50, 60, 62, 63, 65, 66, 79, 80, 88)
        ),

        RutinaPreestablecida(
            id = 30,
            nombre = "Definicion Funcional Avanzada",
            descripcion = "6 días con técnicas de alta intensidad (HIIT, circuitos) y ejercicios multiarticulares para el máximo gasto calórico diario.",
            nivel = Nivel.ESPECIAL,
            objetivo = Objetivo.DEFINICION,
            diasSemana = 6,
            ejercicioIds = listOf(32, 105, 1, 25, 117, 118, 97, 104, 151, 161, 162, 163, 164, 93, 98)
        )
    )
}
