package com.anant.fitbuddy.healthconnect

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/**
 * Privacy-policy entry point required by Health Connect.
 *
 * Android 13 and below invoke this via ACTION_SHOW_PERMISSIONS_RATIONALE.
 * Android 14+ reaches it through the VIEW_PERMISSION_USAGE activity-alias.
 */
class HealthPermissionsRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
                    .addCategory(Intent.CATEGORY_BROWSABLE)
            )
        }
        finish()
    }

    private companion object {
        const val PRIVACY_POLICY_URL = "https://anantdark.github.io/FitBuddy/privacy.html"
    }
}
