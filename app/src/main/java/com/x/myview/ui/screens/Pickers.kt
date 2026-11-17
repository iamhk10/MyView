package com.x.myview.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.x.myview.ui.components.iconFor
import com.x.myview.ui.theme.ITEM_COLORS
import com.x.myview.util.DAY_NAMES

@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) scheme.primary else scheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) scheme.onPrimary else scheme.onSurface,
        )
    }
}

@Composable
fun IconPicker(keys: List<String>, selected: String, color: Color, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    val on = key == selected
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (on) color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .then(if (on) Modifier.border(2.dp, color, CircleShape) else Modifier)
                            .clickable { onSelect(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            iconFor(key),
                            contentDescription = key,
                            tint = if (on) color else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ITEM_COLORS.chunked(4).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { i, c ->
                    val index = rowIndex * 4 + i
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(
                                if (index == selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier,
                            )
                            .clickable { onSelect(index) },
                    )
                }
            }
        }
    }
}

/** Seven round buttons Mon..Sun that toggle bits of a weekday mask. */
@Composable
fun DayPicker(mask: Int, color: Color, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DAY_NAMES.forEachIndexed { i, name ->
            val on = mask and (1 shl i) != 0
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (on) color else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onChange(mask xor (1 shl i)) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.take(1),
                    fontWeight = FontWeight.Bold,
                    color = if (on) com.x.myview.ui.theme.onColorFor(color) else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Text field that only accepts digits. */
@Composable
fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(6)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

/** "- value +" control. */
@Composable
fun Stepper(text: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StepButton(Icons.Rounded.Remove, "Less", onMinus)
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        StepButton(Icons.Rounded.Add, "More", onPlus)
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

/** Pick a time of day with hour and minute (5 minute steps) steppers. [minutes] is minutes after midnight. */
@Composable
fun TimeStepper(minutes: Int, onChange: (Int) -> Unit) {
    val h = minutes / 60
    val m = minutes % 60
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Stepper(
            com.x.myview.util.formatHour(h),
            { onChange(((h + 23) % 24) * 60 + m) },
            { onChange(((h + 1) % 24) * 60 + m) },
        )
        Stepper(
            String.format(java.util.Locale.US, ":%02d", m),
            { onChange(h * 60 + (m + 55) % 60) },
            { onChange(h * 60 + (m + 5) % 60) },
        )
    }
}
