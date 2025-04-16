package com.android.authapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.bundleOf
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import com.android.authapp.ui.theme.ComposeSampleTheme


class MainActivity : ComponentActivity() {

    private lateinit var analytics: FirebaseAnalytics
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onStart() {
        super.onStart()
        analytics.logEvent(
            FirebaseAnalytics.Event.SELECT_CONTENT,
            bundleOf(
                FirebaseAnalytics.Param.ITEM_NAME to "InOnStart",
                FirebaseAnalytics.Param.ITEM_CATEGORY to "2",
                FirebaseAnalytics.Param.CONTENT_TYPE to "text",
            )
        )
    }

    override fun onResume() {
        super.onResume()
        analytics.logEvent(
            FirebaseAnalytics.Event.SELECT_CONTENT,
            bundleOf(
                FirebaseAnalytics.Param.ITEM_NAME to "InOnResume",
                FirebaseAnalytics.Param.ITEM_CATEGORY to "3",
                FirebaseAnalytics.Param.CONTENT_TYPE to "text",
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        analytics = Firebase.analytics
        auth = Firebase.auth
        credentialManager = CredentialManager.create(baseContext)

        setContent {
            ComposeSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        auth,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        launchSignInSetup()
                    }
                }
            }
        }

        analytics.logEvent(
            FirebaseAnalytics.Event.SELECT_CONTENT,
            bundleOf(
                FirebaseAnalytics.Param.ITEM_NAME to "InOnCreate",
                FirebaseAnalytics.Param.ITEM_CATEGORY to "1",
                FirebaseAnalytics.Param.CONTENT_TYPE to "text",
            )
        )
    }

    private fun launchSignInSetup() {
        val googleIdOption = GetSignInWithGoogleOption.Builder(baseContext.getString(R.string.default_web_client_id))
//            .setServerClientId(defaultServiceClientId)
            .build()

//        GetGoogleIdOption.Builder()
//            .setFilterByAuthorizedAccounts(true)
//            .setServerClientId(baseContext.getString(R.string.default_web_client_id))
//            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                // Launch Credential Manager UI
                val result = credentialManager.getCredential(
                    context = baseContext,
                    request = request
                )

                // Extract credential from the result returned by Credential Manager
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                Log.e("TAG", "Couldn't retrieve user's credentials: ${e.localizedMessage}")
            }
        }

    }

    private fun handleSignIn(credential: Credential) {
        // Check if credential is of type Google ID
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            // Create Google ID Token
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            // Sign in to Firebase with using the token
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        } else {
            Log.w("TAG", "Credential is not of type Google ID!")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("TAG", "signInWithCredential:success")
                    val user = auth.currentUser
//                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user
                    Log.w("TAG", "signInWithCredential:failure", task.exception)
//                    updateUI(null)
                }
            }
    }


}

@Composable
fun Greeting(
    auth: FirebaseAuth,
    modifier: Modifier = Modifier,
    clicked: () -> Unit
) {
    Column {
        for (i in 1..5) {
            TwoTextView(i.toString(), modifier)
        }
        val context = LocalContext.current
        Button(
            onClick = clicked
//            {
//
////            throw NullPointerException()
////            auth.signInWithEmailAndPassword("adam@wrong.com", "haha123")
////                .addOnCompleteListener { task ->
////                    if (task.isSuccessful) {
////                        Toast.makeText(context, "Congratulations?", Toast.LENGTH_SHORT).show()
////                    } else {
////                        Toast.makeText(context, "Get out?", Toast.LENGTH_SHORT).show()
////                    }
////                }
//        }
        ) {
            Text("Crash!")
        }
    }
}

@Composable
private fun TwoTextView(name: String, modifier: Modifier) {
    Row {
        Text(
            text = "Hola $name!",
            modifier = modifier
        )
        Text(
            text = "Hola $name!",
            modifier = modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
//    Scaffold { padding ->
//        ComposeSampleTheme {
////            Greeting("Android", modifier = Modifier.padding(padding))
//        }
//    }
}