package com.raccoon.infinityarts.inappbilling;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.util.Log;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import java.util.ArrayList;
import java.util.List;

public class BillingManager implements PurchasesUpdatedListener {
    private Activity activity;
    private BillingClient billingClient;
    private boolean isServiceConnected = false;
    private int billingClientResponseCode;
    private BillingUpdatesListener billingUpdatesListener;
    private static final String TAG = "BillingManager";

    public BillingManager(Activity activity, final BillingUpdatesListener billingUpdatesListener) {
        this.activity = activity;
        this.billingUpdatesListener = billingUpdatesListener;
        billingClient = BillingClient.newBuilder(activity).setListener(this).build();
        startServiceConnection(new Runnable() {
            @Override
            public void run() {
                billingUpdatesListener.onBillingClientSetupFinished();

            }
        });
    }

    public void startServiceConnection(final Runnable runnable) {

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(int responseCode) {

                if (responseCode == BillingClient.BillingResponse.OK) {
                    isServiceConnected = true;
                    if (runnable != null) {
                        runnable.run();
                    }
                }
                billingClientResponseCode = responseCode;

            }

            @Override
            public void onBillingServiceDisconnected() {

                isServiceConnected = false;
            }
        });
    }

    public interface BillingUpdatesListener {
        void onBillingClientSetupFinished();

        void onConsumeFinished(String token, @BillingClient.BillingResponse int result);

        void onPurchasesUpdated(List<Purchase> purchases);
    }

    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {

        if (responseCode == BillingClient.BillingResponse.OK) {

            billingUpdatesListener.onPurchasesUpdated(purchases);
        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            Log.i(TAG, "onPurchasesUpdated() – user cancelled the purchase flow – skipping");
        } else {
            Log.w(TAG, "onPurchasesUpdated() got unknown resultCode: " + responseCode);
        }

    }

    private void executeServiceRequest(Runnable runnable) {
        if (isServiceConnected) {
            runnable.run();
        } else {
            // If the billing service disconnects, try to reconnect once.
            startServiceConnection(runnable);
        }
    }

    public void initiatePurchaseFlow(final String skuId, final ArrayList<String> oldSkus,
                                     final @BillingClient.SkuType String billingType) {
        Runnable purchaseFlowRequest = new Runnable() {
            @Override
            public void run() {
                BillingFlowParams mParams = BillingFlowParams.newBuilder().
                        setSku(skuId).setType(billingType).setOldSkus(oldSkus).build();
                billingClient.launchBillingFlow(activity, mParams);
            }
        };
        executeServiceRequest(purchaseFlowRequest);
    }
}