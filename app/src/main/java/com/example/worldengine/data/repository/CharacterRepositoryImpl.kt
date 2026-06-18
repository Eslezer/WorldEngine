package com.example.worldengine.data.repository

import com.example.worldengine.core.data.local.dao.CharacterDao
import com.example.worldengine.data.mapper.toDomain
import com.example.worldengine.data.mapper.toEntity
import com.example.worldengine.domain.model.Character
import com.example.worldengine.domain.repository.CharacterRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CharacterRepositoryImpl(
    private val dao: CharacterDao,
    private val ioDispatcher: CoroutineDispatcher,
) : CharacterRepository {

    override fun observeCharacters(worldId: Long): Flow<List<Character>> =
        dao.observeByWorld(worldId).map { entities -> entities.map { it.toDomain() } }

    override fun observeAllCharacters(): Flow<List<Character>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getCharacter(id: Long): Character? =
        withContext(ioDispatcher) { dao.getById(id)?.toDomain() }

    override suspend fun save(character: Character): Long =
        withContext(ioDispatcher) { dao.upsert(character.toEntity()) }

    override suspend fun delete(character: Character) =
        withContext(ioDispatcher) { dao.delete(character.toEntity()) }

    override suspend fun setPortrait(id: Long, path: String) =
        withContext(ioDispatcher) { dao.updatePortrait(id, path) }
}
