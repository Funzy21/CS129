package com.amadeus.ting

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

// SPLASHCREEN
class Splashscreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,1
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen)
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)
        val music: MediaPlayer = MediaPlayer.create(this, R.raw.ting_sfx)

        Handler().postDelayed({
            music.start()
        }, 950)
        Handler().postDelayed({
            startActivity(Intent(this, HomePage::class.java))
            finish()
        }, 2500)

    }
}