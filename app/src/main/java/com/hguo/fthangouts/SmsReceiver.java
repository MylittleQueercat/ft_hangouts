package com.hguo.fthangouts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

public final class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }
        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts == null || parts.length == 0) {
            return;
        }
        String sender = parts[0].getOriginatingAddress();
        if (sender == null) {
            return;
        }
        StringBuilder body = new StringBuilder();
        for (SmsMessage part : parts) {
            if (part != null && part.getMessageBody() != null) {
                body.append(part.getMessageBody());
            }
        }
        try (ContactDatabase database = new ContactDatabase(context)) {
            Contact contact = database.findContactByPhone(sender);
            if (contact != null && contact.blocked) {
                return;
            }
            long contactId = contact == null
                    ? database.createContact(sender, sender, "", "", "", "", "", "")
                    : contact.id;
            database.addMessage(contactId, body.toString(), Message.RECEIVED,
                    System.currentTimeMillis(), Message.COMPLETE);
            SmsEvents.notifyChanged(context, contactId);
        }
    }
}
