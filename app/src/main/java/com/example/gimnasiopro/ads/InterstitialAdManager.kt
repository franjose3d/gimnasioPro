package com.example.gimnasiopro.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAdManager {

    private const val AD_UNIT_ID = "ca-app-pub-2121593613571802/7660530688"
    private const val COOLDOWN_MS = 5 * 60 * 1000L // 5 minutos
    private const val TAG = "InterstitialAdManager"

    private var interstitialAd: InterstitialAd? = null
    private var lastShownTime: Long = 0L
    private var isLoading = false

    fun preload(context: Context) {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    Log.d(TAG, "Interstitial cargado")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.w(TAG, "Error al cargar interstitial: ${error.message}")
                }
            }
        )
    }

    fun showIfReady(activity: Activity, onFinished: () -> Unit) {
        val now = System.currentTimeMillis()
        val ad = interstitialAd
        if (ad != null && (now - lastShownTime) >= COOLDOWN_MS) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    preload(activity)
                    onFinished()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    onFinished()
                }
            }
            lastShownTime = now
            interstitialAd = null
            ad.show(activity)
        } else {
            onFinished()
        }
    }
}
