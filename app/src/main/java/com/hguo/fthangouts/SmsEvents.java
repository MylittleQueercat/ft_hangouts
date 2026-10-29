package com.hguo.fthangouts;

import android.content.Context;
import android.content.Intent;

final class SmsEvents {
    static final String ACTION_CHANGED = "com.hguo.fthangouts.MESSAGE_CHANGED";
    static final String EXTRA_CONTACT_ID = "contact_id";
    static final String EXTRA_MESSAGE_ID = "message_id";

    private SmsEvents() { }

    static void notifyChanged(Context context, long contactId) {
        Intent intent = new Intent(ACTION_CHANGED);
        intent.setPackage(context.getPackageName());
        intent.putExtra(EXTRA_CONTACT_ID, contactId);
        context.sendBroadcast(intent);
    }
}
