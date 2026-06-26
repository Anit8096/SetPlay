package com.kmp.setplay.data.repository

import kotlinx.browser.window

// webMain actual
// Returns the current origin automatically —
// works for http://localhost:8080 in dev and your deployed URL in prod.
// Add both to Supabase Dashboard → Auth → URL Configuration → Redirect URLs.
actual fun getOAuthRedirectUrl(): String = window.location.origin