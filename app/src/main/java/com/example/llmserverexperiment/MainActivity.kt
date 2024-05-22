package com.example.llmserverexperiment

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.example.llmserverexperiment.databinding.ActivityMainBinding
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import io.ktor.server.request.receive
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class LlmGenerateResponse(
    val response: String = "Unknown",
    val created_at: String = "2024-01-01T00:00:00Z",
    val model: String = "gemma-2b",
    val done: Boolean = true,
)

data class LlmGenerateRequest(
    val model: String = "gemma-2b",
    val prompt: String = "Tell me a joke",
)

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val _partialResults = MutableSharedFlow<Pair<String, Boolean>>(
        extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val partialResults: SharedFlow<Pair<String, Boolean>> = _partialResults.asSharedFlow()
    private val options =
        LlmInference.LlmInferenceOptions.builder().setModelPath("/data/local/tmp/llm/model.bin")
            .setMaxTokens(256).setTemperature(1.0F).setRandomSeed(1).setTopK(1)
            .setResultListener { partialResult, done ->
                _partialResults.tryEmit(partialResult to done)
            }.build()
    private lateinit var LlmService: LlmInference

    private fun startServer() {
        println("debug_tianhaoz will start server")
        embeddedServer(Jetty, port = 8090, watchPaths = emptyList()) {
            install(ContentNegotiation) {
                gson {}
            }
            routing {
                post ("/api/generate") {
                    val req = call.receive<LlmGenerateRequest>()
                    val answer = LlmService.generateResponse(req.prompt)
                    call.respond(LlmGenerateResponse(response = answer))
                }
            }
        }.start()
        println("debug_tianhaoz server started")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LlmService = LlmInference.createFromOptions(this, options)

        startServer()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).setAnchorView(R.id.fab).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}