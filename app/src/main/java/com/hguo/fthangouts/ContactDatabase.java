package com.hguo.fthangouts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.telephony.PhoneNumberUtils;

import java.util.ArrayList;
import java.util.List;

final class ContactDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "hangouts.db";
    private static final int DATABASE_VERSION = 3;
    private static final String[] COLUMNS = {
            "id", "name", "phone", "email", "address", "notes", "photo_uri"
    };

    ContactDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase database) {
        database.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE contacts ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "phone TEXT NOT NULL, "
                + "email TEXT NOT NULL DEFAULT '', "
                + "address TEXT NOT NULL DEFAULT '', "
                + "notes TEXT NOT NULL DEFAULT '', "
                + "photo_uri TEXT NOT NULL DEFAULT '')");
        createMessagesTable(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createMessagesTable(database);
        }
        if (oldVersion < 3) {
            database.execSQL("ALTER TABLE contacts ADD COLUMN photo_uri TEXT NOT NULL DEFAULT ''");
        }
    }

    List<Contact> listContacts() {
        List<Contact> contacts = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("contacts", COLUMNS,
                null, null, null, null, "name COLLATE NOCASE ASC, id ASC")) {
            while (cursor.moveToNext()) {
                contacts.add(readContact(cursor));
            }
        }
        return contacts;
    }

    Contact getContact(long id) {
        try (Cursor cursor = getReadableDatabase().query("contacts", COLUMNS,
                "id = ?", new String[]{String.valueOf(id)}, null, null, null)) {
            return cursor.moveToFirst() ? readContact(cursor) : null;
        }
    }

    long createContact(String name, String phone, String email, String address, String notes,
                       String photoUri) {
        return getWritableDatabase().insertOrThrow("contacts", null,
                values(name, phone, email, address, notes, photoUri));
    }

    boolean updateContact(long id, String name, String phone, String email,
                          String address, String notes, String photoUri) {
        return getWritableDatabase().update("contacts",
                values(name, phone, email, address, notes, photoUri),
                "id = ?", new String[]{String.valueOf(id)}) == 1;
    }

    boolean deleteContact(long id) {
        return getWritableDatabase().delete("contacts", "id = ?",
                new String[]{String.valueOf(id)}) == 1;
    }

    Contact findContactByPhone(String phone) {
        for (Contact contact : listContacts()) {
            if (PhoneNumberUtils.compare(contact.phone, phone)) {
                return contact;
            }
        }
        return null;
    }

    List<Message> listMessages(long contactId) {
        List<Message> messages = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("messages",
                new String[]{"id", "contact_id", "body", "direction", "timestamp", "status"},
                "contact_id = ?", new String[]{String.valueOf(contactId)}, null, null,
                "timestamp ASC, id ASC")) {
            while (cursor.moveToNext()) {
                messages.add(new Message(cursor.getLong(0), cursor.getLong(1), cursor.getString(2),
                        cursor.getInt(3), cursor.getLong(4), cursor.getInt(5)));
            }
        }
        return messages;
    }

    long addMessage(long contactId, String body, int direction, long timestamp, int status) {
        ContentValues values = new ContentValues();
        values.put("contact_id", contactId);
        values.put("body", body);
        values.put("direction", direction);
        values.put("timestamp", timestamp);
        values.put("status", status);
        return getWritableDatabase().insertOrThrow("messages", null, values);
    }

    void markMessageSent(long id, boolean succeeded) {
        if (succeeded) {
            getWritableDatabase().execSQL("UPDATE messages SET status = 1 "
                    + "WHERE id = ? AND status != 2", new Object[]{id});
        } else {
            getWritableDatabase().execSQL("UPDATE messages SET status = 2 WHERE id = ?",
                    new Object[]{id});
        }
    }

    private static void createMessagesTable(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE messages ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "contact_id INTEGER NOT NULL REFERENCES contacts(id) ON DELETE CASCADE, "
                + "body TEXT NOT NULL, "
                + "direction INTEGER NOT NULL, "
                + "timestamp INTEGER NOT NULL, "
                + "status INTEGER NOT NULL DEFAULT 1)");
        database.execSQL("CREATE INDEX messages_contact_time "
                + "ON messages(contact_id, timestamp, id)");
    }

    private static ContentValues values(String name, String phone, String email,
                                        String address, String notes, String photoUri) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("phone", phone);
        values.put("email", email);
        values.put("address", address);
        values.put("notes", notes);
        values.put("photo_uri", photoUri);
        return values;
    }

    private static Contact readContact(Cursor cursor) {
        return new Contact(cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                cursor.getString(3), cursor.getString(4), cursor.getString(5),
                cursor.getString(6));
    }
}
