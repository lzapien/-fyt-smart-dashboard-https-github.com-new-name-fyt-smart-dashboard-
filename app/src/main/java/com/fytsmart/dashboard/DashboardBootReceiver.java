package com.fytsmart.dashboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** No GPS start or third-party launch from this receiver. Android may deny a background UI request. */
public final class DashboardBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) DashboardRecoveryController.get(context).onBoot();
    }
}
