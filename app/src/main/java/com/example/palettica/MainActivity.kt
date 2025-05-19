package com.example.palettica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.palettica.ui.theme.PaletticaTheme
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore by preferencesDataStore(name = "color_storage")

object ColorStorage {
    private val SAVED_COLORS_KEY = stringSetPreferencesKey("saved_colors")

    fun getSavedColors(context: Context): Flow<Set<String>> {
        return context.dataStore.data.map { preferences ->
            preferences[SAVED_COLORS_KEY] ?: emptySet()
        }
    }

    suspend fun saveColor(context: Context, hex: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[SAVED_COLORS_KEY]?.toMutableSet() ?: mutableSetOf()
            current.add(hex)
            prefs[SAVED_COLORS_KEY] = current
        }
    }

    suspend fun deleteColor(context: Context, hex: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[SAVED_COLORS_KEY]?.toMutableSet() ?: mutableSetOf()
            current.remove(hex)
            prefs[SAVED_COLORS_KEY] = current
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaletticaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ColorMixDemo()
                }
            }
        }
    }
}

@Composable
fun ColorMixDemo() {
    var showMixCard by remember { mutableStateOf(true) }
    var showDialog1 by remember { mutableStateOf(false) }
    var showDialog2 by remember { mutableStateOf(false) }
    var color1 by remember { mutableStateOf(Color.Red) }
    var color2 by remember { mutableStateOf(Color.Blue) }
    var blendRatio by remember { mutableFloatStateOf(0.5f) }
    val mixedColor by remember(color1, color2, blendRatio) {
        derivedStateOf {
            Color(
                red = color1.red * (1 - blendRatio) + color2.red * blendRatio,
                green = color1.green * (1 - blendRatio) + color2.green * blendRatio,
                blue = color1.blue * (1 - blendRatio) + color2.blue * blendRatio
            )
        }
    }
    val savedColors = remember { mutableStateListOf<Color>() }
    val clipboardManager = LocalClipboardManager.current
    val controller = rememberColorPickerController()
    val myCardColor = Color.White
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        ColorStorage.getSavedColors(context).collect { hexSet ->
            savedColors.clear()
            savedColors.addAll(hexSet.mapNotNull { runCatching { Color(it.toColorInt()) }.getOrNull() })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showMixCard) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = myCardColor),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 56.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Pick colors you want to mix together", modifier = Modifier.padding(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ColorDisplayBox(color1) { showDialog1 = true }
                            ColorDisplayBox(color2) { showDialog2 = true }
                        }

                        Text("Blending Ratio: ${((1 - blendRatio) * 100).toInt()}% / ${(blendRatio * 100).toInt()}%")

                        Slider(
                            value = blendRatio,
                            onValueChange = { blendRatio = it },
                            valueRange = 0f..1f,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Text("Mixed Color")
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(mixedColor)
                                .border(1.dp, Color.Black, RoundedCornerShape(16.dp))

                        )

                        Button(onClick = {
                            val hex = "#${mixedColor.toArgb().toUInt().toString(16).uppercase().takeLast(6)}"
                            if (!savedColors.contains(mixedColor)) {
                                savedColors.add(mixedColor)
                                CoroutineScope(Dispatchers.IO).launch {
                                    ColorStorage.saveColor(context, hex)
                                }
                            }
                        }) {
                            Text("Save Color")
                        }

                        if (savedColors.isNotEmpty()) {
                            Text("Saved Colors:")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                itemsIndexed(savedColors) { index, color ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(color)
                                                .border(1.dp, Color.Black, RoundedCornerShape(16.dp))
                                        )
                                        Text(
                                            text = "#${color.toArgb().toUInt().toString(16).uppercase().takeLast(6)}",
                                            modifier = Modifier
                                                .clickable {
                                                    clipboardManager.setText(
                                                        AnnotatedString("#${color.toArgb().toUInt().toString(16).uppercase().takeLast(6)}")
                                                    )
                                                }
                                                .padding(top = 4.dp)
                                        )
                                        IconButton(onClick = {
                                            val hex = "#${color.toArgb().toUInt().toString(16).uppercase().takeLast(6)}"
                                            savedColors.removeAt(index)
                                            CoroutineScope(Dispatchers.IO).launch {
                                                ColorStorage.deleteColor(context, hex)
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                                        }
                                    }

                                }
                            }
                        }
                    }

                    // Toggle at bottom of card
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Unmix Mode")
                        Switch(
                            checked = !showMixCard,
                            onCheckedChange = { isChecked ->
                                showMixCard = !isChecked
                            }
                        )
                    }
                }
            }
        } else {
            var hexInput by remember { mutableStateOf("") }
            var unmixColor by remember { mutableStateOf<Color?>(null) }
            var inputError by remember { mutableStateOf(false) }
            var unmixBlendRatio by remember { mutableFloatStateOf(0.5f) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text("Enter HEX color to unmix",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .padding(8.dp)
                    )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = hexInput,
                    onValueChange = {
                        hexInput = it
                        inputError = false
                    },
                    label = { Text("Hex code (e.g. #FF5733)") },
                    isError = inputError,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    enabled=hexInput!="",
                    onClick = {
                        try {
                        val parsed = Color(hexInput.toColorInt())
                        unmixColor = parsed
                        } catch (e: IllegalArgumentException) {
                            inputError = true
                        }
                }) {
                    Text("Confirm")
                }

                if (inputError) {
                    Text("Invalid hex code!", color = Color.Red, modifier = Modifier.padding(top = 4.dp))
                }

                unmixColor?.let { color ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Selected Color")
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(color)
                            .border(2.dp, Color.Black, RoundedCornerShape(16.dp))

                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Guess Blend Ratio: ${(unmixBlendRatio * 100).toInt()}%")

                Slider(
                    value = unmixBlendRatio,
                    onValueChange = { unmixBlendRatio = it },
                    valueRange = 0.1f..0.9f, // avoid 0 or 1 to prevent divide by zero
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                val (guessA, guessB) = remember(unmixColor, unmixBlendRatio) {
                    if (unmixColor != null)
                        findClosestColorPair(unmixColor!!, unmixBlendRatio)
                    else Pair(Color.Transparent, Color.Transparent)
                }

                Text("Guessed Source Colors:")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(guessA)
                            .border(1.dp, Color.Black, RoundedCornerShape(16.dp))

                    )
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(guessB)
                            .border(1.dp, Color.Black, RoundedCornerShape(16.dp)),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Unmix Mode")
                    Switch(
                        checked = true,
                        onCheckedChange = { checked -> if (!checked) showMixCard = true }
                    )
                }
            }
        }
    }

    // Color picker dialogs
    if (showDialog1) {
        ColorPickerDialog(onDismiss = { showDialog1 = false }) {
            HsvColorPicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                controller = controller,
                onColorChanged = {
                    color1 = it.color
                }
            )
        }
    }

    if (showDialog2) {
        ColorPickerDialog(onDismiss = { showDialog2 = false }) {
            HsvColorPicker(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                controller = controller,
                onColorChanged = {
                    color2 = it.color
                }
            )
        }
    }
}

@Composable
fun ColorDisplayBox(color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .clickable { onClick() }
            .border(1.dp, Color.Black, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
    ){}
}

@Composable
fun ColorPickerDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Pick a Color") },
        text = content
    )
}

fun findClosestColorPair(target: Color, ratio: Float, samples: Int = 500): Pair<Color, Color> {
    var bestA = Color.Black
    var bestB = Color.White
    var minDistance = Float.MAX_VALUE

    repeat(samples) {
        val a = Color(
            red = Math.random().toFloat(),
            green = Math.random().toFloat(),
            blue = Math.random().toFloat()
        )
        val b = Color(
            red = Math.random().toFloat(),
            green = Math.random().toFloat(),
            blue = Math.random().toFloat()
        )
        val blended = Color(
            red = a.red * (1 - ratio) + b.red * ratio,
            green = a.green * (1 - ratio) + b.green * ratio,
            blue = a.blue * (1 - ratio) + b.blue * ratio
        )
        val dist = colorDistance(blended, target)
        if (dist < minDistance) {
            minDistance = dist
            bestA = a
            bestB = b
        }
    }

    return Pair(bestA, bestB)
}

fun colorDistance(c1: Color, c2: Color): Float {
    val dr = c1.red - c2.red
    val dg = c1.green - c2.green
    val db = c1.blue - c2.blue
    return dr * dr + dg * dg + db * db
}


