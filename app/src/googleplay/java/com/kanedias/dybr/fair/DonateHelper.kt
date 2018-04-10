package com.kanedias.dybr.fair

import android.support.v7.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponse
import android.support.v4.content.ContextCompat.startActivity
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.BillingFlowParams




/**
 * Flavor-specific donation helper class. This manages menu option "Donate" in the main activity.
 *
 * @author Kanedias
 *
 * Created on 10.04.18
 */
class DonateHelper(private val activity: AppCompatActivity) : PurchasesUpdatedListener, ConsumeResponseListener {

    private var billingClient : BillingClient = BillingClient.newBuilder(activity).setListener(this).build()
    private var canProceed = false

    init {
        billingClient.startConnection(object: BillingClientStateListener {

            override fun onBillingSetupFinished(@BillingClient.BillingResponse responseCode: Int){
                if (responseCode == BillingClient.BillingResponse.OK) {
                    canProceed = true
                }
            }

            override fun onBillingServiceDisconnected() {
                // It will try to reconnect later by itself anyway
                canProceed = false
            }
        })
    }

    fun donate() {
        if (!canProceed)
            return

        val flowParams = BillingFlowParams.newBuilder().setSku("small").setType(SkuType.INAPP).build()
        val responseCode = billingClient.launchBillingFlow(activity, flowParams)
        if (responseCode != BillingResponse.OK) {
            Log.e("[Billing]", "Couldn't start billing flow, error code: $responseCode")
        }
    }

    override fun onPurchasesUpdated(responseCode: Int, purchases: MutableList<Purchase>?) {
        if (responseCode == BillingResponse.OK && purchases != null) {
            for (purchase in purchases) {
                billingClient.consumeAsync(purchase.purchaseToken, this)
            }
            return
        }

        if (responseCode == BillingResponse.USER_CANCELED) {
            Log.w("[Billing]", "User canceled purchase of donation")
        } else {
            Log.e("[Billing]", "Unexpected error: response code = $responseCode")
        }
    }

    override fun onConsumeResponse(responseCode: Int, purchaseToken: String?) {
        if (responseCode == BillingResponse.OK) {
            Log.i("[Billing]", "Purchase consumed, token: $purchaseToken")
            MaterialDialog.Builder(activity)
                    .title(R.string.donate)
                    .content(R.string.thanks_for_your_pledge)
                    .positiveText(android.R.string.ok)
                    .negativeText(R.string.request_a_feature)
                    .onNegative({ _, _ -> redirectToIssues() })
                    .show()
            return
        }

        // not consumed, should not happen
        Log.e("[Billing]", "Consume failed, response code = $responseCode")
    }

    /**
     * Redirect those who want to request a feature to the issue tracker
     */
    private fun redirectToIssues() {
        MaterialDialog.Builder(activity)
                .title(R.string.donate)
                .content(R.string.create_issue)
                .positiveText(R.string.understood)
                .negativeText(android.R.string.cancel)
                .onPositive({_, _ ->
                    val starter = Intent(Intent.ACTION_VIEW)
                    starter.data = Uri.parse("https://gitlab.com/Kanedias/Fair/issues")
                    activity.startActivity(starter)
                }).show()
    }

}