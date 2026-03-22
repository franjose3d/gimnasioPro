package com.example.gimnasiopro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gimnasiopro.data.firestore.Notificacion
import com.example.gimnasiopro.presentation.conversacion.ConversacionScreen
import com.example.gimnasiopro.presentation.navigation.Screen
import com.example.gimnasiopro.presentation.notificaciones.NotificacionesScreen
import com.example.gimnasiopro.ui.theme.GimnasioProTheme

class NotificacionesActivity : ComponentActivity() {

    // Shared state between onNewIntent and the Compose NavController
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GimnasioProTheme {
                val navController = rememberNavController()
                val startDestination = remember { resolverDestino(intent) }

                // React to intents received while the Activity is already alive (singleTop)
                val nuevaRuta = pendingRoute.value
                LaunchedEffect(nuevaRuta) {
                    if (nuevaRuta != null) {
                        navController.navigate(nuevaRuta) {
                            popUpTo(Screen.Notificaciones.route) { inclusive = true }
                        }
                        pendingRoute.value = null
                    }
                }

                NavHost(navController = navController, startDestination = startDestination) {
                    composable(Screen.Notificaciones.route) {
                        NotificacionesScreen(
                            onNavigateBack = { finish() },
                            onNavigateToChat = { userId, userName ->
                                navController.navigate(Screen.Conversacion.createRoute(userId, userName))
                            }
                        )
                    }
                    composable(
                        route = Screen.Conversacion.route,
                        arguments = listOf(
                            navArgument("OTRO_USUARIO_ID") { type = NavType.StringType },
                            navArgument("OTRO_USUARIO_NOMBRE") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) {
                        ConversacionScreen(onNavigateBack = { finish() })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute.value = resolverDestino(intent)
    }

    private fun resolverDestino(intent: Intent): String {
        val tipo        = intent.getStringExtra("notificacion_tipo") ?: ""
        val remitenteId = intent.getStringExtra("OTRO_USUARIO_ID") ?: ""
        val nombre      = intent.getStringExtra("OTRO_USUARIO_NOMBRE")

        return if (tipo == Notificacion.TIPO_MENSAJE && remitenteId.isNotBlank()) {
            Screen.Conversacion.createRoute(remitenteId, nombre)
        } else {
            Screen.Notificaciones.route
        }
    }
}
