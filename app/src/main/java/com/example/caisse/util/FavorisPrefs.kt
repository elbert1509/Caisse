package com.example.caisse.util

import android.content.Context
import java.util.UUID

/**
 * Favoris produit (écran "Prendre une commande") : stockage local au pad (SharedPreferences),
 * pas de synchro Firestore — c'est une commodité d'usage par appareil, pas une donnée métier
 * à répliquer entre pads.
 */
object FavorisPrefs {

    private const val PREFS = "favoris_prefs"
    private const val KEY_FAVORIS = "favoris_produit_ids"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getFavoris(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_FAVORIS, emptySet()) ?: emptySet()

    /** Bascule l'état favori du produit et retourne le nouvel état (true = ajouté). */
    fun toggleFavori(context: Context, productId: UUID): Boolean {
        val current = getFavoris(context).toMutableSet()
        val id = productId.toString()
        val nowFavori = if (!current.add(id)) {
            current.remove(id)
            false
        } else {
            true
        }
        prefs(context).edit().putStringSet(KEY_FAVORIS, current).apply()
        return nowFavori
    }
}
