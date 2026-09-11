package com.example.dsmdesafiorl241378

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.example.dsmdesafiorl241378.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        callbackManager = CallbackManager.Factory.create()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            val padding = (20 * resources.displayMetrics.density).toInt()

            v.setPadding(
                padding + systemBars.left,
                padding + systemBars.top,
                padding + systemBars.right,
                padding + systemBars.bottom
            )

            insets
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }

        binding.btnLinkGoogle.setOnClickListener {
            linkGoogle()
        }
        binding.btnLinkMicrosoft.setOnClickListener {
            linkMicrosoft()
        }
        binding.btnLinkFacebook.setOnClickListener {
            linkFacebook()
        }
        binding.btnLinkGithub.setOnClickListener {
            linkGithub()
        }
        binding.btnLinkX.setOnClickListener {
            linkX()
        }
        binding.btnGestionarMesas.setOnClickListener {
            startActivity(Intent(this, MesaActivity::class.java))
        }
        binding.btnVerMesas.setOnClickListener {
            startActivity(Intent(this, ListaMesasActivity::class.java))
        }

        actualizarBotonesVinculados()
        mostrarNombreUsuario()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun logout() {
        auth.signOut()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun mostrarNombreUsuario() {
        val user = auth.currentUser ?: return

        val nombre = user.displayName
            ?: user.providerData.firstOrNull {
                it.providerId != "firebase" && !it.displayName.isNullOrBlank()
            }?.displayName
            ?: user.email
            ?: "Usuario"

        binding.tvUsername.text = "Hola, $nombre"
    }

    private fun actualizarBotonesVinculados() {
        val providers = auth.currentUser?.providerData
            ?.map { it.providerId }
            ?: emptyList()

        binding.btnLinkGoogle.isEnabled = !providers.contains("google.com")
        binding.btnLinkMicrosoft.isEnabled = !providers.contains("microsoft.com")
        binding.btnLinkFacebook.isEnabled = !providers.contains("facebook.com")
        binding.btnLinkGithub.isEnabled = !providers.contains("github.com")
        binding.btnLinkX.isEnabled = !providers.contains("twitter.com")
    }

    // VINCULAR GOOGLE
    private fun linkGoogle() {
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
                    context = this@MainActivity,
                    request = request
                )
                val cred = result.credential

                if (cred is CustomCredential &&
                    cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val tokenCredential = GoogleIdTokenCredential.createFrom(cred.data)
                    val authCredential = GoogleAuthProvider.getCredential(tokenCredential.idToken, null)
                    vincularProveedor(authCredential, "Google")
                } else {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this@MainActivity, "Tipo de credencial no soportado", Toast.LENGTH_SHORT).show()
                }
            } catch (e: GetCredentialException) {
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this@MainActivity, "Error con Google: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // VINCULAR MICROSOFT
    private fun linkMicrosoft() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        val provider = OAuthProvider.newBuilder("microsoft.com")
        provider.addCustomParameter("prompt", "select_account")

        auth.currentUser?.startActivityForLinkWithProvider(this, provider.build())
            ?.addOnSuccessListener {
                binding.progressBar.visibility = android.view.View.GONE
                actualizarBotonesVinculados()
                Toast.makeText(this, "Microsoft vinculado correctamente", Toast.LENGTH_SHORT).show()
            }
            ?.addOnFailureListener { e ->
                binding.progressBar.visibility = android.view.View.GONE
                manejarErrorDeVinculo(e)
            }
    }

    // VINCULAR GITHUB
    private fun linkGithub() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        val provider = OAuthProvider.newBuilder("github.com")
        provider.scopes = listOf("user:email")

        auth.currentUser?.startActivityForLinkWithProvider(this, provider.build())
            ?.addOnSuccessListener {
                binding.progressBar.visibility = android.view.View.GONE
                actualizarBotonesVinculados()
                Toast.makeText(this, "GitHub vinculado correctamente", Toast.LENGTH_SHORT).show()
            }
            ?.addOnFailureListener { e ->
                binding.progressBar.visibility = android.view.View.GONE
                manejarErrorDeVinculo(e)
            }
    }

    // VINCULAR FACEBOOK
    private fun linkFacebook() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        LoginManager.getInstance().logInWithReadPermissions(
            this,
            listOf("email", "public_profile")
        )

        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                    vincularProveedor(credential, "Facebook")
                }

                override fun onCancel() {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this@MainActivity, "Vinculación cancelada", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: FacebookException) {
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this@MainActivity, "Error con Facebook: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    // VINCULAR X (TWITTER)
    private fun linkX() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        val provider = OAuthProvider.newBuilder("twitter.com")

        auth.currentUser?.startActivityForLinkWithProvider(
            this,
            provider.build()
        )
            ?.addOnSuccessListener {
                binding.progressBar.visibility = android.view.View.GONE
                actualizarBotonesVinculados()

                Toast.makeText(
                    this,
                    "X vinculado correctamente",
                    Toast.LENGTH_SHORT
                ).show()
            }
            ?.addOnFailureListener { e ->
                binding.progressBar.visibility = android.view.View.GONE
                manejarErrorDeVinculo(e)
            }
    }

    private fun vincularProveedor(credential: AuthCredential, nombreProveedor: String) {
        auth.currentUser?.linkWithCredential(credential)
            ?.addOnCompleteListener(this) { task ->
                binding.progressBar.visibility = android.view.View.GONE
                if (task.isSuccessful) {
                    actualizarBotonesVinculados()
                    Toast.makeText(this, "$nombreProveedor vinculado correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    manejarErrorDeVinculo(task.exception)
                }
            }
    }

    private fun manejarErrorDeVinculo(exception: Exception?) {
        when (exception) {
            is FirebaseAuthUserCollisionException ->
                Toast.makeText(
                    this,
                    "Ese proveedor ya está siendo usado por otra cuenta distinta.",
                    Toast.LENGTH_LONG
                ).show()
            else ->
                Toast.makeText(this, "No se pudo vincular: ${exception?.message}", Toast.LENGTH_LONG).show()
        }
    }
}