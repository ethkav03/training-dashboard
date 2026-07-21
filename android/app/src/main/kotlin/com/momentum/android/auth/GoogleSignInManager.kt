package com.momentum.android.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.momentum.android.BuildConfig

sealed class GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult()
    data class Failure(val message: String) : GoogleSignInResult()
}

/**
 * Native Google Sign-In via Credential Manager. The resulting ID token is
 * handed to the backend's POST /auth/google/mobile, not used locally --
 * the server is the only thing that verifies it.
 */
class GoogleSignInManager(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): GoogleSignInResult {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            return GoogleSignInResult.Failure(
                "Set GOOGLE_WEB_CLIENT_ID in android/local.properties " +
                    "(same value as backend/.env's GOOGLE_CLIENT_ID)."
            )
        }

        // setServerClientId takes the *web* OAuth client ID (not an
        // Android-type client ID) -- the ID token's audience must match
        // that same web client for the backend's verifyIdToken call to
        // accept it, since it verifies against GOOGLE_CLIENT_ID.
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val response = credentialManager.getCredential(context, request)
            val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
            GoogleSignInResult.Success(credential.idToken)
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Failure(e.message ?: "Google Sign-In was cancelled or failed.")
        }
    }
}
