package com.amadeus.ting

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


class RegLogin : AppCompatActivity() {

    // Authentication object to handle Firebase authentication
    private lateinit var auth : FirebaseAuth

    // Client object for handling Google Sign-in
    private lateinit var googleSignInClient : GoogleSignInClient

    private var isSignIn : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reg_login)
        window.statusBarColor = ContextCompat.getColor(this, R.color.dark_green)

        // Initialize the FirebaseAuth instance
        auth = FirebaseAuth.getInstance()

        // Configure the Google Sign-In Options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        // Create the GoogleSignInClient instance
        googleSignInClient = GoogleSignIn.getClient(this, gso)


        findViewById<Button>(R.id.signin_button).setOnClickListener {
            signInGoogle()
        }

        findViewById<Button>(R.id.login_button).setOnClickListener {
            logInGoogle()
        }

    }

    // Function to initiate Google Sign-in flow
    private fun signInGoogle() {
        googleSignInClient.signOut()
        isSignIn = true
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private fun logInGoogle() {

        isSignIn = false
        googleSignInClient.signOut().addOnCompleteListener(this) {
            // Start a new Google Sign-In flow
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }
    }

    // Handle the result of the Google Sign-in flow
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

            if (!isSignIn) {
                handleResultsLogIn(task)
            } else {
                handleResultsSignIn(task)
            }

        }
    }

    private fun handleResultsSignIn(task: Task<GoogleSignInAccount>) {

        if (task.isSuccessful) {
            val account : GoogleSignInAccount? = task.result
            if (account != null) {
                updateUI(account)
            }
        } else {
            Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT).show()
        }

    }

    private fun handleResultsLogIn(task: Task<GoogleSignInAccount>) {

        if (task.isSuccessful) {
            val account : GoogleSignInAccount? = task.result
            if (account != null) {
                // Check if the user's email is already in Firebase
                val email = account.email
                if (email != null) {
                    auth.fetchSignInMethodsForEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val result = task.result
                                if (result.signInMethods?.isEmpty() == true) {
                                    // Email is not registered
                                    Toast.makeText(this, "Email not registered", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Email is already registered
                                    // Direct the user to the home page
                                    updateUI(account)
                                }
                            } else {
                                // Error occurred while fetching sign-in methods
                                Toast.makeText(this, "Error occurred while fetching sign-in methods", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        } else {
            Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT).show()
        }

    }
    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                val intent : Intent = Intent(this, HomePage::class.java)
                intent.putExtra("email",account.email)
                intent.putExtra("name",account.displayName)
                intent.putExtra("profileImage", account.photoUrl?.toString())
                startActivity(intent)
            } else {
                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

}