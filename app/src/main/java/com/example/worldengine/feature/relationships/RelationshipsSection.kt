package com.example.worldengine.feature.relationships

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.worldengine.domain.model.Character
import com.example.worldengine.domain.model.CharacterRelationship
import com.example.worldengine.domain.model.CustomRelationshipType
import com.example.worldengine.domain.model.HierarchyKind
import com.example.worldengine.domain.model.RelationshipTone
import com.example.worldengine.ui.components.LabeledDropdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Relationships section inside a world. Shows the relationship graph in one of three layouts —
 * **Web** (ring), **Tree** (layered top-down by one-way edges) and **Pyramid** (the same, narrowing
 * to an apex) — or, when a character is focused, an ego view centred on them (parents/superiors
 * above, children/subordinates below, mutual links to the sides). Characters with no relationships
 * are omitted from the graph. Nodes show the character's portrait when they have one. Self-contained:
 * owns its [RelationshipViewModel], FAB and dialog.
 */
@Composable
fun RelationshipsSection(
    worldId: Long,
    viewModel: RelationshipViewModel = koinViewModel { parametersOf(worldId) },
) {
    val relationships by viewModel.relationships.collectAsStateWithLifecycle()
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val customTypes by viewModel.customTypes.collectAsStateWithLifecycle()
    val typeOptions by viewModel.typeOptions.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val focusId by viewModel.focusCharacterId.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()

    val characterNames = remember(characters) { characters.associate { it.id to it.name } }
    val typesById = remember(customTypes) { customTypes.associateBy { it.id } }
    // Only characters that take part in at least one relationship belong in the graph.
    val connected = remember(characters, relationships) {
        characters.filter { c -> relationships.any { it.fromCharacterId == c.id || it.toCharacterId == c.id } }
    }
    val portraits = rememberPortraits(connected)
    val enoughCharacters = characters.size >= 2

    Box(modifier = Modifier.fillMaxSize()) {
        if (!enoughCharacters) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add at least two characters", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Relationships connect characters together. Create two or more characters in this " +
                        "world first, then link them here.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { ViewModeChips(selected = viewMode, onSelect = viewModel::setViewMode) }
                if (connected.isNotEmpty()) {
                    item { FocusSelector(connected, focusId, viewModel::setFocus) }
                }
                item {
                    if (connected.isEmpty()) {
                        Text(
                            "No relationships yet. Use the + button to connect two characters.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        val (graphChars, graphRels) = graphSubset(connected, relationships, focusId)
                        RelationshipGraph(graphChars, graphRels, typesById, viewMode, focusId, portraits)
                    }
                }
                items(relationships, key = { it.id }) { relationship ->
                    RelationshipCard(
                        relationship = relationship,
                        characterNames = characterNames,
                        typesById = typesById,
                        onEdit = { viewModel.startEdit(relationship) },
                        onDelete = { viewModel.delete(relationship) },
                    )
                }
            }
        }

        if (enoughCharacters) {
            FloatingActionButton(
                onClick = viewModel::startCreate,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add relationship")
            }
        }
    }

    draft?.let {
        RelationshipEditorDialog(
            draft = it,
            characters = characters,
            typeOptions = typeOptions,
            onFromSelect = viewModel::onFromSelect,
            onToSelect = viewModel::onToSelect,
            onTypeOptionSelect = viewModel::onTypeOptionSelect,
            onLabelChange = viewModel::onLabelChange,
            onSave = viewModel::save,
            onDismiss = viewModel::dismissDraft,
        )
    }
}

@Composable
private fun ViewModeChips(selected: RelationshipViewMode, onSelect: (RelationshipViewMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RelationshipViewMode.entries.forEach { mode ->
            FilterChip(selected = mode == selected, onClick = { onSelect(mode) }, label = { Text(mode.name) })
        }
    }
}

@Composable
private fun FocusSelector(characters: List<Character>, focusId: Long?, onSelect: (Long?) -> Unit) {
    val options = remember(characters) { listOf<Character?>(null) + characters }
    val selected = characters.firstOrNull { it.id == focusId }
    LabeledDropdown(
        label = "Focus",
        options = options,
        selected = selected,
        optionLabel = { it?.name ?: "Everyone (no focus)" },
        onSelected = { onSelect(it?.id) },
    )
}

/** Restricts the graph to a focused character's neighbourhood, or the whole connected set. */
private fun graphSubset(
    connected: List<Character>,
    relationships: List<CharacterRelationship>,
    focusId: Long?,
): Pair<List<Character>, List<CharacterRelationship>> {
    if (focusId == null) return connected to relationships
    val incident = relationships.filter { it.fromCharacterId == focusId || it.toCharacterId == focusId }
    val keep = incident.flatMapTo(HashSet()) { listOf(it.fromCharacterId, it.toCharacterId) }
    return connected.filter { it.id in keep } to incident
}

@Composable
private fun RelationshipGraph(
    characters: List<Character>,
    relationships: List<CharacterRelationship>,
    typesById: Map<String, CustomRelationshipType>,
    mode: RelationshipViewMode,
    focusId: Long?,
    portraits: Map<Long, ImageBitmap>,
) {
    val textMeasurer = rememberTextMeasurer()
    val nodeColor = MaterialTheme.colorScheme.primaryContainer
    val nodeBorder = MaterialTheme.colorScheme.primary
    val focusBorder = MaterialTheme.colorScheme.tertiary
    val labelColor = MaterialTheme.colorScheme.onSurface
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)

    // Tree ranks by family (Parent of), Pyramid by org (Superior of). Keyed on mode so switching
    // views recomputes (an earlier omission of the mode key reused a stale map).
    val levels = remember(characters, relationships, mode) {
        when (mode) {
            RelationshipViewMode.Tree -> computeLevels(characters, relationships, HierarchyKind.FAMILY)
            RelationshipViewMode.Pyramid -> computeLevels(characters, relationships, HierarchyKind.ORG)
            RelationshipViewMode.Web -> emptyMap()
        }
    }
    val maxLevel = levels.values.maxOrNull() ?: 0
    val edgeOffset = remember(relationships) { computeEdgeOffsets(relationships) }

    val canvasHeight = when (mode) {
        RelationshipViewMode.Web -> 300.dp
        else -> (140 + maxLevel * 90).dp
    }

    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Focus only filters to a character's neighbourhood; the layout still follows the chosen
            // view mode (so Tree stays a tree). In Tree/Pyramid that naturally puts the focus's
            // parents above, children below and mutual peers beside it.
            val title = "Relationship ${mode.name.lowercase()}" + if (focusId != null) " (focused)" else ""
            Text(title, style = MaterialTheme.typography.titleMedium)
            Canvas(modifier = Modifier.fillMaxWidth().height(canvasHeight)) {
                if (characters.isEmpty()) return@Canvas
                val nodeRadius = 18.dp.toPx()
                val positions: Map<Long, Offset> = when (mode) {
                    RelationshipViewMode.Web -> circularPositions(characters, size.width, size.height)
                    RelationshipViewMode.Tree ->
                        layeredPositions(characters, levels, maxLevel, size.width, size.height, pyramid = false)
                    RelationshipViewMode.Pyramid ->
                        layeredPositions(characters, levels, maxLevel, size.width, size.height, pyramid = true)
                }

                val spacing = 14.dp.toPx()
                val headSize = 9.dp.toPx()
                relationships.forEach { rel ->
                    val fromNode = positions[rel.fromCharacterId] ?: return@forEach
                    val toNode = positions[rel.toCharacterId] ?: return@forEach
                    // Lane direction uses a consistent pair orientation (low id → high id) so that
                    // opposite-direction edges between the same pair land in different lanes.
                    val lowPos = positions[minOf(rel.fromCharacterId, rel.toCharacterId)] ?: fromNode
                    val highPos = positions[maxOf(rel.fromCharacterId, rel.toCharacterId)] ?: toNode
                    val custom = rel.customTypeId?.let { typesById[it] }
                    val color = custom?.color ?: rel.type.color
                    val tone = custom?.tone ?: rel.type.tone
                    drawRelationshipEdge(
                        from = fromNode,
                        to = toNode,
                        lowPos = lowPos,
                        highPos = highPos,
                        offsetPx = (edgeOffset[rel.id] ?: 0f) * spacing,
                        color = color,
                        tone = tone,
                        mutual = rel.type.mutual,
                        nodeRadius = nodeRadius,
                        headSize = headSize,
                    )
                }

                characters.forEach { character ->
                    val pos = positions[character.id] ?: return@forEach
                    val portrait = portraits[character.id]
                    if (portrait != null) {
                        drawCircularImage(portrait, pos, nodeRadius)
                    } else {
                        drawCircle(color = nodeColor, radius = nodeRadius, center = pos)
                    }
                    val border = if (character.id == focusId) focusBorder else nodeBorder
                    val borderWidth = if (character.id == focusId) 4.dp.toPx() else 2.dp.toPx()
                    drawCircle(color = border, radius = nodeRadius, center = pos, style = Stroke(width = borderWidth))
                    val layout = textMeasurer.measure(character.name, style = labelStyle)
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(pos.x - layout.size.width / 2f, pos.y + nodeRadius + 2.dp.toPx()),
                    )
                }
            }
            RelationshipLegend()
        }
    }
}

/** Explains the line styles (tone) and arrowheads (direction) used in the graph. */
@Composable
private fun RelationshipLegend() {
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Legend", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LegendLine(RelationshipTone.POSITIVE, "Friendly", lineColor)
            LegendLine(RelationshipTone.NEUTRAL, "Neutral", lineColor)
            LegendLine(RelationshipTone.HOSTILE, "Hostile", lineColor)
        }
        Text(
            "→ one-way    ↔ two-way (mutual).  Tree ranks family (Parent of); Pyramid ranks org " +
                "(Superior of).",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun LegendLine(tone: RelationshipTone, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(modifier = Modifier.size(width = 32.dp, height = 10.dp)) {
            val y = size.height / 2f
            drawRelationshipLine(Offset(0f, y), Offset(size.width, y), color, tone, 3.dp.toPx())
        }
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

/** Decodes each character's portrait file (downscaled, center-crop-friendly) into an ImageBitmap. */
@Composable
private fun rememberPortraits(characters: List<Character>): Map<Long, ImageBitmap> {
    val paths = characters.mapNotNull { c -> c.portraitPath?.let { c.id to it } }
    return produceState(emptyMap<Long, ImageBitmap>(), paths) {
        value = withContext(Dispatchers.IO) {
            paths.mapNotNull { (id, path) ->
                runCatching { decodeDownscaled(path, 192)?.asImageBitmap() }.getOrNull()?.let { id to it }
            }.toMap()
        }
    }.value
}

private fun decodeDownscaled(path: String, target: Int): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    val minSide = min(bounds.outWidth, bounds.outHeight)
    if (minSide <= 0) return null
    var sample = 1
    while (minSide / (sample * 2) >= target) sample *= 2
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}

/** Even ring layout (Web). */
private fun circularPositions(characters: List<Character>, w: Float, h: Float): Map<Long, Offset> {
    val n = characters.size
    val center = Offset(w / 2f, h / 2f)
    val radius = min(w, h) / 2f * 0.74f
    return characters.mapIndexed { i, c ->
        val angle = (2.0 * Math.PI * i / n) - Math.PI / 2.0
        c.id to Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
    }.toMap()
}

/** Layered top-down layout used by Tree and Pyramid (rows narrow toward the top when [pyramid]). */
private fun layeredPositions(
    characters: List<Character>,
    levels: Map<Long, Int>,
    maxLevel: Int,
    w: Float,
    h: Float,
    pyramid: Boolean,
): Map<Long, Offset> {
    val byLevel = characters.groupBy { levels[it.id] ?: 0 }
    val rowHeight = h / (maxLevel + 1)
    val out = HashMap<Long, Offset>(characters.size)
    byLevel.forEach { (level, rowChars) ->
        val y = rowHeight * (level + 0.5f)
        val rowWidth = if (pyramid) w * (level + 1f) / (maxLevel + 1f) else w
        val startX = (w - rowWidth) / 2f
        rowChars.forEachIndexed { i, c ->
            out[c.id] = Offset(startX + rowWidth * (i + 0.5f) / rowChars.size, y)
        }
    }
    return out
}

/**
 * Assigns each character a depth from the one-way edges of a single [HierarchyKind] (from = parent →
 * to = child): roots with no incoming such edge sit at level 0, children one below their deepest
 * parent. Tree ranks by [HierarchyKind.FAMILY] (Parent of), Pyramid by [HierarchyKind.ORG] (Superior
 * of); other links never stack. Cycles are broken so the recursion always terminates.
 */
private fun computeLevels(
    characters: List<Character>,
    relationships: List<CharacterRelationship>,
    kind: HierarchyKind,
): Map<Long, Int> {
    val ids = characters.mapTo(HashSet()) { it.id }
    val parents = relationships
        .filter { it.type.hierarchy == kind && it.fromCharacterId in ids && it.toCharacterId in ids }
        .groupBy({ it.toCharacterId }, { it.fromCharacterId })

    val level = HashMap<Long, Int>()
    fun depth(id: Long, visiting: Set<Long>): Int {
        level[id]?.let { return it }
        val ps = parents[id]
        if (ps.isNullOrEmpty() || id in visiting) {
            if (id !in visiting) level[id] = 0
            return 0
        }
        val d = ps.maxOf { depth(it, visiting + id) } + 1
        level[id] = d
        return d
    }
    characters.forEach { depth(it.id, emptySet()) }
    return level
}

/**
 * Assigns each edge a signed lane index within its character pair (…-1, 0, +1…), centred on 0, so
 * parallel relationships between the same two characters draw side by side instead of overlapping.
 */
private fun computeEdgeOffsets(relationships: List<CharacterRelationship>): Map<Long, Float> {
    val groups = relationships.groupBy {
        val a = it.fromCharacterId
        val b = it.toCharacterId
        if (a <= b) a to b else b to a
    }
    val out = HashMap<Long, Float>(relationships.size)
    groups.values.forEach { group ->
        val k = group.size
        group.forEachIndexed { i, rel -> out[rel.id] = i - (k - 1) / 2f }
    }
    return out
}

/** Unit perpendicular to the line low → high (a consistent direction for both edges of a pair). */
private fun perpUnit(lowPos: Offset, highPos: Offset): Offset {
    val dx = highPos.x - lowPos.x
    val dy = highPos.y - lowPos.y
    val len = kotlin.math.hypot(dx, dy)
    if (len == 0f) return Offset(0f, 0f)
    return Offset(-dy / len, dx / len)
}

/**
 * Draws one relationship edge. Edges sharing a character pair are shifted into separate parallel
 * lanes (offset perpendicular along their whole length, using a consistent low → high orientation so
 * opposite-direction edges still split apart). Arrowheads sit at the `to` end, plus the `from` end
 * when [mutual].
 */
private fun DrawScope.drawRelationshipEdge(
    from: Offset,
    to: Offset,
    lowPos: Offset,
    highPos: Offset,
    offsetPx: Float,
    color: Color,
    tone: RelationshipTone,
    mutual: Boolean,
    nodeRadius: Float,
    headSize: Float,
) {
    val start: Offset
    val end: Offset
    if (offsetPx == 0f) {
        start = from
        end = to
    } else {
        val perp = perpUnit(lowPos, highPos)
        val shift = Offset(perp.x * offsetPx, perp.y * offsetPx)
        start = Offset(from.x + shift.x, from.y + shift.y)
        end = Offset(to.x + shift.x, to.y + shift.y)
    }
    drawRelationshipLine(start, end, color, tone, 3.dp.toPx())
    drawArrowHead(start, end, nodeRadius, color, headSize)
    if (mutual) drawArrowHead(end, start, nodeRadius, color, headSize)
}

/** Tone → stroke dash pattern: solid (positive), long dashes (neutral), round dots (hostile). */
private fun DrawScope.toneEffect(tone: RelationshipTone): PathEffect? = when (tone) {
    RelationshipTone.POSITIVE -> null
    RelationshipTone.NEUTRAL -> PathEffect.dashPathEffect(floatArrayOf(14.dp.toPx(), 8.dp.toPx()))
    RelationshipTone.HOSTILE -> PathEffect.dashPathEffect(floatArrayOf(0.5.dp.toPx(), 7.dp.toPx()))
}

/** Draws a portrait clipped into the node circle, centre-cropped to a square. */
private fun DrawScope.drawCircularImage(image: ImageBitmap, center: Offset, radius: Float) {
    val side = min(image.width, image.height)
    val srcOffset = IntOffset((image.width - side) / 2, (image.height - side) / 2)
    val srcSize = IntSize(side, side)
    val d = (radius * 2f).toInt()
    val dstOffset = IntOffset((center.x - radius).toInt(), (center.y - radius).toInt())
    val circle = Path().apply { addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)) }
    clipPath(circle) {
        drawImage(image, srcOffset, srcSize, dstOffset, IntSize(d, d))
    }
}

/** Straight tone-styled line, used for the [RelationshipLegend] swatches. */
private fun DrawScope.drawRelationshipLine(
    start: Offset,
    end: Offset,
    color: Color,
    tone: RelationshipTone,
    strokeWidth: Float,
) {
    val cap = if (tone == RelationshipTone.HOSTILE) StrokeCap.Round else StrokeCap.Butt
    drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth, cap = cap, pathEffect = toneEffect(tone))
}

/** Draws a small filled triangle pointing at the [to] node (offset back by the node radius). */
private fun DrawScope.drawArrowHead(from: Offset, to: Offset, nodeRadius: Float, color: Color, sizePx: Float) {
    val angle = atan2((to.y - from.y).toDouble(), (to.x - from.x).toDouble())
    val tip = Offset(to.x - nodeRadius * cos(angle).toFloat(), to.y - nodeRadius * sin(angle).toFloat())
    val left = Offset(tip.x - sizePx * cos(angle - 0.4).toFloat(), tip.y - sizePx * sin(angle - 0.4).toFloat())
    val right = Offset(tip.x - sizePx * cos(angle + 0.4).toFloat(), tip.y - sizePx * sin(angle + 0.4).toFloat())
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(left.x, left.y)
        lineTo(right.x, right.y)
        close()
    }
    drawPath(path, color = color)
}

@Composable
private fun RelationshipCard(
    relationship: CharacterRelationship,
    characterNames: Map<Long, String>,
    typesById: Map<String, CustomRelationshipType>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val from = characterNames[relationship.fromCharacterId] ?: "?"
    val to = characterNames[relationship.toCharacterId] ?: "?"
    val connector = if (relationship.type.mutual) "↔" else "→"
    val typeName = relationship.customTypeId?.let { typesById[it]?.name } ?: relationship.type.label

    Card(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$from  $connector  $to",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val subtitle = buildString {
                    append(typeName)
                    if (relationship.label.isNotBlank()) append(" · ${relationship.label}")
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete relationship")
            }
        }
    }
}
