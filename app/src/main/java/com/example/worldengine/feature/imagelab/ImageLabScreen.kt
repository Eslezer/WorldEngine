package com.example.worldengine.feature.imagelab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.worldengine.domain.model.ImageModel
import com.example.worldengine.domain.model.NoiseSchedule
import com.example.worldengine.domain.model.ResolutionPreset
import com.example.worldengine.domain.model.Sampler
import com.example.worldengine.ui.components.LabeledDropdown
import org.koin.androidx.compose.koinViewModel
import java.io.File
import kotlin.math.roundToInt

@Composable
fun ImageLabScreen(viewModel: ImageLabViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Image Lab", style = MaterialTheme.typography.headlineMedium)

        if (!state.hasApiKey) {
            Card {
                Text(
                    "Add your NovelAI API key in the Settings tab to start generating.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        ResultArea(state)

        OutlinedTextField(
            value = state.prompt,
            onValueChange = viewModel::onPromptChange,
            label = { Text("Prompt") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.negativePrompt,
            onValueChange = viewModel::onNegativePromptChange,
            label = { Text("Negative prompt") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )

        SettingsPanel(state, viewModel)

        Button(
            onClick = viewModel::generate,
            enabled = state.canGenerate,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.status == GenStatus.Loading) "Generating…" else "Generate")
        }
    }
}

@Composable
private fun ResultArea(state: ImageLabUiState) {
    Card {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(state.settings.width.toFloat() / state.settings.height.toFloat()),
            contentAlignment = Alignment.Center,
        ) {
            when (val status = state.status) {
                GenStatus.Loading -> CircularProgressIndicator()
                is GenStatus.Error -> Text(
                    status.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
                GenStatus.Idle -> {
                    val path = state.latestImagePath
                    if (path != null) {
                        AsyncImage(
                            model = File(path),
                            contentDescription = "Generated image",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            "Your generated image will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPanel(state: ImageLabUiState, viewModel: ImageLabViewModel) {
    val s = state.settings
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.titleMedium)

            LabeledDropdown(
                label = "Model",
                options = ImageModel.entries,
                selected = s.model,
                optionLabel = { it.label },
                onSelected = viewModel::onModelChange,
            )

            Text("Resolution", style = MaterialTheme.typography.labelLarge)
            ResolutionChips(state, viewModel)

            LabeledDropdown(
                label = "Sampler",
                options = Sampler.entries,
                selected = s.sampler,
                optionLabel = { it.label },
                onSelected = viewModel::onSamplerChange,
            )
            LabeledDropdown(
                label = "Noise schedule",
                options = NoiseSchedule.entries,
                selected = s.noiseSchedule,
                optionLabel = { it.label },
                onSelected = viewModel::onNoiseScheduleChange,
            )

            Text("Steps: ${s.steps}", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = s.steps.toFloat(),
                onValueChange = { viewModel.onStepsChange(it.roundToInt()) },
                valueRange = 1f..28f,
            )

            Text("Guidance (scale): ${"%.1f".format(s.scale)}", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = s.scale.toFloat(),
                onValueChange = { viewModel.onScaleChange((it * 10).roundToInt() / 10.0) },
                valueRange = 0f..10f,
            )

            SeedRow(state, viewModel)

            val hint = if (s.isFreeGeneration) {
                "⚡ Free generation (Opus tier)"
            } else {
                "💎 May cost Anlas (resolution/steps above the free limit)"
            }
            Text(hint, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ResolutionChips(state: ImageLabUiState, viewModel: ImageLabViewModel) {
    val s = state.settings
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ResolutionPreset.all.forEach { preset ->
            val selected = s.width == preset.width && s.height == preset.height
            FilterChip(
                selected = selected,
                onClick = { viewModel.onResolutionChange(preset) },
                label = { Text("${preset.label} ${preset.width}×${preset.height}") },
            )
        }
    }
}

@Composable
private fun SeedRow(state: ImageLabUiState, viewModel: ImageLabViewModel) {
    val s = state.settings
    val seedText = if (s.seed <= 0L) "" else s.seed.toString()
    OutlinedTextField(
        value = seedText,
        onValueChange = { input ->
            viewModel.onSeedChange(input.filter(Char::isDigit).take(10).toLongOrNull() ?: 0L)
        },
        label = { Text("Seed (blank = random)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(onClick = viewModel::randomizeSeed) { Text("🎲 Randomize seed") }
}
