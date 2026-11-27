package com.x.myview.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.x.myview.ui.MainViewModel
import com.x.myview.ui.components.MvCard
import com.x.myview.ui.components.SectionTitle

/** Settings block where the user pastes his own Google Gemini key for the run coach. */
@Composable
fun AiCoachSettings(vm: MainViewModel) {
    val s by vm.settings.collectAsState()
    val ctx = LocalContext.current
    var text by remember(s.geminiKey) { mutableStateOf(s.geminiKey) }
    var show by remember { mutableStateOf(false) }
    val t = text.trim()

    SectionTitle("AI coach")
    MvCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Gemini key", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "After a run the AI coach reviews your numbers (distance, time, speed) and gives tips. " +
                    "Only those numbers are sent to Google, never your name or location. Without a key you still get the offline review.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Gemini API key") },
                singleLine = true,
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            // Google now issues keys that start with AQ. (older ones start with AIza). Only obvious mistakes are flagged.
            val wrong = t.startsWith("sb_") || (t.isNotEmpty() && t.length < 20)
            if (wrong) {
                Text(
                    if (t.startsWith("sb_")) "That is a Supabase key, not a Google Gemini key. Make one at aistudio.google.com/apikey."
                    else "This key looks too short. Copy the whole key from Google AI Studio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { vm.updateSettings { it.copy(geminiKey = t) } },
                    enabled = t != s.geminiKey,
                ) { Text(if (t.isEmpty() && s.geminiKey.isNotEmpty()) "Remove key" else "Save") }
                TextButton(onClick = { show = !show }) { Text(if (show) "Hide" else "Show") }
                TextButton(onClick = {
                    try {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey")))
                    } catch (_: Exception) {
                    }
                }) { Text("Get free key") }
            }
        }
    }
}
