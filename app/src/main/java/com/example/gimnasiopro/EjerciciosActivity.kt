package com.example.gimnasiopro

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest

class EjerciciosActivity : AppCompatActivity() {

    private lateinit var imageLoader: ImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ejercicios)

        // Inicializar ImageLoader con soporte SVG
        imageLoader = ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()

        setupBackButton()
        loadSvgImages()
        setupButtons()
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun loadSvgImages() {
        loadSvg(R.id.imgPectorales, R.raw.pectorales)
        loadSvg(R.id.imgEspalda, R.raw.espalda)
        loadSvg(R.id.imgHombros, R.raw.hombros)
        loadSvg(R.id.imgBiceps, R.raw.biceps)
        loadSvg(R.id.imgTriceps, R.raw.triceps)
        loadSvg(R.id.imgAbdominales, R.raw.abdominales)
        loadSvg(R.id.imgPiernas, R.raw.piernas)
        loadSvg(R.id.imgGluteos, R.raw.gluteos)
        loadSvg(R.id.imgGemelos, R.raw.gemelos)
    }

    private fun loadSvg(imageViewId: Int, rawResId: Int) {
        val imageView = findViewById<ImageView>(imageViewId)
        val request = ImageRequest.Builder(this)
            .data("android.resource://${packageName}/${rawResId}")
            .target(imageView)
            .build()
        imageLoader.enqueue(request)
    }

    private fun setupButtons() {
        findViewById<CardView>(R.id.btnPectorales).setOnClickListener {
            navigateToEjercicios("Pectorales")
        }

        findViewById<CardView>(R.id.btnEspalda).setOnClickListener {
            navigateToEjercicios("Espalda")
        }

        findViewById<CardView>(R.id.btnHombros).setOnClickListener {
            navigateToEjercicios("Hombros")
        }

        findViewById<CardView>(R.id.btnBiceps).setOnClickListener {
            navigateToEjercicios("Bíceps y Antebrazo")
        }

        findViewById<CardView>(R.id.btnTriceps).setOnClickListener {
            navigateToEjercicios("Tríceps")
        }

        findViewById<CardView>(R.id.btnAbdominales).setOnClickListener {
            navigateToEjercicios("Abdominales")
        }

        findViewById<CardView>(R.id.btnPiernas).setOnClickListener {
            navigateToEjercicios("Piernas")
        }

        findViewById<CardView>(R.id.btnGluteos).setOnClickListener {
            navigateToEjercicios("Glúteos")
        }

        findViewById<CardView>(R.id.btnGemelos).setOnClickListener {
            navigateToEjercicios("Gemelos")
        }
    }

    private fun navigateToEjercicios(grupoMuscular: String) {
        val intent = Intent(this, ListaEjerciciosActivity::class.java).apply {
            putExtra(ListaEjerciciosActivity.EXTRA_GRUPO_MUSCULAR, grupoMuscular)
        }
        startActivity(intent)
    }
}
