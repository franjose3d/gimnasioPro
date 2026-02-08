package com.example.gimnasiopro.data

/**
 * Modelo que representa una serie de un ejercicio.
 */
data class SerieEntrenamiento(
    var pesoKg: Float = 0f
)

/**
 * Modelo que representa un ejercicio durante el entrenamiento activo.
 * Contiene los datos del ejercicio más los valores de peso por serie.
 */
data class EjercicioEntrenamiento(
    val ejercicio: Ejercicio,
    var series: MutableList<SerieEntrenamiento> = mutableListOf(
        SerieEntrenamiento(),
        SerieEntrenamiento(),
        SerieEntrenamiento()
    ),
    var completado: Boolean = false
) {
    // Para compatibilidad con código existente
    var repeticiones: Int
        get() = series.size
        set(value) { /* No usado */ }

    var pesoKg: Float
        get() = series.firstOrNull()?.pesoKg ?: 0f
        set(value) { series.firstOrNull()?.pesoKg = value }

    // Número de series visibles (mínimo 3, máximo 6)
    var seriesVisibles: Int = 3
        set(value) {
            field = value.coerceIn(3, 6)
            // Asegurar que hay suficientes series
            while (series.size < field) {
                series.add(SerieEntrenamiento())
            }
        }

    /**
     * Calcula el volumen total (suma de todos los pesos de las series).
     */
    fun calcularVolumenTotal(): Float {
        return series.take(seriesVisibles).sumOf { it.pesoKg.toDouble() }.toFloat()
    }
}
