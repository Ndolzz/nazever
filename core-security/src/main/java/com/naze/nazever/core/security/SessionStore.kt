package com.naze.nazever.core.security

/**
 * Abstraction over secure session persistence.
 * Implementations must store tokens only in encrypted storage.
 */
interface SessionStore {
    fun save(session: SessionData)
    fun read(): SessionData?
    fun clear()
    fun hasSession(): Boolean
}
