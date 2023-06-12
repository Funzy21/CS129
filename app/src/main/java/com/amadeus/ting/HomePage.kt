package com.amadeus.ting

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.*


class HomePage : AppCompatActivity() {

    private lateinit var auth : FirebaseAuth //Firebase authentication instance
    private lateinit var homeadapter: HomepageAdapter
    private lateinit var recyclerView: RecyclerView
//    private var isLogoAnimated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)

        val currentTime = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila")).time

        auth = FirebaseAuth.getInstance()
        val user: FirebaseUser? = auth.currentUser
        val profileImageUrl = user?.photoUrl.toString()

        val imageViewProfile: ImageView = findViewById(R.id.user_image)

        Glide.with(this)
            .load(profileImageUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(imageViewProfile)


        recyclerView = findViewById(R.id.homepage_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        homeadapter = HomepageAdapter(cardDataList)
        recyclerView.adapter = homeadapter


        onClick<ShapeableImageView>(R.id.user_image) {
            val goToUserProfile = Intent(this, UserProfile::class.java)
            startActivity(goToUserProfile)
        }
        changeMessage(currentTime)

        onClick<ShapeableImageView>(R.id.applogo) {
            AnimateLogo()
        }

    }

    private fun AnimateLogo() {
        val music: MediaPlayer = MediaPlayer.create(this, R.raw.ting_sfx)
        val shake: Animation = AnimationUtils.loadAnimation(this, R.anim.shakeanimation)
        val imgBell = findViewById<View>(R.id.applogo) as ImageView
        imgBell.setImageResource(R.drawable.logo)

        music.start()
        Handler().postDelayed({
            imgBell.startAnimation(shake)}, 100)
    }

    fun changeMessage(currentTime: Date) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"))
        calendar.time = currentTime
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val t: TextView = findViewById(R.id.textView_Username)
        val d: TextView = findViewById(R.id.textView_date)
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(currentTime).toUpperCase(Locale.getDefault())
        d.text = formattedDate

        when {
            hour in 4..11 -> t.text = "Good Morning! ⛅"
            hour in 12..17 -> t.text = "Good Afternoon! ☀"
            hour in 18..23 -> t.text = "Good Evening! \uD83C\uDF19"
            else -> t.text = "Hello! \uD83D\uDE0A"
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // disables the back button
    }

    //Inline function that sets an onClickListener for a given view ID
    private inline fun <reified T : View> Activity.onClick(id: Int, crossinline action: (T) -> Unit) {
        findViewById<T>(id)?.apply {
            setOnClickListener {
                action(this)
            }
        }
    }
}