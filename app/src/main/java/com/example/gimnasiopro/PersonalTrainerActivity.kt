package com.example.gimnasiopro

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity que muestra la sección del Personal Trainer virtual.
 * Esta funcionalidad estará disponible en futuras versiones con asistente IA.
 */
class PersonalTrainerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_trainer)

        setupBackButton()
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}

