package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthManager"

class AuthManager(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> {
        return try {
            val credentialManager = CredentialManager.create(activity)
            val webClientId = activity.getString(R.string.default_web_client_id)

            // GetGoogleIdOption with filterByAuthorizedAccounts = false ensures new accounts or unlinked accounts on device can be selected
            val getGoogleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val getSignInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(getGoogleIdOption)
                .addCredentialOption(getSignInWithGoogleOption)
                .build()

            val response = credentialManager.getCredential(activity, request)
            val credential = response.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw IllegalStateException("FirebaseUser is null after sign in")
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In was cancelled by the user", e)
            Result.failure(Exception("Sign-in cancelled.", e))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredentialException during Google Sign-In: ${e.message}", e)
            val friendlyMsg = when {
                e.message?.contains("16") == true ||
                e.message?.contains("Cannot find a matching", ignoreCase = true) == true ||
                e.message?.contains("No credentials", ignoreCase = true) == true ->
                    "No Google account selected or found on this device. Please open Android Settings -> Passwords & accounts, ensure a Google account is added, and tap Sign in with Google again."
                e.message?.contains("network", ignoreCase = true) == true || e.message?.contains("7") == true ->
                    "Network error during sign-in. Please verify your internet connection and try again."
                else -> e.localizedMessage ?: "Sign-in error: ${e.message}"
            }
            Result.failure(Exception(friendlyMsg, e))
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun isAdmin(user: FirebaseUser?): Boolean {
        // Any signed-in user or aprico.blr@gmail.com is granted Admin privileges to manage work orders
        return user != null
    }
}
