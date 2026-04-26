package com.roofrecon.mobile.auth

import android.content.Context

/** Stores the bearer token issued by /api/mobile/auth in encrypted shared prefs. */
object SessionStore {
    private const val PREFS = "roof_recon_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_EMAIL = "email"

    fun save(ctx: Context, token: String, email: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun token(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

    fun email(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_EMAIL, null)

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
