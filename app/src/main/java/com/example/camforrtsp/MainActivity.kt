package com.example.camforrtsp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import android.widget.Button


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnLoadFragment = findViewById<Button>(R.id.btnLoadFragment)
        btnLoadFragment.setOnClickListener {
            loadFragmentCamIP()
            // Oculta el botón al cargar el fragmento
            btnLoadFragment.visibility = View.GONE
        }
    }

    private fun loadFragmentCamIP() {
        val fragment = FragmentCamIP()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}






