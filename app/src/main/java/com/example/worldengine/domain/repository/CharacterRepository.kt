package com.example.worldengine.domain.repository

import com.example.worldengine.domain.model.Character
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    fun observeCharacters(worldId: Long): Flow<List<Character>>

    /** Characters from every world, for cross-world pickers (e.g. assigning a generated portrait). */
    fun observeAllCharacters(): Flow<List<Character>>

    suspend fun getCharacter(id: Long): Character?
    suspend fun save(character: Character): Long
    suspend fun delete(character: Character)

    /** Sets [path] as the character's portrait/profile picture without touching its other fields. */
    suspend fun setPortrait(id: Long, path: String)
}
