package com.nazeworks.nazever.data.auth

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/** user_metadata yang dikirim ke Supabase Auth saat sign up. Tidak pernah berisi password. */
internal fun buildJsonObjectDisplayName(displayName: String): JsonObject = buildJsonObject {
    put("display_name", JsonPrimitive(displayName))
}
