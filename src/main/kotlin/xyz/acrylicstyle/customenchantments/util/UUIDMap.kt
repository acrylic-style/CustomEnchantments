package xyz.acrylicstyle.customenchantments.util

import java.util.UUID

class UUIDMap<V : Any>(private val constructor: (UUID) -> V) : HashMap<UUID, V>() {
    override operator fun get(key: UUID): V = computeIfAbsent(key, constructor)
}
