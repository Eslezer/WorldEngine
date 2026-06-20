package com.example.worldengine.domain.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/**
 * A user-defined relationship type built on top of a built-in [RelationshipType] template. It reuses
 * the template's behaviour — its [RelationshipDirection] (mutual vs one-way arrows, and so its
 * Tree/Pyramid layering) and its [RelationshipTone] (line style) — but carries a custom [name] and
 * optional [colorArgb], so e.g. a "Sire / Childe" type can be based on the "Parent of" template.
 * Stored globally and reusable across worlds.
 */
@Serializable
data class CustomRelationshipType(
    val id: String,
    val name: String,
    val base: RelationshipType,
    val colorArgb: Int? = null,
) {
    val direction: RelationshipDirection get() = base.direction
    val mutual: Boolean get() = base.mutual
    val hierarchy: HierarchyKind get() = base.hierarchy
    val tone: RelationshipTone get() = base.tone
    val color: Color get() = colorArgb?.let { Color(it) } ?: base.color
}
