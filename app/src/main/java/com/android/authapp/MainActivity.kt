package com.android.authapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.authapp.ui.theme.ComposeSampleTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    private lateinit var analytics: FirebaseAnalytics
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        analytics = Firebase.analytics
        auth = Firebase.auth

        // Google Sign-In config
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            ComposeSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AuthScreen(
                        auth = auth,
                        modifier = Modifier.padding(innerPadding),
                        onGoogleSignIn = { signInWithGoogle() },
                        onYahooSignIn = { signInWithYahoo() }
                    )
                }
            }
        }
    }

    // Google Sign-In
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    // Yahoo Sign-In
    private fun signInWithYahoo() {
        val pendingResultTask = auth.pendingAuthResult
        if (pendingResultTask != null) {
            pendingResultTask
                .addOnSuccessListener { result ->
                    val user = result.user
                    Log.d("YahooSignIn", "Pending sign-in success: ${user?.displayName}")
                    Toast.makeText(this, "Welcome back, ${user?.displayName}", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.w("YahooSignIn", "Pending Yahoo sign-in failed", e)
                    Toast.makeText(this, "Yahoo sign-in failed", Toast.LENGTH_SHORT).show()
                }
            return
        }

        val provider = OAuthProvider.newBuilder("yahoo.com")
        provider.addCustomParameter("prompt", "login") // Ensure prompt for login
        provider.scopes = listOf("email", "profile", "openid") // Adding the necessary scopes for Yahoo

        auth.startActivityForSignInWithProvider(this, provider.build())
            .addOnSuccessListener { result ->
                val user = result.user
                Log.d("YahooSignIn", "Signed in with Yahoo: ${user?.displayName}")
                Toast.makeText(this, "Welcome, ${user?.displayName}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("YahooSignIn", "Yahoo sign-in failed", e)
                Toast.makeText(this, "Yahoo sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("GoogleSignIn", "Google sign-in failed", e)
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("GoogleSignIn", "Google sign-in success, UID: ${auth.currentUser?.uid}")
                } else {
                    Log.w("GoogleSignIn", "Google sign-in failure", task.exception)
                    Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    @Composable
    fun AuthScreen(
        auth: FirebaseAuth,
        modifier: Modifier = Modifier,
        onGoogleSignIn: () -> Unit,
        onYahooSignIn: () -> Unit
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🚀 Welcome to Android", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onGoogleSignIn, modifier = Modifier.fillMaxWidth()) {
                Text("Sign in with Google")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onYahooSignIn, modifier = Modifier.fillMaxWidth()) {
                Text("Sign in with Yahoo")
            }
            Spacer(Modifier.height(16.dp))
            Text("Your credentials are secure 🔒", style = MaterialTheme.typography.bodySmall)
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun AuthScreenPreview() {
        ComposeSampleTheme {
            AuthScreen(auth = Firebase.auth, onGoogleSignIn = {}, onYahooSignIn = {})
        }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}
