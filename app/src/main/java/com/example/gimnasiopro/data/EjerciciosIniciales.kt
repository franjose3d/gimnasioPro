package com.example.gimnasiopro.data

/**
 * Proveedor de datos iniciales de ejercicios.
 * Versión reducida para testing: solo 2 ejercicios por grupo muscular.
 * Los ejercicios completos se recuperarán automáticamente desde Firebase.
 */
object EjerciciosIniciales {

    /**
     * Lista reducida de ejercicios para testing.
     *
     * Total: 18 ejercicios (2 por cada grupo muscular)
     * Firebase debería tener 160+ ejercicios completos.
     */
    fun getEjerciciosIniciales(): List<Ejercicio> {
        return listOf(
            // ==================== PECTORALES (2/14) ====================
            Ejercicio(
                id = 1,
                grupoMuscular = "Pectorales",
                nombre = "Press de banca con barra",
                descripcion = "Empuje horizontal básico acostado para fuerza y volumen general del pecho.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 2,
                grupoMuscular = "Pectorales",
                nombre = "Press banca inclinado con mancuernas",
                descripcion = "Empuje en banco a 30-45° para enfatizar la parte superior del pectoral.",
                imagenUrl = null
            ),

            // ==================== ESPALDA (2/26) ====================
            Ejercicio(
                id = 15,
                grupoMuscular = "Espalda",
                nombre = "Remo con mancuerna a una mano",
                descripcion = "Tirón unilateral apoyado en banco, enfoca el dorsal ancho y corrige asimetrías.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 16,
                grupoMuscular = "Espalda",
                nombre = "Jalón con agarre ancho",
                descripcion = "Tracción vertical en polea para anchura de espalda (dorsales).",
                imagenUrl = null
            ),

            // ==================== HOMBROS (2/21) ====================
            Ejercicio(
                id = 41,
                grupoMuscular = "Hombros",
                nombre = "Press de hombro con mancuernas",
                descripcion = "Empuje vertical sentado o de pie para masa general del deltoides.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 42,
                grupoMuscular = "Hombros",
                nombre = "Elevación lateral con mancuernas",
                descripcion = "Abducción de brazos para aislar la cabeza lateral del hombro (anchura).",
                imagenUrl = null
            ),

            // ==================== BÍCEPS Y ANTEBRAZO (2/16) ====================
            Ejercicio(
                id = 62,
                grupoMuscular = "Bíceps y Antebrazo",
                nombre = "Curl con barra",
                descripcion = "Flexión de codos de pie con barra, el básico para masa de bíceps.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 63,
                grupoMuscular = "Bíceps y Antebrazo",
                nombre = "Curl alterno con mancuernas",
                descripcion = "Flexión alternando brazos, permite supinación (giro de muñeca).",
                imagenUrl = null
            ),

            // ==================== TRÍCEPS (2/15) ====================
            Ejercicio(
                id = 78,
                grupoMuscular = "Tríceps",
                nombre = "Extensión de tríceps tumbado (Rompecráneos)",
                descripcion = "Extensión de codos acostado llevando la barra a la frente.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 79,
                grupoMuscular = "Tríceps",
                nombre = "Extensión de tríceps en polea",
                descripcion = "Empuje hacia abajo en polea alta con barra recta o V.",
                imagenUrl = null
            ),

            // ==================== ABDOMINALES (2/12) ====================
            Ejercicio(
                id = 93,
                grupoMuscular = "Abdominales",
                nombre = "Crunch",
                descripcion = "Flexión clásica de tronco acostado para el recto abdominal.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 94,
                grupoMuscular = "Abdominales",
                nombre = "Crunch oblicuo",
                descripcion = "Flexión con rotación para trabajar los costados del abdomen.",
                imagenUrl = null
            ),

            // ==================== PIERNAS (2/30) ====================
            Ejercicio(
                id = 105,
                grupoMuscular = "Piernas",
                nombre = "Sentadilla",
                descripcion = "Flexión de rodillas y cadera con barra en espalda, el rey para piernas.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 106,
                grupoMuscular = "Piernas",
                nombre = "Prensa de piernas",
                descripcion = "Empuje de carga en máquina sentada/inclinada, alto volumen sin estabilidad.",
                imagenUrl = null
            ),

            // ==================== GLÚTEOS (2/26) ====================
            Ejercicio(
                id = 131,
                grupoMuscular = "Glúteos",
                nombre = "Puente con propio peso",
                descripcion = "Elevación de pelvis acostado para glúteo mayor.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 138,
                grupoMuscular = "Glúteos",
                nombre = "Elevaciones de cadera con maquina Smith (Hip Thrust)",
                descripcion = "Empuje de cadera con espalda apoyada y barra guiada, el mejor para glúteo.",
                imagenUrl = null
            ),

            // ==================== GEMELOS (2/2) ====================
            Ejercicio(
                id = 159,
                grupoMuscular = "Gemelos",
                nombre = "Elevación de gemelos sentado",
                descripcion = "Flexión plantar con rodillas dobladas, enfoca el músculo sóleo.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 160,
                grupoMuscular = "Gemelos",
                nombre = "Elevación de gemelos de pie",
                descripcion = "Flexión plantar con piernas rectas, enfoca el gastrocnemio (gemelo visible).",
                imagenUrl = null
            )
        )
    }
}
