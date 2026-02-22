package com.example.gimnasiopro.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios para RutinaRepository.
 * Verifica la lógica de agregar y eliminar rutinas dinámicamente.
 */
class RutinaRepositoryTest {

    private lateinit var fakeDao: FakeRutinaDao
    private lateinit var repository: RutinaRepository

    @Before
    fun setup() {
        fakeDao = FakeRutinaDao()
        repository = RutinaRepository(fakeDao)
    }

    // ===== Tests para agregarNuevaRutina =====

    @Test
    fun `agregarNuevaRutina crea rutina con numero correcto cuando no hay rutinas`() = runBlocking {
        // Given: No hay rutinas
        fakeDao.rutinas.clear()

        // When: Agregamos una nueva rutina
        val nuevoNumero = repository.agregarNuevaRutina()

        // Then: Se crea la rutina 1
        assertEquals(1, nuevoNumero)
        assertEquals(1, fakeDao.rutinas.size)
        assertEquals("Rutina 1", fakeDao.rutinas[0].nombre)
    }

    @Test
    fun `agregarNuevaRutina incrementa numero correctamente`() = runBlocking {
        // Given: Ya hay 5 rutinas
        fakeDao.rutinas.clear()
        repeat(5) { i ->
            fakeDao.rutinas.add(Rutina(numeroRutina = i + 1, nombre = "Rutina ${i + 1}"))
        }

        // When: Agregamos una nueva rutina
        val nuevoNumero = repository.agregarNuevaRutina()

        // Then: Se crea la rutina 6
        assertEquals(6, nuevoNumero)
        assertEquals(6, fakeDao.rutinas.size)
    }

    @Test
    fun `agregarNuevaRutina devuelve null cuando se alcanza el maximo`() = runBlocking {
        // Given: Ya hay 20 rutinas (máximo)
        fakeDao.rutinas.clear()
        repeat(20) { i ->
            fakeDao.rutinas.add(Rutina(numeroRutina = i + 1, nombre = "Rutina ${i + 1}"))
        }

        // When: Intentamos agregar otra rutina
        val resultado = repository.agregarNuevaRutina(maxRutinas = 20)

        // Then: Devuelve null y no se agrega ninguna
        assertNull(resultado)
        assertEquals(20, fakeDao.rutinas.size)
    }

    @Test
    fun `agregarNuevaRutina respeta limite personalizado`() = runBlocking {
        // Given: Ya hay 5 rutinas con límite de 5
        fakeDao.rutinas.clear()
        repeat(5) { i ->
            fakeDao.rutinas.add(Rutina(numeroRutina = i + 1, nombre = "Rutina ${i + 1}"))
        }

        // When: Intentamos agregar con límite de 5
        val resultado = repository.agregarNuevaRutina(maxRutinas = 5)

        // Then: No se puede agregar
        assertNull(resultado)
    }

    @Test
    fun `agregarNuevaRutina crea rutina con ejercicios vacios`() = runBlocking {
        // Given: No hay rutinas
        fakeDao.rutinas.clear()

        // When: Agregamos una nueva rutina
        repository.agregarNuevaRutina()

        // Then: La rutina tiene ejercicios vacíos
        assertTrue(fakeDao.rutinas[0].ejercicioIds.isEmpty())
    }

    // ===== Tests para eliminarUltimaRutina =====

    @Test
    fun `eliminarUltimaRutina elimina la rutina con mayor numero`() = runBlocking {
        // Given: Hay 5 rutinas
        fakeDao.rutinas.clear()
        repeat(5) { i ->
            fakeDao.rutinas.add(Rutina(numeroRutina = i + 1, nombre = "Rutina ${i + 1}"))
        }

        // When: Eliminamos la última
        val resultado = repository.eliminarUltimaRutina()

        // Then: Se elimina correctamente
        assertTrue(resultado)
        assertEquals(4, fakeDao.rutinas.size)
        assertNull(fakeDao.rutinas.find { it.numeroRutina == 5 })
    }

    @Test
    fun `eliminarUltimaRutina devuelve false cuando solo queda una rutina`() = runBlocking {
        // Given: Solo hay 1 rutina
        fakeDao.rutinas.clear()
        fakeDao.rutinas.add(Rutina(numeroRutina = 1, nombre = "Rutina 1"))

        // When: Intentamos eliminar
        val resultado = repository.eliminarUltimaRutina(minRutinas = 1)

        // Then: No se elimina
        assertFalse(resultado)
        assertEquals(1, fakeDao.rutinas.size)
    }

    @Test
    fun `eliminarUltimaRutina devuelve false cuando no hay rutinas`() = runBlocking {
        // Given: No hay rutinas
        fakeDao.rutinas.clear()

        // When: Intentamos eliminar
        val resultado = repository.eliminarUltimaRutina()

        // Then: Devuelve false
        assertFalse(resultado)
    }

    @Test
    fun `eliminarUltimaRutina respeta minimo personalizado`() = runBlocking {
        // Given: Hay 3 rutinas con mínimo de 3
        fakeDao.rutinas.clear()
        repeat(3) { i ->
            fakeDao.rutinas.add(Rutina(numeroRutina = i + 1, nombre = "Rutina ${i + 1}"))
        }

        // When: Intentamos eliminar con mínimo de 3
        val resultado = repository.eliminarUltimaRutina(minRutinas = 3)

        // Then: No se elimina
        assertFalse(resultado)
        assertEquals(3, fakeDao.rutinas.size)
    }

    // ===== Tests para getMaxNumeroRutina =====

    @Test
    fun `getMaxNumeroRutina devuelve el numero mas alto`() = runBlocking {
        // Given: Rutinas 1, 3, 5
        fakeDao.rutinas.clear()
        fakeDao.rutinas.add(Rutina(numeroRutina = 1, nombre = "Rutina 1"))
        fakeDao.rutinas.add(Rutina(numeroRutina = 3, nombre = "Rutina 3"))
        fakeDao.rutinas.add(Rutina(numeroRutina = 5, nombre = "Rutina 5"))

        // When: Obtenemos el máximo
        val max = repository.getMaxNumeroRutina()

        // Then: Es 5
        assertEquals(5, max)
    }

    @Test
    fun `getMaxNumeroRutina devuelve 0 cuando no hay rutinas`() = runBlocking {
        // Given: No hay rutinas
        fakeDao.rutinas.clear()

        // When: Obtenemos el máximo
        val max = repository.getMaxNumeroRutina()

        // Then: Es 0
        assertEquals(0, max)
    }

    // ===== Tests para getCountRutinas =====

    @Test
    fun `getCountRutinas devuelve el conteo correcto`() = runBlocking {
        // Given: Hay 7 rutinas
        fakeDao.rutinas.clear()
        repeat(7) { i ->
            fakeDao.rutinas.add(Rutina(numeroRutina = i + 1, nombre = "Rutina ${i + 1}"))
        }

        // When: Obtenemos el conteo
        val count = repository.getCountRutinas()

        // Then: Es 7
        assertEquals(7, count)
    }

    @Test
    fun `getCountRutinas devuelve 0 cuando no hay rutinas`() = runBlocking {
        // Given: No hay rutinas
        fakeDao.rutinas.clear()

        // When: Obtenemos el conteo
        val count = repository.getCountRutinas()

        // Then: Es 0
        assertEquals(0, count)
    }

    // ===== Tests de integración incrementar/decrementar =====

    @Test
    fun `ciclo completo de agregar y eliminar rutinas`() = runBlocking {
        // Given: Empezamos con 10 rutinas
        fakeDao.rutinas.clear()
        repeat(10) { i ->
            fakeDao.rutinas.add(Rutina(numeroRutina = i + 1, nombre = "Rutina ${i + 1}"))
        }

        // When: Agregamos 2 y eliminamos 1
        val nuevo1 = repository.agregarNuevaRutina()
        val nuevo2 = repository.agregarNuevaRutina()
        val eliminado = repository.eliminarUltimaRutina()

        // Then: Tenemos 11 rutinas
        assertEquals(11, nuevo1)
        assertEquals(12, nuevo2)
        assertTrue(eliminado)
        assertEquals(11, fakeDao.rutinas.size)
    }

    @Test
    fun `agregar rutinas hasta el limite y luego no poder agregar mas`() = runBlocking {
        // Given: Empezamos con 18 rutinas
        fakeDao.rutinas.clear()
        repeat(18) { i ->
            fakeDao.rutinas.add(Rutina(numeroRutina = i + 1, nombre = "Rutina ${i + 1}"))
        }

        // When: Agregamos hasta el límite
        val r19 = repository.agregarNuevaRutina()
        val r20 = repository.agregarNuevaRutina()
        val r21 = repository.agregarNuevaRutina() // No debería poder

        // Then: Solo se agregan 2
        assertEquals(19, r19)
        assertEquals(20, r20)
        assertNull(r21)
        assertEquals(20, fakeDao.rutinas.size)
    }

    @Test
    fun `eliminar rutinas hasta el minimo y luego no poder eliminar mas`() = runBlocking {
        // Given: Empezamos con 3 rutinas
        fakeDao.rutinas.clear()
        repeat(3) { i ->
            fakeDao.rutinas.add(Rutina(numeroRutina = i + 1, nombre = "Rutina ${i + 1}"))
        }

        // When: Eliminamos hasta el mínimo
        val e1 = repository.eliminarUltimaRutina()
        val e2 = repository.eliminarUltimaRutina()
        val e3 = repository.eliminarUltimaRutina() // No debería poder

        // Then: Solo se eliminan 2
        assertTrue(e1)
        assertTrue(e2)
        assertFalse(e3)
        assertEquals(1, fakeDao.rutinas.size)
    }
}

/**
 * Fake implementation de RutinaDao para tests unitarios.
 */
class FakeRutinaDao : RutinaDao {
    val rutinas = mutableListOf<Rutina>()

    override fun getAllRutinas(): Flow<List<Rutina>> = flowOf(rutinas.toList())

    override fun getRutinaByNumero(numeroRutina: Int): Flow<Rutina?> =
        flowOf(rutinas.find { it.numeroRutina == numeroRutina })

    override suspend fun getRutinaByNumeroSync(numeroRutina: Int): Rutina? =
        rutinas.find { it.numeroRutina == numeroRutina }

    override suspend fun insertRutina(rutina: Rutina) {
        val existing = rutinas.indexOfFirst { it.numeroRutina == rutina.numeroRutina }
        if (existing >= 0) {
            rutinas[existing] = rutina
        } else {
            rutinas.add(rutina)
        }
    }

    override suspend fun insertRutinas(rutinas: List<Rutina>) {
        rutinas.forEach { insertRutina(it) }
    }

    override suspend fun updateRutina(rutina: Rutina) {
        val index = this.rutinas.indexOfFirst { it.numeroRutina == rutina.numeroRutina }
        if (index >= 0) {
            this.rutinas[index] = rutina
        }
    }

    override suspend fun updateEjerciciosDeRutina(numeroRutina: Int, ejercicioIds: String, fechaModificacion: Long) {
        val index = rutinas.indexOfFirst { it.numeroRutina == numeroRutina }
        if (index >= 0) {
            val ids = if (ejercicioIds.isEmpty()) emptyList() else ejercicioIds.split(",").map { it.toInt() }
            rutinas[index] = rutinas[index].copy(ejercicioIds = ids, fechaModificacion = fechaModificacion)
        }
    }

    override suspend fun getCountRutinas(): Int = rutinas.size

    override suspend fun clearEjerciciosDeRutina(numeroRutina: Int, fechaModificacion: Long) {
        val index = rutinas.indexOfFirst { it.numeroRutina == numeroRutina }
        if (index >= 0) {
            rutinas[index] = rutinas[index].copy(ejercicioIds = emptyList(), fechaModificacion = fechaModificacion)
        }
    }

    override suspend fun updateNombreRutina(numeroRutina: Int, nombre: String, fechaModificacion: Long) {
        val index = rutinas.indexOfFirst { it.numeroRutina == numeroRutina }
        if (index >= 0) {
            rutinas[index] = rutinas[index].copy(nombre = nombre, fechaModificacion = fechaModificacion)
        }
    }

    override suspend fun deleteRutinaByNumero(numeroRutina: Int) {
        rutinas.removeAll { it.numeroRutina == numeroRutina }
    }

    override suspend fun getMaxNumeroRutina(): Int? = rutinas.maxOfOrNull { it.numeroRutina }
}
