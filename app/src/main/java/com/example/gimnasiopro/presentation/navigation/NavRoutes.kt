package com.example.gimnasiopro.presentation.navigation

import android.net.Uri

/**
 * All navigation destinations in the app.
 *
 * Routes that carry arguments define:
 *  - A route template string with {placeholder} segments / query params.
 *  - A [createRoute] factory that URL-encodes string values so that names
 *    containing spaces or special characters are safe to pass via the route.
 */
sealed class Screen(val route: String) {

    // ── Auth ──────────────────────────────────────────────────────────────
    object PersonalTrainer : Screen("personal_trainer")
    object Login           : Screen("login")
    object RegisterCliente : Screen("register_cliente")
    object RegisterTrainer : Screen("register_trainer")
    object VerificarEmail  : Screen("verificar_email")

    // ── Main / Dashboard ──────────────────────────────────────────────────
    object Main          : Screen("main")
    object Perfil        : Screen("perfil")
    object Notificaciones: Screen("notificaciones")

    // ── Gym ───────────────────────────────────────────────────────────────
    object Gym : Screen("gym")

    // ── Ejercicios flow ───────────────────────────────────────────────────
    object Ejercicios : Screen("ejercicios?extra_numero_rutina={extra_numero_rutina}") {
        fun createRoute(numeroRutina: Int = -1) =
            "ejercicios?extra_numero_rutina=$numeroRutina"
    }

    object ListaEjercicios : Screen(
        "lista_ejercicios/{extra_grupo_muscular}?extra_numero_rutina={extra_numero_rutina}"
    ) {
        fun createRoute(grupoMuscular: String, numeroRutina: Int = -1) =
            "lista_ejercicios/${Uri.encode(grupoMuscular)}?extra_numero_rutina=$numeroRutina"
    }

    object BuscarEjercicios : Screen("buscar_ejercicios?extra_numero_rutina={extra_numero_rutina}") {
        fun createRoute(numeroRutina: Int = -1) =
            "buscar_ejercicios?extra_numero_rutina=$numeroRutina"
    }

    // ── Rutinas flow ──────────────────────────────────────────────────────
    object Rutinas : Screen(
        "rutinas?MODO_TRAINER={MODO_TRAINER}&CLIENTE_ID={CLIENTE_ID}&CLIENTE_NOMBRE={CLIENTE_NOMBRE}"
    ) {
        fun createRoute(
            modoTrainer: Boolean = false,
            clienteId: String?   = null,
            clienteNombre: String? = null
        ) = buildString {
            append("rutinas?MODO_TRAINER=$modoTrainer")
            if (clienteId    != null) append("&CLIENTE_ID=${Uri.encode(clienteId)}")
            if (clienteNombre != null) append("&CLIENTE_NOMBRE=${Uri.encode(clienteNombre)}")
        }
    }

    object DetalleRutina : Screen(
        "detalle_rutina/{extra_numero_rutina}" +
        "?MODO_TRAINER={MODO_TRAINER}&CLIENTE_ID={CLIENTE_ID}&CLIENTE_NOMBRE={CLIENTE_NOMBRE}"
    ) {
        fun createRoute(
            numero: Int,
            modoTrainer: Boolean = false,
            clienteId: String?    = null,
            clienteNombre: String? = null
        ) = buildString {
            append("detalle_rutina/$numero?MODO_TRAINER=$modoTrainer")
            if (clienteId    != null) append("&CLIENTE_ID=${Uri.encode(clienteId)}")
            if (clienteNombre != null) append("&CLIENTE_NOMBRE=${Uri.encode(clienteNombre)}")
        }
    }

    object Entrenamiento : Screen("entrenamiento/{extra_numero_rutina}") {
        fun createRoute(numero: Int) = "entrenamiento/$numero"
    }

    // ── Progreso ──────────────────────────────────────────────────────────
    object Progreso : Screen(
        "progreso?MODO_TRAINER={MODO_TRAINER}&CLIENTE_ID={CLIENTE_ID}&CLIENTE_NOMBRE={CLIENTE_NOMBRE}"
    ) {
        fun createRoute(
            modoTrainer: Boolean = false,
            clienteId: String?    = null,
            clienteNombre: String? = null
        ) = buildString {
            append("progreso?MODO_TRAINER=$modoTrainer")
            if (clienteId    != null) append("&CLIENTE_ID=${Uri.encode(clienteId)}")
            if (clienteNombre != null) append("&CLIENTE_NOMBRE=${Uri.encode(clienteNombre)}")
        }
    }

    // ── Social ────────────────────────────────────────────────────────────
    object Contacta      : Screen("contacta")
    object BuscarTrainer : Screen("buscar_trainer")
    object ListaClientes : Screen("lista_clientes")

    object Conversacion : Screen(
        "conversacion/{OTRO_USUARIO_ID}?OTRO_USUARIO_NOMBRE={OTRO_USUARIO_NOMBRE}"
    ) {
        fun createRoute(otroUsuarioId: String, otroNombre: String? = null) = buildString {
            append("conversacion/${Uri.encode(otroUsuarioId)}")
            if (otroNombre != null) append("?OTRO_USUARIO_NOMBRE=${Uri.encode(otroNombre)}")
        }
    }
}
