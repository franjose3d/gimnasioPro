package com.example.gimnasiopro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gimnasiopro.components.EjercicioAdapter
import com.example.gimnasiopro.data.Ejercicio
import com.example.gimnasiopro.data.firestore.EjercicioRepositoryHibrido
import com.example.gimnasiopro.data.firestore.RutinaRepositoryHibrido
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Activity que muestra el detalle de una rutina con sus ejercicios.
 */
class DetalleRutinaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NUMERO_RUTINA = "extra_numero_rutina"
        private const val TAG = "DetalleRutinaActivity"
    }

    private lateinit var ejercicioRepository: EjercicioRepositoryHibrido
    private lateinit var rutinaRepository: RutinaRepositoryHibrido
    private lateinit var adapter: EjercicioAdapter

    private lateinit var tvTituloRutina: TextView
    private lateinit var layoutRutinaVacia: LinearLayout
    private lateinit var rvEjerciciosRutina: RecyclerView
    private lateinit var layoutAcciones: LinearLayout
    private lateinit var btnAgregarEjercicios: Button
    private lateinit var btnAnadirEjercicio: Button
    private lateinit var btnBorrarEjercicio: Button
    private lateinit var btnIniciarRutina: Button

    private var numeroRutina: Int = 1

    // Modo trainer: acceso directo a Firestore del cliente
    private var modoTrainer = false
    private var clienteId: String? = null
    private var clienteNombre: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_rutina)

        // Obtener el número de rutina del intent
        numeroRutina = intent.getIntExtra(EXTRA_NUMERO_RUTINA, 1)

        // Detectar modo trainer
        modoTrainer = intent.getBooleanExtra(RutinasActivity.EXTRA_MODO_TRAINER, false)
        clienteId = intent.getStringExtra(RutinasActivity.EXTRA_CLIENTE_ID)
        clienteNombre = intent.getStringExtra(RutinasActivity.EXTRA_CLIENTE_NOMBRE)

        if (modoTrainer && clienteId != null) {
            Log.d(TAG, "📋 Modo TRAINER: viendo rutina $numeroRutina del cliente $clienteNombre ($clienteId)")
        }

        // Obtener los repositorios desde la Application
        val app = application as GimnasioproApplication
        ejercicioRepository = app.ejercicioRepository
        rutinaRepository = app.rutinaRepositoryHibrido

        setupBackButton()
        setupViews()
        loadRutinaEjercicios()
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun setupViews() {
        tvTituloRutina = findViewById(R.id.tvTituloRutina)
        layoutRutinaVacia = findViewById(R.id.layoutRutinaVacia)
        rvEjerciciosRutina = findViewById(R.id.rvEjerciciosRutina)
        layoutAcciones = findViewById(R.id.layoutAcciones)
        btnAgregarEjercicios = findViewById(R.id.btnAgregarEjercicios)
        btnAnadirEjercicio = findViewById(R.id.btnAnadirEjercicio)
        btnBorrarEjercicio = findViewById(R.id.btnBorrarEjercicio)
        btnIniciarRutina = findViewById(R.id.btnIniciarRutina)

        val btnLimpiarRutina = findViewById<ImageButton>(R.id.btnLimpiarRutina)

        // Configurar título
        if (modoTrainer && clienteNombre != null) {
            tvTituloRutina.text = "Rutina $numeroRutina de $clienteNombre"
        } else {
            tvTituloRutina.text = getString(R.string.rutina_1).replace("1", numeroRutina.toString())
        }

        // Configurar adaptador con selección habilitada para esta rutina
        adapter = EjercicioAdapter(
            onEjercicioClick = { _ ->
                // La selección se maneja internamente en el adapter
            },
            onSelectionChanged = { selectedEjercicios ->
                // Actualizar estado del botón borrar según la cantidad seleccionada
                actualizarEstadoBotonBorrar(selectedEjercicios.size)
            },
            onEjercicioLongPress = null,
            maxSeleccion = 10
        )
        rvEjerciciosRutina.layoutManager = LinearLayoutManager(this)
        rvEjerciciosRutina.adapter = adapter

        // Configurar botones

        btnLimpiarRutina.setOnClickListener {
            confirmarLimpiarRutina()
        }

        btnAgregarEjercicios.setOnClickListener {
            navegarAEjercicios()
        }

        btnAnadirEjercicio.setOnClickListener {
            navegarAEjercicios()
        }

        btnBorrarEjercicio.setOnClickListener {
            borrarEjerciciosSeleccionados()
        }

        btnIniciarRutina.setOnClickListener {
            iniciarEntrenamiento()
        }

        // En modo trainer, deshabilitar el botón de iniciar entrenamiento
        if (modoTrainer) {
            btnIniciarRutina.visibility = View.GONE
        }

        // Inicializar botón borrar como deshabilitado
        actualizarEstadoBotonBorrar(0)
    }

    private fun loadRutinaEjercicios() {
        if (modoTrainer && clienteId != null) {
            // Modo trainer: cargar desde Firestore del cliente directamente
            loadRutinaEjerciciosDesdeFirestore()
        } else {
            // Modo normal: observar cambios desde Room local
            loadRutinaEjerciciosDesdeRoom()
        }
    }

    /**
     * Cargar rutina desde Room local (modo normal del usuario).
     */
    private fun loadRutinaEjerciciosDesdeRoom() {
        lifecycleScope.launch {
            // Observar cambios en la rutina
            rutinaRepository.getRutinaByIdFlow(numeroRutina).collectLatest { rutina ->
                val ejercicioIds = rutina?.getEjercicioIdsList() ?: emptyList()

                if (ejercicioIds.isEmpty()) {
                    mostrarRutinaVacia()
                } else {
                    cargarEjercicios(ejercicioIds)
                }
            }
        }
    }

    /**
     * Cargar rutina desde Firestore del cliente (modo trainer).
     * NO toca Room local del trainer.
     */
    private fun loadRutinaEjerciciosDesdeFirestore() {
        lifecycleScope.launch {
            try {
                val trainerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                Log.d(TAG, "🔄 Cargando rutina $numeroRutina del cliente $clienteId (trainer: $trainerId)...")

                val rutina = rutinaRepository.getRutinaDeClientePorNumero(clienteId!!, trainerId, numeroRutina)

                if (rutina == null) {
                    Log.w(TAG, "⚠️ Rutina $numeroRutina no encontrada para el cliente $clienteId")
                    mostrarRutinaVacia()
                } else if (rutina.ejercicioIds.isEmpty()) {
                    Log.w(TAG, "⚠️ Rutina $numeroRutina sin ejercicios")
                    mostrarRutinaVacia()
                } else {
                    // Convertir IDs de String a Long para el repositorio de ejercicios
                    Log.d(TAG, "✅ Rutina encontrada: ${rutina.nombre} con ${rutina.ejercicioIds.size} ejercicios")
                    val ejercicioIds = rutina.ejercicioIds.mapNotNull { it.toLongOrNull() }
                    if (ejercicioIds.isEmpty()) {
                        Log.w(TAG, "⚠️ No se pudieron convertir los IDs de ejercicios")
                        mostrarRutinaVacia()
                    } else {
                        Log.d(TAG, "🏋️ Cargando ${ejercicioIds.size} ejercicios...")
                        cargarEjercicios(ejercicioIds)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando rutina del cliente desde Firestore: ${e.message}", e)
                mostrarRutinaVacia()
                Toast.makeText(this@DetalleRutinaActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Recargar datos desde Firestore (para modo trainer, después de operaciones de escritura).
     */
    private fun recargarDatosFirestore() {
        if (modoTrainer && clienteId != null) {
            loadRutinaEjerciciosDesdeFirestore()
        }
    }

    private fun mostrarRutinaVacia() {
        layoutRutinaVacia.visibility = View.VISIBLE
        rvEjerciciosRutina.visibility = View.GONE
        layoutAcciones.visibility = View.GONE
    }

    private fun mostrarRutinaConEjercicios() {
        layoutRutinaVacia.visibility = View.GONE
        rvEjerciciosRutina.visibility = View.VISIBLE
        layoutAcciones.visibility = View.VISIBLE
    }

    private fun cargarEjercicios(ejercicioIds: List<Long>) {
        lifecycleScope.launch {
            val ejercicios = ejercicioRepository.getEjerciciosByIds(ejercicioIds)

            // Mantener el orden original de los IDs
            val ejerciciosOrdenados = ejercicioIds.mapNotNull { id ->
                ejercicios.find { it.id == id }
            }

            if (ejerciciosOrdenados.isNotEmpty()) {
                adapter.submitList(ejerciciosOrdenados)
                mostrarRutinaConEjercicios()
            } else {
                mostrarRutinaVacia()
            }
        }
    }

    private fun confirmarLimpiarRutina() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.btn_limpiar_rutina))
            .setMessage(getString(R.string.confirmar_limpiar_rutina))
            .setPositiveButton(getString(R.string.btn_limpiar_rutina)) { _, _ ->
                limpiarRutina()
            }
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .show()
    }

    private fun limpiarRutina() {
        lifecycleScope.launch {
            if (modoTrainer && clienteId != null) {
                // Modo trainer: limpiar en Firestore del cliente
                val trainerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                rutinaRepository.limpiarEjerciciosDeRutinaCliente(clienteId!!, trainerId, numeroRutina).fold(
                    onSuccess = {
                        Toast.makeText(this@DetalleRutinaActivity, R.string.rutina_limpiada, Toast.LENGTH_SHORT).show()
                        recargarDatosFirestore()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@DetalleRutinaActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // Modo normal: limpiar en Room + Firebase propio
                rutinaRepository.limpiarEjerciciosDeRutina(numeroRutina).fold(
                    onSuccess = {
                        Toast.makeText(this@DetalleRutinaActivity, R.string.rutina_limpiada, Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@DetalleRutinaActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun navegarAEjercicios() {
        val intent = Intent(this, EjerciciosActivity::class.java)
        // Pasar el número de rutina para que al guardar ejercicios se añadan directamente
        intent.putExtra(EjerciciosActivity.EXTRA_NUMERO_RUTINA, numeroRutina)

        // Propagar modo trainer para que toda la cadena de navegación lo respete
        if (modoTrainer && clienteId != null) {
            intent.putExtra(RutinasActivity.EXTRA_MODO_TRAINER, true)
            intent.putExtra(RutinasActivity.EXTRA_CLIENTE_ID, clienteId)
            intent.putExtra(RutinasActivity.EXTRA_CLIENTE_NOMBRE, clienteNombre)
        }

        startActivity(intent)
    }

    private fun actualizarEstadoBotonBorrar(cantidadSeleccionados: Int) {
        btnBorrarEjercicio.isEnabled = cantidadSeleccionados > 0
        btnBorrarEjercicio.alpha = if (cantidadSeleccionados > 0) 1.0f else 0.5f
    }

    private fun borrarEjerciciosSeleccionados() {
        val ejerciciosSeleccionados = adapter.getSelectedEjercicios()

        if (ejerciciosSeleccionados.isEmpty()) {
            Toast.makeText(this, R.string.selecciona_ejercicios_borrar, Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.btn_borrar))
            .setMessage(getString(R.string.confirmar_borrar_ejercicios))
            .setPositiveButton(getString(R.string.btn_borrar)) { _, _ ->
                eliminarEjerciciosSeleccionados(ejerciciosSeleccionados)
            }
            .setNegativeButton(getString(R.string.btn_cancelar), null)
            .show()
    }

    private fun eliminarEjerciciosSeleccionados(ejercicios: Set<Ejercicio>) {
        lifecycleScope.launch {
            val ids = ejercicios.map { it.id }

            if (modoTrainer && clienteId != null) {
                // Modo trainer: eliminar en Firestore del cliente
                val trainerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                rutinaRepository.eliminarEjerciciosDeRutinaCliente(clienteId!!, trainerId, numeroRutina, ids).fold(
                    onSuccess = { eliminados ->
                        adapter.clearSelection()
                        Toast.makeText(
                            this@DetalleRutinaActivity,
                            resources.getQuantityString(R.plurals.ejercicios_eliminados, eliminados, eliminados),
                            Toast.LENGTH_SHORT
                        ).show()
                        recargarDatosFirestore()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@DetalleRutinaActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // Modo normal: eliminar en Room + Firebase propio
                val eliminados = rutinaRepository.eliminarEjerciciosDeRutina(numeroRutina, ids)

                adapter.clearSelection()

                Toast.makeText(
                    this@DetalleRutinaActivity,
                    resources.getQuantityString(R.plurals.ejercicios_eliminados, eliminados, eliminados),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun iniciarEntrenamiento() {
        val intent = Intent(this, EntrenamientoActivity::class.java)
        intent.putExtra(EntrenamientoActivity.EXTRA_NUMERO_RUTINA, numeroRutina)
        startActivity(intent)
    }
}

