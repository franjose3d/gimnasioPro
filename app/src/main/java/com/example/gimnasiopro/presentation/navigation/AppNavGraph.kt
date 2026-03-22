package com.example.gimnasiopro.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.gimnasiopro.presentation.auth.LoginScreen
import com.example.gimnasiopro.presentation.auth.VerificarEmailScreen
import com.example.gimnasiopro.presentation.buscarejercictos.BuscarEjerciciosScreen
import com.example.gimnasiopro.presentation.buscartrainer.BuscarTrainerScreen
import com.example.gimnasiopro.presentation.cliente.RegisterClienteScreen
import com.example.gimnasiopro.presentation.contacta.ContactaScreen
import com.example.gimnasiopro.presentation.conversacion.ConversacionScreen
import com.example.gimnasiopro.presentation.detallerutina.DetalleRutinaScreen
import com.example.gimnasiopro.presentation.ejercicios.EjerciciosScreen
import com.example.gimnasiopro.presentation.entrenamiento.EntrenamientoScreen
import com.example.gimnasiopro.presentation.gim.GimScreen
import com.example.gimnasiopro.presentation.listaclientes.ListaClientesScreen
import com.example.gimnasiopro.presentation.listaejercictos.ListaEjerciciosScreen
import com.example.gimnasiopro.presentation.main.MainScreen
import com.example.gimnasiopro.presentation.notificaciones.NotificacionesScreen
import com.example.gimnasiopro.presentation.perfil.PerfilScreen
import com.example.gimnasiopro.presentation.personaltrainer.PersonalTrainerScreen
import com.example.gimnasiopro.presentation.progreso.ProgresoScreen
import com.example.gimnasiopro.presentation.rutinas.RutinasScreen
import com.example.gimnasiopro.presentation.trainer.RegisterTrainerScreen

/**
 * Central navigation graph — every screen is a composable destination here.
 *
 * The host (MainActivity) owns the NavController and calls this function once.
 * No other Activity should navigate with Intents; all navigation goes through
 * navController.navigate(Screen.Xxx.createRoute(...)).
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Main.route
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {

        // ── Auth ──────────────────────────────────────────────────────────────

        composable(Screen.PersonalTrainer.route) {
            PersonalTrainerScreen(
                onNavigateBack              = { navController.popBackStack() },
                onNavigateToLogin           = { navController.navigate(Screen.Login.route) },
                onNavigateToRegisterCliente = { navController.navigate(Screen.RegisterCliente.route) },
                onNavigateToRegisterTrainer = { navController.navigate(Screen.RegisterTrainer.route) }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess       = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.popBackStack() }
            )
        }

        composable(Screen.RegisterCliente.route) {
            RegisterClienteScreen(
                onRegistroExitoso = { navController.navigate(Screen.VerificarEmail.route) },
                onNavigateBack    = { navController.popBackStack() }
            )
        }

        composable(Screen.RegisterTrainer.route) {
            RegisterTrainerScreen(
                onRegistroExitoso = { navController.navigate(Screen.VerificarEmail.route) },
                onNavigateBack    = { navController.popBackStack() }
            )
        }

        composable(Screen.VerificarEmail.route) {
            VerificarEmailScreen(
                onVerified = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSignOut  = {
                    navController.navigate(Screen.PersonalTrainer.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Main / Dashboard ──────────────────────────────────────────────────

        composable(Screen.Main.route) {
            MainScreen(
                onGim           = { navController.navigate(Screen.Gym.route) },
                onEjercicios    = { navController.navigate(Screen.Ejercicios.createRoute()) },
                onRutinas       = { navController.navigate(Screen.Rutinas.createRoute()) },
                onProgreso      = { navController.navigate(Screen.Progreso.createRoute()) },
                onContacta      = { navController.navigate(Screen.Contacta.route) },
                onNotificaciones = { navController.navigate(Screen.Notificaciones.route) },
                onPerfil        = { navController.navigate(Screen.Perfil.route) },
                onLogin         = { navController.navigate(Screen.PersonalTrainer.route) }
            )
        }

        composable(Screen.Perfil.route) {
            PerfilScreen(
                onNavigateBack    = { navController.popBackStack() },
                onCuentaEliminada = {
                    navController.navigate(Screen.PersonalTrainer.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Notificaciones.route) {
            NotificacionesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { userId, userName ->
                    navController.navigate(Screen.Conversacion.createRoute(userId, userName))
                }
            )
        }

        // ── Gym ───────────────────────────────────────────────────────────────

        composable(Screen.Gym.route) {
            GimScreen(
                onNavigateBack     = { navController.popBackStack() },
                onNavigateDetalle  = { numero ->
                    navController.navigate(Screen.DetalleRutina.createRoute(numero))
                },
                onNavigateRutinas  = { navController.navigate(Screen.Rutinas.createRoute()) }
            )
        }

        // ── Ejercicios flow ───────────────────────────────────────────────────

        composable(
            route     = Screen.Ejercicios.route,
            arguments = listOf(
                navArgument("extra_numero_rutina") {
                    type         = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val numeroRutina = backStackEntry.arguments?.getInt("extra_numero_rutina") ?: -1
            EjerciciosScreen(
                numeroRutina       = numeroRutina,
                onNavigateBack     = { navController.popBackStack() },
                onBuscar           = {
                    navController.navigate(Screen.BuscarEjercicios.createRoute(numeroRutina))
                },
                onGrupoSeleccionado = { grupo ->
                    navController.navigate(Screen.ListaEjercicios.createRoute(grupo, numeroRutina))
                }
            )
        }

        composable(
            route     = Screen.ListaEjercicios.route,
            arguments = listOf(
                navArgument("extra_grupo_muscular") {
                    type = NavType.StringType
                },
                navArgument("extra_numero_rutina") {
                    type         = NavType.IntType
                    defaultValue = -1
                }
            )
        ) {
            ListaEjerciciosScreen(
                onNavigateBack = { navController.popBackStack() },
                onRequestLogin = { navController.navigate(Screen.PersonalTrainer.route) }
            )
        }

        composable(
            route     = Screen.BuscarEjercicios.route,
            arguments = listOf(
                navArgument("extra_numero_rutina") {
                    type         = NavType.IntType
                    defaultValue = -1
                }
            )
        ) {
            BuscarEjerciciosScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Rutinas flow ──────────────────────────────────────────────────────

        composable(
            route     = Screen.Rutinas.route,
            arguments = listOf(
                navArgument("MODO_TRAINER") {
                    type         = NavType.BoolType
                    defaultValue = false
                },
                navArgument("CLIENTE_ID") {
                    type         = NavType.StringType
                    nullable     = true
                    defaultValue = null
                },
                navArgument("CLIENTE_NOMBRE") {
                    type         = NavType.StringType
                    nullable     = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val modoTrainer  = backStackEntry.arguments?.getBoolean("MODO_TRAINER") ?: false
            val clienteId    = backStackEntry.arguments?.getString("CLIENTE_ID")
            val clienteNombre = backStackEntry.arguments?.getString("CLIENTE_NOMBRE")
            RutinasScreen(
                onNavigateBack   = { navController.popBackStack() },
                onRutinaSelected = { numero ->
                    navController.navigate(
                        Screen.DetalleRutina.createRoute(numero, modoTrainer, clienteId, clienteNombre)
                    )
                }
            )
        }

        composable(
            route     = Screen.DetalleRutina.route,
            arguments = listOf(
                navArgument("extra_numero_rutina") {
                    type = NavType.IntType
                },
                navArgument("MODO_TRAINER") {
                    type         = NavType.BoolType
                    defaultValue = false
                },
                navArgument("CLIENTE_ID") {
                    type         = NavType.StringType
                    nullable     = true
                    defaultValue = null
                },
                navArgument("CLIENTE_NOMBRE") {
                    type         = NavType.StringType
                    nullable     = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val numero = backStackEntry.arguments?.getInt("extra_numero_rutina") ?: 1
            DetalleRutinaScreen(
                onNavigateBack         = { navController.popBackStack() },
                onAgregarEjercicios    = {
                    navController.navigate(Screen.Ejercicios.createRoute(numero))
                },
                onIniciarEntrenamiento = {
                    navController.navigate(Screen.Entrenamiento.createRoute(numero))
                }
            )
        }

        composable(
            route     = Screen.Entrenamiento.route,
            arguments = listOf(
                navArgument("extra_numero_rutina") {
                    type = NavType.IntType
                }
            )
        ) {
            EntrenamientoScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Progreso ──────────────────────────────────────────────────────────

        composable(
            route     = Screen.Progreso.route,
            arguments = listOf(
                navArgument("MODO_TRAINER") {
                    type         = NavType.BoolType
                    defaultValue = false
                },
                navArgument("CLIENTE_ID") {
                    type         = NavType.StringType
                    nullable     = true
                    defaultValue = null
                },
                navArgument("CLIENTE_NOMBRE") {
                    type         = NavType.StringType
                    nullable     = true
                    defaultValue = null
                }
            )
        ) {
            ProgresoScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Social ────────────────────────────────────────────────────────────

        composable(Screen.Contacta.route) {
            ContactaScreen(
                onNavigateBack   = { navController.popBackStack() },
                onBuscarTrainer  = { navController.navigate(Screen.BuscarTrainer.route) },
                onMisClientes    = { navController.navigate(Screen.ListaClientes.route) },
                onChatConTrainer = { trainerId, trainerNombre ->
                    navController.navigate(Screen.Conversacion.createRoute(trainerId, trainerNombre))
                }
            )
        }

        composable(Screen.BuscarTrainer.route) {
            BuscarTrainerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ListaClientes.route) {
            ListaClientesScreen(
                onVerRutinas     = { clienteId, clienteNombre ->
                    navController.navigate(
                        Screen.Rutinas.createRoute(true, clienteId, clienteNombre)
                    )
                },
                onVerProgreso    = { clienteId, clienteNombre ->
                    navController.navigate(
                        Screen.Progreso.createRoute(true, clienteId, clienteNombre)
                    )
                },
                onEnviarMensaje  = { clienteId, clienteNombre ->
                    navController.navigate(
                        Screen.Conversacion.createRoute(clienteId, clienteNombre)
                    )
                },
                onNavigateBack   = { navController.popBackStack() }
            )
        }

        composable(
            route     = Screen.Conversacion.route,
            arguments = listOf(
                navArgument("OTRO_USUARIO_ID") {
                    type = NavType.StringType
                },
                navArgument("OTRO_USUARIO_NOMBRE") {
                    type         = NavType.StringType
                    nullable     = true
                    defaultValue = null
                }
            )
        ) {
            ConversacionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
