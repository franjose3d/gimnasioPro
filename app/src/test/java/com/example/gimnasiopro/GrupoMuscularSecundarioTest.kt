package com.example.gimnasiopro

import com.example.gimnasiopro.data.Ejercicio
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para la funcionalidad de grupos musculares secundarios.
 *
 * Valida:
 * - Filtrado de ejercicios por grupo principal y secundario
 * - Cálculo de estadísticas (conteo) incluyendo grupos secundarios
 * - Comportamiento con ejercicios sin grupo secundario
 */
class GrupoMuscularSecundarioTest {

    // ==================== Datos de prueba ====================

    private val facePull = Ejercicio(
        id = 38,
        grupoMuscular = "Espalda",
        grupoMuscularSecundario = "Hombros",
        nombre = "Face Pull (Jalón a la cara)",
        descripcion = "Tracción alta con cuerda hacia la frente."
    )

    private val pressBanca = Ejercicio(
        id = 1,
        grupoMuscular = "Pectorales",
        grupoMuscularSecundario = "Tríceps",
        nombre = "Press de banca con barra",
        descripcion = "Empuje horizontal básico."
    )

    private val sentadilla = Ejercicio(
        id = 105,
        grupoMuscular = "Piernas",
        grupoMuscularSecundario = "Glúteos",
        nombre = "Sentadilla",
        descripcion = "Flexión de rodillas y cadera."
    )

    private val curlBiceps = Ejercicio(
        id = 62,
        grupoMuscular = "Bíceps y Antebrazo",
        grupoMuscularSecundario = null,
        nombre = "Curl con barra",
        descripcion = "Flexión de codos con barra."
    )

    private val dominadas = Ejercicio(
        id = 25,
        grupoMuscular = "Espalda",
        grupoMuscularSecundario = "Bíceps y Antebrazo",
        nombre = "Elevaciones en barra fija (Dominadas)",
        descripcion = "Tracción vertical con peso corporal."
    )

    private val pesoMuertoRumano = Ejercicio(
        id = 115,
        grupoMuscular = "Piernas",
        grupoMuscularSecundario = "Glúteos",
        nombre = "Peso muerto rumano con barra",
        descripcion = "Bisagra de cadera con piernas semirrígidas."
    )

    private val hipThrust = Ejercicio(
        id = 139,
        grupoMuscular = "Glúteos",
        grupoMuscularSecundario = "Piernas",
        nombre = "Elevaciones de cadera con barra (Hip Thrust)",
        descripcion = "Empuje de cadera con barra libre."
    )

    // ==================== Tests de modelo de datos ====================

    @Test
    fun `ejercicio con grupo secundario tiene ambos campos correctamente asignados`() {
        assertEquals("Espalda", facePull.grupoMuscular)
        assertEquals("Hombros", facePull.grupoMuscularSecundario)
    }

    @Test
    fun `ejercicio sin grupo secundario tiene null`() {
        assertNull(curlBiceps.grupoMuscularSecundario)
    }

    @Test
    fun `ejercicio con grupo secundario no pierde su grupo principal`() {
        assertEquals("Pectorales", pressBanca.grupoMuscular)
        assertNotNull(pressBanca.grupoMuscularSecundario)
    }

    // ==================== Tests de filtrado por grupo muscular ====================

    private fun filtrarPorGrupo(ejercicios: List<Ejercicio>, grupo: String): List<Ejercicio> {
        return ejercicios.filter {
            it.grupoMuscular == grupo || it.grupoMuscularSecundario == grupo
        }
    }

    @Test
    fun `face pull aparece en Espalda y en Hombros`() {
        val todos = listOf(facePull, pressBanca, sentadilla, curlBiceps)

        val espalda = filtrarPorGrupo(todos, "Espalda")
        val hombros = filtrarPorGrupo(todos, "Hombros")

        assertTrue(espalda.contains(facePull))
        assertTrue(hombros.contains(facePull))
    }

    @Test
    fun `press banca aparece en Pectorales y en Triceps`() {
        val todos = listOf(facePull, pressBanca, sentadilla, curlBiceps)

        val pectorales = filtrarPorGrupo(todos, "Pectorales")
        val triceps = filtrarPorGrupo(todos, "Tríceps")

        assertTrue(pectorales.contains(pressBanca))
        assertTrue(triceps.contains(pressBanca))
    }

    @Test
    fun `sentadilla aparece en Piernas y en Gluteos`() {
        val todos = listOf(facePull, pressBanca, sentadilla, curlBiceps)

        val piernas = filtrarPorGrupo(todos, "Piernas")
        val gluteos = filtrarPorGrupo(todos, "Glúteos")

        assertTrue(piernas.contains(sentadilla))
        assertTrue(gluteos.contains(sentadilla))
    }

    @Test
    fun `ejercicio sin grupo secundario solo aparece en su grupo principal`() {
        val todos = listOf(facePull, pressBanca, sentadilla, curlBiceps)

        val biceps = filtrarPorGrupo(todos, "Bíceps y Antebrazo")
        val pectorales = filtrarPorGrupo(todos, "Pectorales")

        assertTrue(biceps.contains(curlBiceps))
        assertFalse(pectorales.contains(curlBiceps))
    }

    @Test
    fun `filtro por grupo devuelve lista vacia si no hay coincidencias`() {
        val todos = listOf(pressBanca, sentadilla, curlBiceps)

        val gemelos = filtrarPorGrupo(todos, "Gemelos")

        assertTrue(gemelos.isEmpty())
    }

    // ==================== Tests de cálculo de estadísticas ====================

    private fun calcularConteoPorGrupo(ejercicios: List<Ejercicio>): Map<String, Int> {
        val resultado = mutableMapOf<String, Int>()
        ejercicios.forEach { ej ->
            resultado[ej.grupoMuscular] = (resultado[ej.grupoMuscular] ?: 0) + 1
            ej.grupoMuscularSecundario?.let { secundario ->
                resultado[secundario] = (resultado[secundario] ?: 0) + 1
            }
        }
        return resultado
    }

    @Test
    fun `face pull cuenta para Espalda y para Hombros en estadisticas`() {
        val ejerciciosEntrenados = listOf(facePull)

        val conteo = calcularConteoPorGrupo(ejerciciosEntrenados)

        assertEquals(1, conteo["Espalda"])
        assertEquals(1, conteo["Hombros"])
    }

    @Test
    fun `ejercicio sin grupo secundario solo cuenta para su grupo en estadisticas`() {
        val ejerciciosEntrenados = listOf(curlBiceps)

        val conteo = calcularConteoPorGrupo(ejerciciosEntrenados)

        assertEquals(1, conteo["Bíceps y Antebrazo"])
        assertEquals(1, conteo.size)
    }

    @Test
    fun `porcentajes suman correctamente con ejercicios multimusculares`() {
        // Face Pull: Espalda + Hombros → 2 entradas
        // Curl: Bíceps → 1 entrada
        // Total: 3
        val ejerciciosEntrenados = listOf(facePull, curlBiceps)

        val conteo = calcularConteoPorGrupo(ejerciciosEntrenados)
        val total = conteo.values.sum()
        val porcentajes = conteo.mapValues { (_, v) -> ((v.toFloat() / total) * 100).toInt() }

        assertEquals(3, total)
        assertEquals(33, porcentajes["Espalda"])
        assertEquals(33, porcentajes["Hombros"])
        assertEquals(33, porcentajes["Bíceps y Antebrazo"])
    }

    @Test
    fun `conteo correcto con multiples ejercicios multimusculares`() {
        // Sentadilla: Piernas + Glúteos
        // Hip Thrust: Glúteos + Piernas
        // Peso muerto rumano: Piernas + Glúteos
        val ejerciciosEntrenados = listOf(sentadilla, hipThrust, pesoMuertoRumano)

        val conteo = calcularConteoPorGrupo(ejerciciosEntrenados)

        assertEquals(3, conteo["Piernas"])
        assertEquals(3, conteo["Glúteos"])
    }

    @Test
    fun `conteo con mezcla de ejercicios simples y multimusculares`() {
        // Press banca: Pectorales + Tríceps
        // Dominadas: Espalda + Bíceps y Antebrazo
        // Curl: Bíceps y Antebrazo (solo)
        val ejerciciosEntrenados = listOf(pressBanca, dominadas, curlBiceps)

        val conteo = calcularConteoPorGrupo(ejerciciosEntrenados)

        assertEquals(1, conteo["Pectorales"])
        assertEquals(1, conteo["Tríceps"])
        assertEquals(1, conteo["Espalda"])
        assertEquals(2, conteo["Bíceps y Antebrazo"]) // dominadas + curl
    }

    @Test
    fun `conteo con lista vacia devuelve mapa vacio`() {
        val conteo = calcularConteoPorGrupo(emptyList())

        assertTrue(conteo.isEmpty())
    }

    // ==================== Tests de ejercicios iniciales ====================

    @Test
    fun `face pull de EjerciciosIniciales tiene grupo secundario correcto`() {
        val ejercicios = com.example.gimnasiopro.data.EjerciciosIniciales.getEjerciciosIniciales()
        val facePullReal = ejercicios.find { it.id == 38L }

        assertNotNull(facePullReal)
        assertEquals("Espalda", facePullReal!!.grupoMuscular)
        assertEquals("Hombros", facePullReal.grupoMuscularSecundario)
    }

    @Test
    fun `press de banca de EjerciciosIniciales tiene grupo secundario Triceps`() {
        val ejercicios = com.example.gimnasiopro.data.EjerciciosIniciales.getEjerciciosIniciales()
        val pressBancaReal = ejercicios.find { it.id == 1L }

        assertNotNull(pressBancaReal)
        assertEquals("Pectorales", pressBancaReal!!.grupoMuscular)
        assertEquals("Tríceps", pressBancaReal.grupoMuscularSecundario)
    }

    @Test
    fun `sentadilla de EjerciciosIniciales tiene grupo secundario Gluteos`() {
        val ejercicios = com.example.gimnasiopro.data.EjerciciosIniciales.getEjerciciosIniciales()
        val sentadillaReal = ejercicios.find { it.id == 105L }

        assertNotNull(sentadillaReal)
        assertEquals("Piernas", sentadillaReal!!.grupoMuscular)
        assertEquals("Glúteos", sentadillaReal.grupoMuscularSecundario)
    }

    @Test
    fun `hip thrust de EjerciciosIniciales tiene grupo secundario Piernas`() {
        val ejercicios = com.example.gimnasiopro.data.EjerciciosIniciales.getEjerciciosIniciales()
        val hipThrustReal = ejercicios.find { it.id == 139L }

        assertNotNull(hipThrustReal)
        assertEquals("Glúteos", hipThrustReal!!.grupoMuscular)
        assertEquals("Piernas", hipThrustReal.grupoMuscularSecundario)
    }

    @Test
    fun `dominadas de EjerciciosIniciales tienen grupo secundario Biceps`() {
        val ejercicios = com.example.gimnasiopro.data.EjerciciosIniciales.getEjerciciosIniciales()
        val dominadasReal = ejercicios.find { it.id == 25L }

        assertNotNull(dominadasReal)
        assertEquals("Espalda", dominadasReal!!.grupoMuscular)
        assertEquals("Bíceps y Antebrazo", dominadasReal.grupoMuscularSecundario)
    }

    @Test
    fun `todos los ejercicios tienen al menos grupo muscular principal`() {
        val ejercicios = com.example.gimnasiopro.data.EjerciciosIniciales.getEjerciciosIniciales()

        ejercicios.forEach { ejercicio ->
            assertFalse(
                "Ejercicio '${ejercicio.nombre}' tiene grupoMuscular vacío",
                ejercicio.grupoMuscular.isBlank()
            )
        }
    }

    @Test
    fun `ejercicios de aislamiento no tienen grupo secundario`() {
        val ejercicios = com.example.gimnasiopro.data.EjerciciosIniciales.getEjerciciosIniciales()

        // Extensión de piernas (id=107) es aislamiento puro de cuádriceps
        val extension = ejercicios.find { it.id == 107L }
        assertNotNull(extension)
        assertNull(extension!!.grupoMuscularSecundario)

        // Curl concentrado (id=69) es aislamiento puro de bíceps
        val curlConcentrado = ejercicios.find { it.id == 69L }
        assertNotNull(curlConcentrado)
        assertNull(curlConcentrado!!.grupoMuscularSecundario)
    }
}
