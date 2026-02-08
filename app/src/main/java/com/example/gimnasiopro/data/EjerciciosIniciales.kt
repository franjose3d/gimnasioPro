package com.example.gimnasiopro.data

/**
 * Proveedor de datos iniciales de ejercicios.
 * Aquí se definen todos los ejercicios predefinidos de la aplicación,
 * organizados por grupo muscular.
 */
object EjerciciosIniciales {

    /**
     * Lista de todos los ejercicios predefinidos.
     *
     * Grupos musculares disponibles:
     * - Pectorales
     * - Espalda
     * - Hombros
     * - Bíceps y Antebrazo
     * - Tríceps
     * - Abdominales
     * - Piernas
     * - Glúteos
     * - Gemelos
     */
    fun getEjerciciosIniciales(): List<Ejercicio> {
        return listOf(
            // ==================== PECTORALES ====================
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
            Ejercicio(
                id = 3,
                grupoMuscular = "Pectorales",
                nombre = "Aperturas en máquina Peck Deck o Contractora",
                descripcion = "Aislamiento del pecho mediante aducción de brazos sentados sin involucrar tríceps.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 4,
                grupoMuscular = "Pectorales",
                nombre = "Cruce de poleas",
                descripcion = "Ejercicio de aislamiento con cables de pie para trabajar la parte interna y contorno del pecho.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 5,
                grupoMuscular = "Pectorales",
                nombre = "Press de banca inclinado con barra",
                descripcion = "Empuje con barra en banco inclinado para masa en la zona clavicular del pecho.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 6,
                grupoMuscular = "Pectorales",
                nombre = "Press de banca con mancuernas",
                descripcion = "Similar al press con barra, pero permite mayor rango de movimiento y trabajo estabilizador.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 7,
                grupoMuscular = "Pectorales",
                nombre = "Aperturas con mancuernas",
                descripcion = "Movimiento de abrazo acostado para estirar y aislar las fibras del pecho.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 8,
                grupoMuscular = "Pectorales",
                nombre = "Aperturas Inclinadas con mancuernas",
                descripcion = "Movimiento de apertura en banco inclinado para aislar la parte superior.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 9,
                grupoMuscular = "Pectorales",
                nombre = "Press de banca en máquina sentado",
                descripcion = "Empuje horizontal guiado, ideal para principiantes o finalizar rutinas con seguridad.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 10,
                grupoMuscular = "Pectorales",
                nombre = "Press de banca declinado con barra",
                descripcion = "Empuje con la cabeza más baja que las caderas, enfatiza la parte inferior del pecho.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 11,
                grupoMuscular = "Pectorales",
                nombre = "Press de banca declinado con mancuernas",
                descripcion = "Variante con mancuernas del declinado para mayor rango en la zona inferior.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 12,
                grupoMuscular = "Pectorales",
                nombre = "Flexiones",
                descripcion = "Empuje con el propio peso corporal desde el suelo, trabaja pecho, tríceps y core.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 13,
                grupoMuscular = "Pectorales",
                nombre = "Press Landmine",
                descripcion = "Empuje diagonal de pie con barra anclada, excelente para pecho superior y la zona interna.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 14,
                grupoMuscular = "Pectorales",
                nombre = "Press Svend",
                descripcion = "Presión isométrica de discos frente al pecho, enfoca la conexión mente-músculo y la parte interna.",
                imagenUrl = null
            ),

            // ==================== ESPALDA ====================
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
            Ejercicio(
                id = 17,
                grupoMuscular = "Espalda",
                nombre = "Remo en máquina",
                descripcion = "Tracción horizontal guiada para trabajar la densidad de la espalda media.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 18,
                grupoMuscular = "Espalda",
                nombre = "Jalón al pecho con agarre cerrado",
                descripcion = "Tracción vertical con agarre estrecho, enfatiza la parte baja del dorsal.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 19,
                grupoMuscular = "Espalda",
                nombre = "Remo con barra",
                descripcion = "Ejercicio compuesto de tirón inclinado para densidad y fuerza general de espalda.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 20,
                grupoMuscular = "Espalda",
                nombre = "Jalón tras nuca",
                descripcion = "Variante del jalón bajando la barra tras la cabeza (requiere buena movilidad de hombros).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 21,
                grupoMuscular = "Espalda",
                nombre = "Jalón al pecho con agarre invertido",
                descripcion = "Tracción con palmas hacia la cara, involucra más los bíceps y la parte baja del dorsal.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 22,
                grupoMuscular = "Espalda",
                nombre = "Jalón en polea con cuerda",
                descripcion = "Tracción alta con cuerda para mayor rango de movimiento y contracción dorsal.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 23,
                grupoMuscular = "Espalda",
                nombre = "Remo en barra T",
                descripcion = "Tirón con barra anclada al suelo, excelente para grosor de la espalda media.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 24,
                grupoMuscular = "Espalda",
                nombre = "Remo inclinado con barra con agarre supinado",
                descripcion = "Remo con palmas hacia arriba, mayor énfasis en dorsales bajos y bíceps.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 25,
                grupoMuscular = "Espalda",
                nombre = "Elevaciones en barra fija (Dominadas)",
                descripcion = "Tracción vertical con peso corporal para anchura y fuerza funcional.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 26,
                grupoMuscular = "Espalda",
                nombre = "Elevaciones tras nuca en barra fija",
                descripcion = "Dominada llevando la barra tras la cabeza (uso avanzado).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 27,
                grupoMuscular = "Espalda",
                nombre = "Elevaciones en barra fija con agarre supinado (Chin-ups)",
                descripcion = "Dominada con palmas hacia la cara, mayor trabajo de bíceps.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 28,
                grupoMuscular = "Espalda",
                nombre = "Jalón dorsal con brazos rectos",
                descripcion = "Aislamiento del dorsal mediante extensión de hombro con polea alta (brazos estirados).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 29,
                grupoMuscular = "Espalda",
                nombre = "Remo inclinado con mancuernas",
                descripcion = "Variante del remo con barra usando mancuernas para libertad de movimiento.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 30,
                grupoMuscular = "Espalda",
                nombre = "Pullover con mancuerna",
                descripcion = "Ejercicio que estira el dorsal y trabaja el serrato, acostado en banco.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 31,
                grupoMuscular = "Espalda",
                nombre = "Pullover con barra",
                descripcion = "Variante con barra, permite mantener tensión constante en el dorsal.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 32,
                grupoMuscular = "Espalda",
                nombre = "Peso muerto con barra",
                descripcion = "Levantamiento desde el suelo, rey de los ejercicios para cadena posterior y fuerza total.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 33,
                grupoMuscular = "Espalda",
                nombre = "Peso muerto sumo con barra",
                descripcion = "Peso muerto con piernas muy abiertas, mayor énfasis en glúteos y aductores.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 34,
                grupoMuscular = "Espalda",
                nombre = "Peso muerto con barra hexagonal",
                descripcion = "Peso muerto con agarre neutro a los lados, reduce tensión lumbar y enfoca cuádriceps.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 35,
                grupoMuscular = "Espalda",
                nombre = "Peso muerto con mancuernas",
                descripcion = "Variante para aprender la técnica o trabajar con menos carga axial.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 36,
                grupoMuscular = "Espalda",
                nombre = "Encogimiento de hombros con barra",
                descripcion = "Elevación de hombros para trabajar la porción superior del trapecio.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 37,
                grupoMuscular = "Espalda",
                nombre = "Encogimiento de hombros con mancuernas",
                descripcion = "Variante con mancuernas a los lados para trapecios.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 38,
                grupoMuscular = "Espalda",
                nombre = "Face Pull (Jalón a la cara)",
                descripcion = "Tracción alta con cuerda hacia la frente, vital para deltoides posterior y salud del hombro.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 39,
                grupoMuscular = "Espalda",
                nombre = "Rack Pulls",
                descripcion = "Peso muerto parcial desde las rodillas, permite carga máxima para densificar la espalda alta.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 40,
                grupoMuscular = "Espalda",
                nombre = "Remo Meadows",
                descripcion = "Remo unilateral con barra anclada (Landmine), ángulo único para estirar el dorsal.",
                imagenUrl = null
            ),

            // ==================== HOMBROS ====================
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
            Ejercicio(
                id = 43,
                grupoMuscular = "Hombros",
                nombre = "Elevación frontal con mancuernas",
                descripcion = "Elevación al frente para la cabeza anterior del deltoides.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 44,
                grupoMuscular = "Hombros",
                nombre = "Cruces inversos en polea alta",
                descripcion = "Apertura inversa con cables para la parte posterior del hombro.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 45,
                grupoMuscular = "Hombros",
                nombre = "Press de hombros en Máquina Smith",
                descripcion = "Empuje vertical guiado, permite mover más carga con seguridad.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 46,
                grupoMuscular = "Hombros",
                nombre = "Remo alto con barra",
                descripcion = "Tirón vertical pegado al cuerpo hasta el mentón, trabaja trapecios y deltoides laterales.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 47,
                grupoMuscular = "Hombros",
                nombre = "Elevaciones posteriores para hombros (Pájaro)",
                descripcion = "Elevación lateral con torso inclinado para deltoides posterior.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 48,
                grupoMuscular = "Hombros",
                nombre = "Elevación lateral con cable a una mano",
                descripcion = "Aislamiento lateral con tensión constante gracias a la polea.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 49,
                grupoMuscular = "Hombros",
                nombre = "Press Militar con mancuernas",
                descripcion = "Empuje estricto sobre la cabeza para fuerza y estabilidad.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 50,
                grupoMuscular = "Hombros",
                nombre = "Press Militar",
                descripcion = "El clásico empuje con barra de pie, constructor de fuerza base para el torso superior.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 51,
                grupoMuscular = "Hombros",
                nombre = "Elevaciones frontales con cable a una mano",
                descripcion = "Aislamiento frontal con polea para tensión continua.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 52,
                grupoMuscular = "Hombros",
                nombre = "Elevaciones frontales con barra",
                descripcion = "Elevación de barra al frente, permite más carga en la parte anterior.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 53,
                grupoMuscular = "Hombros",
                nombre = "Press militar sentado con barra",
                descripcion = "Empuje vertical estricto eliminando el impulso de las piernas.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 54,
                grupoMuscular = "Hombros",
                nombre = "Press tras nuca sentado",
                descripcion = "Empuje vertical bajando la barra tras la cabeza (cuidado con la articulación).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 55,
                grupoMuscular = "Hombros",
                nombre = "Press militar de pie",
                descripcion = "Ejecución estándar de pie, requiere gran estabilidad del core.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 56,
                grupoMuscular = "Hombros",
                nombre = "Press militar de pie tras nuca",
                descripcion = "Variante avanzada de pie tras la cabeza.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 57,
                grupoMuscular = "Hombros",
                nombre = "Elevación frontal con mancuernas alternas agarre neutro",
                descripcion = "Elevación frontal con palmas enfrentadas (martillo).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 58,
                grupoMuscular = "Hombros",
                nombre = "Elevación frontal con un brazo en polea baja agarre neutro",
                descripcion = "Aislamiento unilateral frontal con agarre neutro.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 59,
                grupoMuscular = "Hombros",
                nombre = "Elevación frontal con mancuerna a dos manos",
                descripcion = "Sujetando una sola mancuerna con ambas manos al frente.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 60,
                grupoMuscular = "Hombros",
                nombre = "Press Arnold",
                descripcion = "Press con rotación de muñecas, trabaja las tres cabezas del deltoides en un solo movimiento.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 61,
                grupoMuscular = "Hombros",
                nombre = "Rotaciones Cubanas",
                descripcion = "Rotación externa con mancuernas o polea, fortalece el manguito rotador y previene lesiones.",
                imagenUrl = null
            ),

            // ==================== BÍCEPS Y ANTEBRAZO ====================
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
            Ejercicio(
                id = 64,
                grupoMuscular = "Bíceps y Antebrazo",
                nombre = "Curl con cuerda en polea",
                descripcion = "Flexión con cuerda en polea baja, agarre neutro para braquial.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 65,
                grupoMuscular = "Bíceps y Antebrazo",
                nombre = "Curl con barra EZ",
                descripcion = "Flexión con barra ondulada, reduce estrés en muñecas.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 66,
                grupoMuscular = "Bíceps y Antebrazo",
                nombre = "Curl de predicador con barra EZ",
                descripcion = "Flexión apoyando brazos en banco Scott para aislar y evitar inercia.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 67,
                grupoMuscular = "Bíceps y Antebrazo",
                nombre = "Curl alterno de martillo con mancuernas",
                descripcion = "Flexión con agarre neutro, enfoca braquial y antebrazo.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 68,
                grupoMuscular = "Bíceps y Antebrazo",
                nombre = "Curl inclinado con mancuernas",
                descripcion = "Flexión sentado en banco inclinado hacia atrás, estira la cabeza larga del bíceps.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 69,
                grupoMuscular = "Bíceps y Antebrazo",
                nombre = "Curl concentrado con mancuernas",
                descripcion = "Flexión sentado apoyando el codo en el muslo, aislamiento total.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 70,
                grupoMuscular = "Bíceps y Antebrazo",
                nombre = "Curl de cable en polea baja a una mano",
                descripcion = "Aislamiento unilateral con tensión constante.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 71,
                grupoMuscular = "Bíceps y Antebrazo",
                nombre = "Curl de cable con barra recta en polea baja",
                descripcion = "Variante de cable simulando la barra para tensión continua.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 72,
                grupoMuscular = "Bíceps y Antebrazo",
                nombre = "Curl de cable en polea alta de pie",
                descripcion = "Doble bíceps en polea alta, trabaja la contracción en pico.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 73,
                grupoMuscular = "Bíceps y Antebrazo",
                nombre = "Curl de muñeca con barra sentado",
                descripcion = "Flexión de muñeca con antebrazos apoyados para flexores del antebrazo.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 74,
                grupoMuscular = "Bíceps y Antebrazo",
                nombre = "Extensión de muñeca con barra sentado",
                descripcion = "Extensión de muñeca (palmas abajo) para extensores del antebrazo.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 75,
                grupoMuscular = "Bíceps y Antebrazo",
                nombre = "Curl de barra invertido",
                descripcion = "Curl de bíceps con palmas hacia abajo, gran trabajo de antebrazo y braquiorradial.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 76,
                grupoMuscular = "Bíceps y Antebrazo",
                nombre = "Curl Zottman",
                descripcion = "Subida normal y bajada con agarre inverso, trabaja bíceps y antebrazos simultáneamente.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 77,
                grupoMuscular = "Bíceps y Antebrazo",
                nombre = "Curl Araña (Spider Curl)",
                descripcion = "Curl bocabajo en banco inclinado, máxima tensión en la parte corta y cero inercia.",
                imagenUrl = null
            ),

            // ==================== TRÍCEPS ====================
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
            Ejercicio(
                id = 80,
                grupoMuscular = "Tríceps",
                nombre = "Extensión de tríceps en polea con cuerda",
                descripcion = "Empuje hacia abajo abriendo la cuerda al final para contracción lateral.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 81,
                grupoMuscular = "Tríceps",
                nombre = "Extensión de tríceps con mancuernas por encima de la cabeza",
                descripcion = "Extensión vertical tras la nuca, estira la cabeza larga.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 82,
                grupoMuscular = "Tríceps",
                nombre = "Press Banca con agarre cerrado",
                descripcion = "Press de banca con manos juntas, transfiere el trabajo del pecho al tríceps.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 83,
                grupoMuscular = "Tríceps",
                nombre = "Patadas traseras",
                descripcion = "Extensión de codo hacia atrás con mancuerna y torso inclinado.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 84,
                grupoMuscular = "Tríceps",
                nombre = "Extensión de tríceps con cable de agarre inverso con barra",
                descripcion = "Empuje en polea con palmas hacia arriba.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 85,
                grupoMuscular = "Tríceps",
                nombre = "Extensión de tríceps con cable a una mano",
                descripcion = "Aislamiento unilateral en polea (agarre prono).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 86,
                grupoMuscular = "Tríceps",
                nombre = "Extensión de tríceps con cable a una mano con agarre supinado",
                descripcion = "Aislamiento unilateral con agarre inverso.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 87,
                grupoMuscular = "Tríceps",
                nombre = "Extensión de tríceps con mancuernas tumbado",
                descripcion = "Variante del rompecráneos usando mancuernas (agarre neutro).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 88,
                grupoMuscular = "Tríceps",
                nombre = "Press francés sentado con barra",
                descripcion = "Extensión tras nuca sentado con barra (generalmente EZ).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 89,
                grupoMuscular = "Tríceps",
                nombre = "Fondos en banco",
                descripcion = "Flexión de brazos apoyado en un banco, pies en el suelo o elevados.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 90,
                grupoMuscular = "Tríceps",
                nombre = "Fondos en barras paralelas",
                descripcion = "Ejercicio de peso corporal vertical, gran constructor de masa para tríceps.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 91,
                grupoMuscular = "Tríceps",
                nombre = "Press JM",
                descripcion = "Híbrido entre press cerrado y extensión, constructor masivo de fuerza y tamaño.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 92,
                grupoMuscular = "Tríceps",
                nombre = "Press Tate",
                descripcion = "Extensión de codos hacia afuera con mancuernas en banco plano, aísla cabezas lateral y medial.",
                imagenUrl = null
            ),

            // ==================== ABDOMINALES ====================
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
            Ejercicio(
                id = 95,
                grupoMuscular = "Abdominales",
                nombre = "Abdominales en máquina",
                descripcion = "Flexión de tronco con resistencia guiada.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 96,
                grupoMuscular = "Abdominales",
                nombre = "Abdominales con cuerda en polea alta",
                descripcion = "Crunch de rodillas jalando peso desde polea alta.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 97,
                grupoMuscular = "Abdominales",
                nombre = "Plancha",
                descripcion = "Isometría apoyando antebrazos y pies, estabilidad del core.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 98,
                grupoMuscular = "Abdominales",
                nombre = "Elevación de piernas",
                descripcion = "Flexión de cadera acostado o colgado para la parte baja del abdomen.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 99,
                grupoMuscular = "Abdominales",
                nombre = "Encogimientos de rodillas para abdominales",
                descripcion = "Llevar rodillas al pecho sentado o acostado.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 100,
                grupoMuscular = "Abdominales",
                nombre = "Abdominales con brazos estirados",
                descripcion = "Crunch manteniendo brazos rectos hacia el techo o atrás.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 101,
                grupoMuscular = "Abdominales",
                nombre = "Plancha con flexión",
                descripcion = "Transición dinámica entre posición de plancha y posición de flexión de brazos.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 102,
                grupoMuscular = "Abdominales",
                nombre = "Press Pallof",
                descripcion = "Isometría lateral con polea o banda, el mejor ejercicio anti-rotación para el core.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 103,
                grupoMuscular = "Abdominales",
                nombre = "Leñador (Woodchoppers)",
                descripcion = "Rotación controlada de tronco con polea, trabajo intenso de oblicuos.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 104,
                grupoMuscular = "Abdominales",
                nombre = "Rueda Abdominal (Ab Wheel)",
                descripcion = "Rodillo al frente, altísima activación del recto abdominal y control del core.",
                imagenUrl = null
            ),

            // ==================== PIERNAS ====================
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
            Ejercicio(
                id = 107,
                grupoMuscular = "Piernas",
                nombre = "Extensión de piernas",
                descripcion = "Aislamiento de cuádriceps en máquina sentada.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 108,
                grupoMuscular = "Piernas",
                nombre = "Zancada",
                descripcion = "Paso adelante flexionando ambas rodillas (trabajo unilateral).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 109,
                grupoMuscular = "Piernas",
                nombre = "Curl de pierna tumbado en máquina de femoral",
                descripcion = "Flexión de rodilla acostado para isquiotibiales.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 110,
                grupoMuscular = "Piernas",
                nombre = "Sentadilla Hack",
                descripcion = "Sentadilla guiada en máquina con respaldo inclinado, énfasis en cuádriceps.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 111,
                grupoMuscular = "Piernas",
                nombre = "Curl de piernas sentado",
                descripcion = "Flexión de rodillas sentado para isquiotibiales.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 112,
                grupoMuscular = "Piernas",
                nombre = "Extensión a una pierna",
                descripcion = "Aislamiento de cuádriceps unilateral en máquina.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 113,
                grupoMuscular = "Piernas",
                nombre = "Sentadilla frontal",
                descripcion = "Sentadilla con barra apoyada en deltoides frontales, mayor énfasis en cuádriceps y core.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 114,
                grupoMuscular = "Piernas",
                nombre = "Peso muerto rumano con mancuernas",
                descripcion = "Bisagra de cadera con piernas semirrígidas para isquios/glúteo.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 115,
                grupoMuscular = "Piernas",
                nombre = "Peso muerto rumano con barra",
                descripcion = "Igual que el anterior pero con barra, permite más carga.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 116,
                grupoMuscular = "Piernas",
                nombre = "Sentadilla Goblet con mancuerna",
                descripcion = "Sentadilla sujetando una mancuerna al pecho, buena para técnica y profundidad.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 117,
                grupoMuscular = "Piernas",
                nombre = "Salto Rodillas al Pecho",
                descripcion = "Salto explosivo elevando rodillas, potencia y cardio.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 118,
                grupoMuscular = "Piernas",
                nombre = "Burpees",
                descripcion = "Ejercicio metabólico de cuerpo completo que incluye flexión y salto.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 119,
                grupoMuscular = "Piernas",
                nombre = "Sentadillas con propio peso",
                descripcion = "Sentadilla básica sin carga externa.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 120,
                grupoMuscular = "Piernas",
                nombre = "1.5 repeticiones de sentadillas",
                descripcion = "Bajar, subir a la mitad, volver a bajar y subir completo (aumenta tensión).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 121,
                grupoMuscular = "Piernas",
                nombre = "Sentadillas con balón medicinal",
                descripcion = "Sentadilla sujetando un balón medicinal.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 122,
                grupoMuscular = "Piernas",
                nombre = "Sentadilla búlgara con barra",
                descripcion = "Sentadilla unilateral con pie trasero elevado y barra en espalda.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 123,
                grupoMuscular = "Piernas",
                nombre = "Sentadilla búlgara con propio peso",
                descripcion = "Igual que la anterior sin carga, gran trabajo de estabilidad y glúteo.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 124,
                grupoMuscular = "Piernas",
                nombre = "Sentadilla con mini banda",
                descripcion = "Sentadilla con banda sobre rodillas para activar glúteo medio.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 125,
                grupoMuscular = "Piernas",
                nombre = "Sentadilla con salto",
                descripcion = "Sentadilla explosiva despegando del suelo.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 126,
                grupoMuscular = "Piernas",
                nombre = "Sentadilla isométrica apoyado sobre la pared",
                descripcion = "Mantener posición de silla contra la pared (resistencia).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 127,
                grupoMuscular = "Piernas",
                nombre = "Peso muerto con balón medicinal",
                descripcion = "Variante ligera de peso muerto.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 128,
                grupoMuscular = "Piernas",
                nombre = "Peso muerto a una pierna",
                descripcion = "Bisagra de cadera unilateral, gran demanda de equilibrio e isquios.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 129,
                grupoMuscular = "Piernas",
                nombre = "Sentadilla sumo con kettlebell",
                descripcion = "Sentadilla piernas abiertas sujetando pesa rusa al centro (aductores/glúteo).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 130,
                grupoMuscular = "Piernas",
                nombre = "Ejercicio buenos días con barra",
                descripcion = "Bisagra de cadera con barra en espalda, énfasis lumbar e isquios.",
                imagenUrl = null
            ),
            // ==================== GLÚTEOS ====================
            Ejercicio(
                id = 131,
                grupoMuscular = "Glúteos",
                nombre = "Puente con propio peso",
                descripcion = "Elevación de pelvis acostado para glúteo mayor.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 132,
                grupoMuscular = "Glúteos",
                nombre = "Puentes a una pierna",
                descripcion = "Variante unilateral del puente para mayor intensidad.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 133,
                grupoMuscular = "Glúteos",
                nombre = "Puente con bandas",
                descripcion = "Puente de glúteo añadiendo resistencia elástica.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 134,
                grupoMuscular = "Piernas",
                nombre = "Caminata de pato",
                descripcion = "Caminar en posición de sentadilla profunda (movilidad y cuádriceps).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 135,
                grupoMuscular = "Glúteos",
                nombre = "Ejercicio Superman en cuadrupedia",
                descripcion = "Elevación de brazo y pierna contraria (Bird-Dog) para core/espalda baja.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 136,
                grupoMuscular = "Piernas",
                nombre = "Los Groiners",
                descripcion = "Estiramiento dinámico de cadera en posición de plancha.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 137,
                grupoMuscular = "Glúteos",
                nombre = "Hidrantes",
                descripcion = "Abducción de cadera en cuadrupedia para glúteo medio.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 138,
                grupoMuscular = "Glúteos",
                nombre = "Elevaciones de cadera con maquina Smith (Hip Thrust)",
                descripcion = "Empuje de cadera con espalda apoyada y barra guiada, el mejor para glúteo.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 139,
                grupoMuscular = "Glúteos",
                nombre = "Elevaciones de cadera con barra (Hip Thrust)",
                descripcion = "Igual que el anterior con barra libre.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 140,
                grupoMuscular = "Glúteos",
                nombre = "Abducciones de cadera sentado con banda",
                descripcion = "Separar rodillas sentado con banda elástica.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 141,
                grupoMuscular = "Glúteos",
                nombre = "Abducción de cadera con máquina",
                descripcion = "Máquina específica para abrir piernas (glúteo medio/menor).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 142,
                grupoMuscular = "Glúteos",
                nombre = "Abducción con polea",
                descripcion = "Patada lateral con cable de pie.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 143,
                grupoMuscular = "Glúteos",
                nombre = "Elevaciones en posición de rana con propio peso",
                descripcion = "Puente de glúteo con plantas de los pies juntas (Pump frog).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 144,
                grupoMuscular = "Glúteos",
                nombre = "Elevaciones cortas en posición de rana con maquina Smith",
                descripcion = "Variante con carga guiada del ejercicio anterior.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 145,
                grupoMuscular = "Glúteos",
                nombre = "Almejas laterales con banda",
                descripcion = "Apertura de rodilla acostado de lado (rotación externa/glúteo).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 146,
                grupoMuscular = "Glúteos",
                nombre = "Levantamiento de pierna acostado de lado",
                descripcion = "Abducción simple de pierna recta acostado.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 147,
                grupoMuscular = "Piernas",
                nombre = "Elevaciones de bíceps femoral con máquina GHD",
                descripcion = "Extensión de cadera/flexión rodilla avanzada (Glute Ham Raise).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 148,
                grupoMuscular = "Piernas",
                nombre = "Step Up con mancuernas",
                descripcion = "Subida al cajón con peso, trabajo unilateral de pierna y glúteo.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 149,
                grupoMuscular = "Glúteos",
                nombre = "Caminata lateral con minibanda",
                descripcion = "Pasos laterales con banda en rodillas/tobillos para glúteo medio.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 150,
                grupoMuscular = "Piernas",
                nombre = "Elevaciones de rodilla",
                descripcion = "Flexión de cadera de pie o colgado.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 151,
                grupoMuscular = "Glúteos",
                nombre = "Columpios con kettlebell (Swings)",
                descripcion = "Péndulo explosivo de cadera, cadena posterior y cardio.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 152,
                grupoMuscular = "Glúteos",
                nombre = "Contragolpe con cable",
                descripcion = "Patada trasera con polea baja para glúteo mayor.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 153,
                grupoMuscular = "Glúteos",
                nombre = "Patadas de burro",
                descripcion = "Extensión de cadera hacia atrás en cuadrupedia (glúteo).",
                imagenUrl = null
            ),
            Ejercicio(
                id = 154,
                grupoMuscular = "Glúteos",
                nombre = "Elevaciones de cadera acostado lateralmente",
                descripcion = "Plancha lateral dinámica elevando caderas.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 155,
                grupoMuscular = "Piernas",
                nombre = "Sentadilla Posturas funcionales",
                descripcion = "Variaciones de sentadilla enfocadas en movimiento natural.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 156,
                grupoMuscular = "Piernas",
                nombre = "Curl Nórdico",
                descripcion = "Bajada controlada con tobillos fijados, el mejor preventivo para lesiones de isquios.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 157,
                grupoMuscular = "Piernas",
                nombre = "Máquina de Aductores",
                descripcion = "Cierre de piernas en máquina, fortalece la cara interna del muslo.",
                imagenUrl = null
            ),
            Ejercicio(
                id = 158,
                grupoMuscular = "Piernas",
                nombre = "Sissy Squat",
                descripcion = "Sentadilla con el torso inclinado atrás, máximo aislamiento y estiramiento del cuádriceps.",
                imagenUrl = null
            ),

            // ==================== GEMELOS ====================
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

