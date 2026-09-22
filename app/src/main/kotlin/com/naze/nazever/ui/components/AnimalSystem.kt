package com.naze.nazever.ui.components

/**
 * Animal identity system (FR-15.2, FR-16.5, design §10/§11).
 * Single registry for every animal-themed element — notification sound,
 * empty states, note templates, mood, decorative UI.
 *
 * Assets are NOT hardcoded per screen: screens only reference [AnimalKind].
 * Adding a new animal later = one enum entry + asset mapping here.
 * No copyrighted assets: emoji + Material symbols are used until dedicated
 * CC0/OFL illustrations are bundled (FR-16.6 policy).
 */
enum class AnimalKind(val label: String, val emoji: String) {
    CAT("Cat", "🐱"),
    DOG("Dog", "🐶"),
    RABBIT("Rabbit", "🐰"),
    BIRD("Bird", "🐦"),
}

/** Visual + sonic metadata for an [AnimalKind]. */
data class AnimalIdentity(
    val kind: AnimalKind,
    /** Notification sound resource name (res/raw). Asset must be CC0 (FR-16.6). */
    val soundResourceName: String,
)

object AnimalSystem {
    fun identity(kind: AnimalKind): AnimalIdentity = when (kind) {
        AnimalKind.CAT -> AnimalIdentity(kind, "noti_cat")
        AnimalKind.DOG -> AnimalIdentity(kind, "noti_dog")
        AnimalKind.RABBIT -> AnimalIdentity(kind, "noti_rabbit")
        AnimalKind.BIRD -> AnimalIdentity(kind, "noti_bird")
    }
}
