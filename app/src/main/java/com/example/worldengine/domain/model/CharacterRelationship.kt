package com.example.worldengine.domain.model

import androidx.compose.ui.graphics.Color

/** Whether a relationship reads both ways (arrowheads on both ends) or one way (single arrow). */
enum class RelationshipDirection { MUTUAL, ONE_WAY }

/** Sentiment of a relationship, mapped to a line style in the graph (and shown in the legend). */
enum class RelationshipTone { POSITIVE, NEUTRAL, HOSTILE }

/**
 * Which hierarchy a one-way link belongs to. [FAMILY] (e.g. Parent of) ranks the **Tree** view;
 * [ORG] (e.g. Superior of) ranks the **Pyramid** view. [NONE] never stacks characters — that covers
 * all mutual links and one-way links that aren't a ranking (Crush). Keeping family and org separate
 * means e.g. "Superior of" between two partners doesn't disturb their family tree, and vice versa.
 */
enum class HierarchyKind { NONE, FAMILY, ORG }

/**
 * Kinds of links between characters, each carrying display + layout meaning:
 *  - [direction] decides the arrows: [RelationshipDirection.MUTUAL] draws a head on both ends (a
 *    two-way bond like Partner/Rival), [RelationshipDirection.ONE_WAY] a single head from → to.
 *  - [hierarchy] decides which view ranks by it: family links rank the Tree, org links the Pyramid.
 *  - [tone] decides the line style: positive = solid, neutral = dashed, hostile = dotted.
 *  - [color] tints the edge; defaults are semantic (Enemy red, Friend green, …).
 */
enum class RelationshipType(
    val label: String,
    val direction: RelationshipDirection,
    val hierarchy: HierarchyKind,
    val tone: RelationshipTone,
    val color: Color,
) {
    FRIEND("Friend", RelationshipDirection.MUTUAL, HierarchyKind.NONE, RelationshipTone.POSITIVE, Color(0xFF4CAF50)),
    ALLY("Ally", RelationshipDirection.MUTUAL, HierarchyKind.NONE, RelationshipTone.POSITIVE, Color(0xFF2196F3)),
    PARTNER("Partner", RelationshipDirection.MUTUAL, HierarchyKind.NONE, RelationshipTone.POSITIVE, Color(0xFFE91E63)),
    FAMILY("Family", RelationshipDirection.MUTUAL, HierarchyKind.NONE, RelationshipTone.POSITIVE, Color(0xFF9C27B0)),
    PARENT_OF("Parent of", RelationshipDirection.ONE_WAY, HierarchyKind.FAMILY, RelationshipTone.POSITIVE, Color(0xFF7E57C2)),
    CRUSH("Crush", RelationshipDirection.ONE_WAY, HierarchyKind.NONE, RelationshipTone.POSITIVE, Color(0xFFFF4081)),
    SUPERIOR_OF("Superior of", RelationshipDirection.ONE_WAY, HierarchyKind.ORG, RelationshipTone.NEUTRAL, Color(0xFF607D8B)),
    RIVAL("Rival", RelationshipDirection.MUTUAL, HierarchyKind.NONE, RelationshipTone.HOSTILE, Color(0xFFFF9800)),
    ENEMY("Enemy", RelationshipDirection.MUTUAL, HierarchyKind.NONE, RelationshipTone.HOSTILE, Color(0xFFF44336)),
    ;

    /** True for two-way bonds (drawn with arrowheads on both ends). */
    val mutual: Boolean get() = direction == RelationshipDirection.MUTUAL
}

/** A directed relationship edge [fromCharacterId] → [toCharacterId] within one world. */
data class CharacterRelationship(
    val id: Long = 0,
    val worldId: Long,
    val fromCharacterId: Long,
    val toCharacterId: Long,
    val type: RelationshipType = RelationshipType.FRIEND,
    val label: String = "",
    val createdAt: Long = 0,
    /** Optional custom type id ([CustomRelationshipType]); when set it overrides display name/colour. */
    val customTypeId: String? = null,
)
