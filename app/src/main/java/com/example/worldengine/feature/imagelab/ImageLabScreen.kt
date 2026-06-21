package com.example.worldengine.feature.imagelab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.worldengine.domain.model.GeneratedImage
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
    var showGallery by rememberSaveable { mutableStateOf(false) }

    // Refresh the API-key gate when returning here (e.g. after saving a key in Settings).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshKeyState() }

    val pendingDeleteImages = state.pendingDeleteImages
    if (pendingDeleteImages.isNotEmpty()) {
        val count = pendingDeleteImages.size
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteImage,
            title = { Text(if (count == 1) "Delete image?" else "Delete $count images?") },
            text = {
                Text(
                    pendingDeleteImages
                        .take(3)
                        .joinToString("\n") { File(it.filePath).name }
                        .let { names ->
                            if (count > 3) "$names\nand ${count - 3} more" else names
                        },
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteImage) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteImage) {
                    Text("Cancel")
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ImageLabHeader(
            showingGallery = showGallery,
            onOpenGallery = { showGallery = true },
            onCloseGallery = { showGallery = false },
        )

        if (showGallery) {
            DedicatedGalleryView(state, viewModel)
            return@Column
        }

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

        FolderPanel(state, viewModel)

        SettingsPanel(state, viewModel)

        Button(
            onClick = viewModel::generate,
            enabled = state.canGenerate,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.status == GenStatus.Loading) "Generating…" else "Generate")
        }

        AssignPortraitSection(state, viewModel)
    }
}

@Composable
private fun ImageLabHeader(
    showingGallery: Boolean,
    onOpenGallery: () -> Unit,
    onCloseGallery: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (showingGallery) "Image Gallery" else "Image Generation",
            style = MaterialTheme.typography.titleLarge,
        )
        IconButton(onClick = if (showingGallery) onCloseGallery else onOpenGallery) {
            if (showingGallery) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to image generation")
            } else {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Open image gallery")
            }
        }
    }
}

@Composable
private fun FolderPanel(state: ImageLabUiState, viewModel: ImageLabViewModel) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Folder", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.folderInput,
                onValueChange = viewModel::onFolderInputChange,
                label = { Text("Save new generations to") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.folders.forEach { folder ->
                    FilterChip(
                        selected = folder == state.selectedFolder,
                        onClick = { viewModel.onFolderSelected(folder) },
                        label = { Text(folder) },
                    )
                }
            }
        }
    }
}

/**
 * Lets the user attach the most recent generation to a character as their profile picture. This is
 * the intended Image-Gen ↔ Characters bridge: portraits are produced here and pushed onto a chosen
 * character, rather than being generated from inside the character editor. Hidden until there is an
 * image to assign and at least one character to assign it to.
 */
@Composable
private fun AssignPortraitSection(state: ImageLabUiState, viewModel: ImageLabViewModel) {
    if (state.latestImagePath == null || state.characters.isEmpty()) return

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Use as character portrait", style = MaterialTheme.typography.titleMedium)
            Text(
                "Assign the image above as the profile picture of a character in one of your worlds.",
                style = MaterialTheme.typography.bodySmall,
            )

            val selected = state.characters.firstOrNull { it.id == state.selectedCharacterId }
                ?: state.characters.first()
            LabeledDropdown(
                label = "Character",
                options = state.characters,
                selected = selected,
                optionLabel = { it.label },
                onSelected = { viewModel.onCharacterSelected(it.id) },
            )

            Button(
                onClick = viewModel::assignLatestToCharacter,
                enabled = state.canAssignPortrait,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Set as profile picture")
            }

            state.assignMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
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
                            contentScale = ContentScale.Fit,
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
private fun DedicatedGalleryView(state: ImageLabUiState, viewModel: ImageLabViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val preview = state.previewGalleryImage
        Card {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 280.dp, max = 620.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (preview != null) {
                    AsyncImage(
                        model = File(preview.filePath),
                        contentDescription = "Selected gallery image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        "No generated images yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        GallerySection(state, viewModel)
    }
}

@Composable
private fun GallerySection(state: ImageLabUiState, viewModel: ImageLabViewModel) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Gallery", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.newFolderInput,
                    onValueChange = viewModel::onNewFolderInputChange,
                    label = { Text("New folder") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = viewModel::createGalleryFolder,
                    enabled = state.newFolderInput.isNotBlank(),
                ) {
                    Text("Create")
                }
            }

            val selectedImages = state.selectedGalleryImages
            if (selectedImages.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${selectedImages.size} selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    LabeledDropdown(
                        label = "Move to",
                        options = state.folders,
                        selected = state.folders.firstOrNull { it == state.moveTargetFolder }
                            ?: GeneratedImage.DEFAULT_FOLDER,
                        optionLabel = { it },
                        onSelected = viewModel::onMoveTargetFolderSelected,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = viewModel::moveSelectedImage,
                            enabled = selectedImages.any { it.folder != state.moveTargetFolder },
                        ) {
                            Text("Move")
                        }
                        OutlinedButton(onClick = viewModel::exportSelectedImages) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Text("Download")
                        }
                        OutlinedButton(onClick = viewModel::requestDeleteSelectedImages) {
                            Text("Delete")
                        }
                        TextButton(onClick = viewModel::clearGallerySelection) {
                            Text("Clear")
                        }
                    }
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.galleryFolder == null,
                    onClick = { viewModel.onGalleryFolderSelected(null) },
                    label = { Text("All") },
                )
                state.folders.forEach { folder ->
                    FilterChip(
                        selected = state.galleryFolder == folder,
                        onClick = { viewModel.onGalleryFolderSelected(folder) },
                        label = { Text(folder) },
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.galleryImages.forEach { image ->
                    val selected = image.filePath in state.selectedGalleryImagePaths
                    Card(
                        onClick = { viewModel.toggleGalleryImageSelection(image.filePath) },
                        border = BorderStroke(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                        modifier = Modifier.width(112.dp),
                    ) {
                        Column {
                            AsyncImage(
                                model = File(image.filePath),
                                contentDescription = "Generated image in ${image.folder}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                            )
                            Text(
                                if (selected) "Selected" else image.folder,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }
            }
            if (state.galleryImages.isEmpty()) {
                Text("No images here yet.", style = MaterialTheme.typography.bodySmall)
            }
            state.galleryMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
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
