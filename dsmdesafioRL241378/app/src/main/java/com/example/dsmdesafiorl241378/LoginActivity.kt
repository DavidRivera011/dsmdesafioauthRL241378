package com.example.dsmdesafiorl241378

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.example.dsmdesafiorl241378.databinding.ActivityLoginBinding
import com.facebook.CallbackManager
import kotlinx.coroutines.launch
import com.google.firebase.auth.OAuthProvider
import com.facebook.login.LoginManager
import com.facebook.FacebookCallback
import com.facebook.login.LoginResult
import com.facebook.FacebookException
import com.google.firebase.auth.FacebookAuthProvider


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        callbackManager = CallbackManager.Factory.create()

        binding.btnLogin.setOnClickListener {
            login()
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
//        binding.btnMicrosoftSignIn.setOnClickListener {
//            signInWithMicrosoft()
//        }
        binding.btnFacebookSignIn.setOnClickListener {
            signInWithFacebook()
        }
        binding.btnGithubSignIn.setOnClickListener {
            signInWithGithub()
        }
//        binding.btnTwitterSignIn.setOnClickListener {
//            signInWithTwitter()
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        super.onStart()

        val user = auth.currentUser

        if (user != null) {

            val usaCorreoYPassword = user.providerData.any {
                it.providerId == "password"
            }

            if (usaCorreoYPassword) {

                if (user.isEmailVerified) {
                    goToMain()
                } else {
                    auth.signOut()
                }

            } else {
                goToMain()
            }
        }
    }

    private fun login() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = android.view.View.VISIBLE

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = android.view.View.GONE
                if (task.isSuccessful) {

                    val user = auth.currentUser

                    if (user != null && user.isEmailVerified) {

                        Toast.makeText(
                            this,
                            "Bienvenido",
                            Toast.LENGTH_SHORT
                        ).show()

                        goToMain()

                    } else {

                        Toast.makeText(
                            this,
                            "Debes verificar tu correo electrónico antes de iniciar sesión.",
                            Toast.LENGTH_LONG
                        ).show()

                        auth.signOut()
                    }

                } else {
                    val exception = task.exception
                    val mensaje = when (exception) {
                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException,
                        is com.google.firebase.auth.FirebaseAuthInvalidUserException ->
                            "Correo o contraseña incorrectos"
                        else -> "Error: ${exception?.message}"
                    }
                    Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                }
            }
    }

    // INICIO DE SESION CON GOOGLE
    private fun signInWithGoogle() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@LoginActivity,
                    request = request
                )
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(
                    this@LoginActivity,
                    "Error con Google Sign-In: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // INICIO DE SESION CON MICROSOFT
    private fun signInWithMicrosoft() {

        binding.progressBar.visibility = android.view.View.VISIBLE

        val provider = OAuthProvider.newBuilder("microsoft.com")

        provider.addCustomParameter("prompt", "select_account")

        val pendingResultTask = auth.pendingAuthResult

        if (pendingResultTask != null) {

            pendingResultTask
                .addOnSuccessListener {
                    binding.progressBar.visibility = android.view.View.GONE

                    Toast.makeText(
                        this,
                        "Bienvenido",
                        Toast.LENGTH_SHORT
                    ).show()

                    goToMain()
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = android.view.View.GONE
                    manejarErrorOAuth(e)
                }

        } else {

            auth.startActivityForSignInWithProvider(
                this,
                provider.build()
            )
                .addOnSuccessListener {
                    binding.progressBar.visibility = android.view.View.GONE

                    Toast.makeText(
                        this,
                        "Bienvenido",
                        Toast.LENGTH_SHORT
                    ).show()

                    goToMain()
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = android.view.View.GONE
                    manejarErrorOAuth(e)
                }
        }
    }

    // INICIO DE SESION CON GITHUB
    private fun signInWithGithub() {

        binding.progressBar.visibility = android.view.View.VISIBLE

        val provider = OAuthProvider.newBuilder("github.com")

        provider.scopes = listOf("user:email")

        val pendingResultTask = auth.pendingAuthResult

        if (pendingResultTask != null) {

            pendingResultTask
                .addOnSuccessListener {
                    binding.progressBar.visibility = android.view.View.GONE

                    Toast.makeText(
                        this,
                        "Bienvenido",
                        Toast.LENGTH_SHORT
                    ).show()

                    goToMain()
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = android.view.View.GONE
                    manejarErrorOAuth(e)
                }

        } else {

            auth.startActivityForSignInWithProvider(
                this,
                provider.build()
            )
                .addOnSuccessListener {
                    binding.progressBar.visibility = android.view.View.GONE

                    Toast.makeText(
                        this,
                        "Bienvenido",
                        Toast.LENGTH_SHORT
                    ).show()

                    goToMain()
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = android.view.View.GONE
                    manejarErrorOAuth(e)
                }
        }
    }

    // INICIO DE SESION CON FACEBOOK
    private fun signInWithFacebook() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        LoginManager.getInstance().logInWithReadPermissions(
            this,
            listOf("email", "public_profile")
        )

        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    firebaseAuthWithFacebook(result.accessToken.token)
                }

                override fun onCancel() {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this@LoginActivity, "Inicio de sesión cancelado", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: FacebookException) {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this@LoginActivity, "Error con Facebook: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    // INICIO DE SESION CON TWITTER/X
    private fun signInWithTwitter() {

        binding.progressBar.visibility = android.view.View.VISIBLE

        val provider = OAuthProvider.newBuilder("twitter.com")

        val pendingResultTask = auth.pendingAuthResult

        if (pendingResultTask != null) {

            pendingResultTask
                .addOnSuccessListener {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
                    goToMain()
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = android.view.View.GONE
                    manejarErrorOAuth(e)
                }

        } else {

            auth.startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
                    goToMain()
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = android.view.View.GONE
                    manejarErrorOAuth(e)
                }
        }
    }

    private fun firebaseAuthWithFacebook(token: String) {
        val credential = FacebookAuthProvider.getCredential(token)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = android.view.View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
                    goToMain()
                } else {
                    manejarErrorOAuth(task.exception)
                }
            }
    }

    private fun handleSignIn(credential: androidx.credentials.Credential) {
        if (credential is androidx.credentials.CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        } else {
            binding.progressBar.visibility = android.view.View.GONE
            Toast.makeText(this, "Tipo de credencial no soportado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = android.view.View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
                    goToMain()
                } else {
                    manejarErrorOAuth(task.exception)
                }
            }
    }
    private fun manejarErrorOAuth(exception: Exception?) {
        android.util.Log.e("LOGIN_DEBUG", "Error completo: ${exception?.message}", exception)
        if (exception is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            val emailConflicto = exception.email
            if (emailConflicto != null) {
                Toast.makeText(
                    this,
                    "Ya existe una cuenta con $emailConflicto. Verifica ese correo o inicia sesión con el método original para poder vincular.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this, "Ya existe una cuenta con este correo.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Error: ${exception?.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}