package com.example.gimnasiopro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupButtons()
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnGim).setOnClickListener {
            navigateToGim()
        }

        findViewById<Button>(R.id.btnEjercicios).setOnClickListener {
            navigateToEjercicios()
        }

        findViewById<Button>(R.id.btnRutinas).setOnClickListener {
            navigateToRutinas()
        }

        findViewById<Button>(R.id.btnProgreso).setOnClickListener {
            navigateToProgreso()
        }

        findViewById<Button>(R.id.btnPersonalTrainer).setOnClickListener {
            navigateToPersonalTrainer()
        }
    }

    private fun navigateToGim() {
        val intent = Intent(this, GimActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToEjercicios() {
        val intent = Intent(this, EjerciciosActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToRutinas() {
        val intent = Intent(this, RutinasActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToProgreso() {
        val intent = Intent(this, ProgresoActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToPersonalTrainer() {
        val intent = Intent(this, PersonalTrainerActivity::class.java)
        startActivity(intent)
    }
}