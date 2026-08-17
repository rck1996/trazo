package com.trazo.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import com.trazo.app.ui.TrazoApp
import com.trazo.app.ui.TrazoTheme
import com.trazo.app.widget.TrazoWidget

class MainActivity : ComponentActivity() {
    private val requestedSection = mutableStateOf<String?>(null)
    private val appViewModel: TrazoViewModel by viewModels()
    private val exportBackup = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                it.write(appViewModel.exportBackup())
            } ?: error("No se pudo abrir el archivo")
        }.onSuccess {
            Toast.makeText(this, "Copia de Trazo guardada", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "No se pudo guardar la copia", Toast.LENGTH_LONG).show()
        }
    }
    private val importBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("No se pudo leer el archivo")
        }.mapCatching { raw -> appViewModel.importBackup(raw).getOrThrow() }
            .onSuccess {
                Toast.makeText(this, "Copia restaurada correctamente", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, "La copia no es válida", Toast.LENGTH_LONG).show()
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedSection.value = intent.getStringExtra(EXTRA_SECTION)
        enableEdgeToEdge()
        setContent {
            TrazoTheme(appViewModel.settings.value) {
                TrazoApp(
                    appViewModel,
                    requestedSection.value,
                    onExportBackup = { exportBackup.launch("Trazo-copia.json") },
                    onImportBackup = { importBackup.launch(arrayOf("application/json", "text/plain")) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appViewModel.refresh()
        TrazoWidget.updateAll(this)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedSection.value = intent.getStringExtra(EXTRA_SECTION)
    }

    companion object { const val EXTRA_SECTION = "open_section" }
}
