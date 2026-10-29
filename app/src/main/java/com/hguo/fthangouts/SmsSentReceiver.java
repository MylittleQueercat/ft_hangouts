package com.hguo.fthangouts;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class SmsSentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long messageId = intent.getLongExtra(SmsEvents.EXTRA_MESSAGE_ID, -1);
        long contactId = intent.getLongExtra(SmsEvents.EXTRA_CONTACT_ID, -1);
        if (messageId < 0 || contactId < 0) {
            return;
        }
        try (ContactDatabase database = new ContactDatabase(context)) {
            database.markMessageSent(messageId, getResultCode() == Activity.RESULT_OK);
        }
        SmsEvents.notifyChanged(context, contactId);
    }
}
