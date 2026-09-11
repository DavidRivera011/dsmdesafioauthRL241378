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
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        binding.btnLogin.setOnClickListener {
            login()
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            goToMain()
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
                    Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
                    goToMain()
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
                if (task.isSuccessful) {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
                    goToMain()
                } else {
                    val exception = task.exception
                    if (exception is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                        // Ya existe una cuenta con este correo (registrada con password).
                        // Pedimos su password para vincular Google a esa cuenta existente.
                        pedirPasswordParaVincular(credential)
                    } else {
                        binding.progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this, "Error: ${exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun pedirPasswordParaVincular(googleCredential: com.google.firebase.auth.AuthCredential) {
        binding.progressBar.visibility = android.view.View.GONE

        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Contraseña"

        android.app.AlertDialog.Builder(this)
            .setTitle("Ya tienes una cuenta con este correo")
            .setMessage("Ingresa tu contraseña para vincular tu cuenta de Google")
            .setView(input)
            .setPositiveButton("Vincular") { _, _ ->
                val password = input.text.toString().trim()
                val email = binding.etEmail.text.toString().trim()

                binding.progressBar.visibility = android.view.View.VISIBLE

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { signInTask ->
                        if (signInTask.isSuccessful) {
                            auth.currentUser?.linkWithCredential(googleCredential)
                                ?.addOnCompleteListener(this) { linkTask ->
                                    binding.progressBar.visibility = android.view.View.GONE
                                    if (linkTask.isSuccessful) {
                                        Toast.makeText(this, "Cuentas vinculadas correctamente", Toast.LENGTH_SHORT).show()
                                        goToMain()
                                    } else {
                                        Toast.makeText(this, "Error al vincular: ${linkTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        } else {
                            binding.progressBar.visibility = android.view.View.GONE
                            Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                binding.progressBar.visibility = android.view.View.GONE
            }
            .show()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}