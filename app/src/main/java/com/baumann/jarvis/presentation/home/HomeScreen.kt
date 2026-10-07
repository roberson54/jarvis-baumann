package com.baumann.jarvis.presentation.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baumann.jarvis.domain.model.ConversationMessage
import com.baumann.jarvis.domain.model.JarvisState
import com.baumann.jarvis.domain.model.Role
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Background = Color(0xFF05070D)

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    var showLog by remember { mutableStateOf(false) }
    var showKey by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.onMicClick() else vm.onPermissionDenied()
    }

    fun onCoreClick() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        when {
            !ui.hasApiKey -> showKey = true
            granted -> vm.onMicClick()
            else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "J.A.R.V.I.S.",
            color = Color(0xFFE0F7FA),
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 6.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (ui.hasApiKey) "● SISTEMA ONLINE" else "● CHAVE DA API AUSENTE",
            color = if (ui.hasApiKey) Color(0xFF69F0AE) else Color(0xFFFFB300),
            fontSize = 12.sp,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.weight(1f))

        JarvisCore(
            state = ui.state,
            modifier = Modifier
                .size(260.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onCoreClick() }
                )
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stateLabel(ui.state),
            color = stateColor(ui.state),
            fontSize = 14.sp,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(12.dp))

        val message = when (ui.state) {
            JarvisState.LISTENING -> "Sim, Sr. Baumann."
            JarvisState.PROCESSING -> "\"${ui.heard}\""
            else -> ui.response.ifBlank { "Como posso ajudar?" }
        }
        Box(
            modifier = Modifier
                .heightIn(max = 160.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                message,
                color = Color(0xFFB0BEC5),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = {}, enabled = false) { Text("MEMÓRIA", fontSize = 12.sp) }
            TextButton(onClick = {}, enabled = false) { Text("COMANDOS", fontSize = 12.sp) }
            TextButton(onClick = { showLog = true }) { Text("LOG", fontSize = 12.sp) }
            TextButton(onClick = { showKey = true }) { Text("CHAVE API", fontSize = 12.sp) }
        }
    }

    if (showLog) {
        LogDialog(
            history = history,
            onDismiss = { showLog = false },
            onClear = { vm.clearHistory() }
        )
    }
    if (showKey) {
        ApiKeyDialog(
            onDismiss = { showKey = false },
            onSave = {
                vm.saveApiKey(it)
                showKey = false
            }
        )
    }
}

@Composable
private fun LogDialog(
    history: List<ConversationMessage>,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    val format = remember { SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("HISTÓRICO") },
        text = {
            if (history.isEmpty()) {
                Text("Nenhuma conversa ainda.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(history, key = { it.id }) { m ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            val who = if (m.role == Role.USER) "Sr. Baumann" else "JARVIS"
                            Text(
                                "$who · ${format.format(Date(m.timestamp))}",
                                fontSize = 11.sp,
                                color = Color(0xFF29B6F6)
                            )
                            Text(m.content, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
        dismissButton = { TextButton(onClick = onClear) { Text("Apagar histórico") } }
    )
}

@Composable
private fun ApiKeyDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var key by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CHAVE DA API") },
        text = {
            Column {
                Text(
                    "Cole a chave da API da Anthropic (console.anthropic.com). " +
                        "Ela fica guardada criptografada neste aparelho.",
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    label = { Text("sk-ant-...") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(key) }, enabled = key.isNotBlank()) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
