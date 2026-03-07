package com.example.gimnasiopro.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Helper para gestionar anuncios de Google AdMob.
 *
 * Características:
 * - Banners en parte inferior de pantallas
 * - Intersticiales con control de frecuencia (máximo 1 cada 5 minutos)
 * - IDs de PRODUCCIÓN activos
 */
object AdHelper {

    private const val TAG = "AdHelper"

    // ====== IDs DE PRODUCCIÓN (ACTIVOS) ======
    private const val BANNER_AD_UNIT_ID = "ca-app-pub-2121593613571802/9823659590"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-2121593613571802/7660530688"

    // ====== IDs DE PRUEBA (COMENTADOS) ======
    // private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    // private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // Control de frecuencia para intersticiales
    private var ultimoInterstitialMostrado: Long = 0
    private const val INTERVALO_MINIMO_MS = 5 * 60 * 1000L  // 5 minutos

    // Intersticial precargado
    private var interstitialAd: InterstitialAd? = null
    private var estaCargandoIntersticial = false

    /**
     * Inicializar AdMob (llamar en Application o MainActivity.onCreate).
     */
    fun inicializar(context: Context) {
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "✅ AdMob inicializado: ${initializationStatus.adapterStatusMap}")
        }
    }

    /**
     * Cargar y mostrar banner en un contenedor.
     *
     * @param adContainer FrameLayout donde se mostrará el banner
     */
    fun cargarBanner(adContainer: FrameLayout) {
        try {
            val adView = AdView(adContainer.context)
            adView.adUnitId = BANNER_AD_UNIT_ID
            adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER)

            // Añadir AdView al contenedor
            adContainer.removeAllViews()
            adContainer.addView(adView)

            // Cargar anuncio
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)

            // Mostrar contenedor cuando el anuncio se cargue
            adView.adListener = object : com.google.android.gms.ads.AdListener() {
                override fun onAdLoaded() {
                    adContainer.visibility = View.VISIBLE
                    Log.d(TAG, "✅ Banner cargado correctamente")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    adContainer.visibility = View.GONE
                    Log.e(TAG, "❌ Error cargando banner: ${error.message}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inicializando banner: ${e.message}")
            adContainer.visibility = View.GONE
        }
    }

    /**
     * Precargar intersticial para mostrarlo después.
     * Llamar cuando se sepa que puede necesitarse pronto.
     */
    fun precargarIntersticial(context: Context) {
        // No precargar si ya hay uno listo o se está cargando
        if (interstitialAd != null || estaCargandoIntersticial) {
            Log.d(TAG, "⏭️ Intersticial ya disponible o cargándose")
            return
        }

        estaCargandoIntersticial = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    estaCargandoIntersticial = false
                    Log.d(TAG, "✅ Intersticial precargado y listo")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    estaCargandoIntersticial = false
                    Log.e(TAG, "❌ Error precargando intersticial: ${error.message}")
                }
            }
        )
    }

    /**
     * Mostrar intersticial si está precargado y ha pasado el tiempo mínimo.
     *
     * @param activity Activity desde donde se muestra
     * @param onDismissed Callback cuando se cierra el anuncio
     */
    fun mostrarIntersticial(
        activity: Activity,
        onDismissed: () -> Unit = {}
    ) {
        // Verificar frecuencia (máximo 1 cada 5 minutos)
        val ahora = System.currentTimeMillis()
        val tiempoTranscurrido = ahora - ultimoInterstitialMostrado

        if (tiempoTranscurrido < INTERVALO_MINIMO_MS) {
            val minutosRestantes = (INTERVALO_MINIMO_MS - tiempoTranscurrido) / 60000
            Log.d(TAG, "⏰ Intersticial bloqueado (espera $minutosRestantes min)")
            onDismissed()
            return
        }

        // Verificar si el intersticial está listo
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "🔕 Intersticial cerrado por el usuario")
                    interstitialAd = null
                    ultimoInterstitialMostrado = ahora

                    // Precargar el siguiente
                    precargarIntersticial(activity)

                    onDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    Log.e(TAG, "❌ Error mostrando intersticial: ${error.message}")
                    interstitialAd = null
                    onDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "🎬 Intersticial mostrado")
                    interstitialAd = null
                }
            }

            interstitialAd?.show(activity)
        } else {
            Log.w(TAG, "⚠️ Intersticial no disponible, precargando para la próxima vez")
            precargarIntersticial(activity)
            onDismissed()
        }
    }
}