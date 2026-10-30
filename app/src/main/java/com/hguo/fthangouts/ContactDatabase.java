// This helper creates and updates the local SQLite database.
// It also reads and writes contacts and text messages.
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
    private static final int DATABASE_VERSION = 5;
    private static final String[] COLUMNS = {
            "id", "name", "phone", "email", "address", "notes", "photo_uri",
            "starred", "emergency", "pronouns", "birthday", "blocked"
    };

    ContactDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Enable contact-to-message foreign key rules.
    @Override
    public void onConfigure(SQLiteDatabase database) {
        database.setForeignKeyConstraintsEnabled(true);
    }

    // Create the contact and message tables for a new install.
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE contacts ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "phone TEXT NOT NULL, "
                + "email TEXT NOT NULL DEFAULT '', "
                + "address TEXT NOT NULL DEFAULT '', "
                + "notes TEXT NOT NULL DEFAULT '', "
                + "photo_uri TEXT NOT NULL DEFAULT '', "
                + "starred INTEGER NOT NULL DEFAULT 0, "
                + "emergency INTEGER NOT NULL DEFAULT 0, "
                + "pronouns TEXT NOT NULL DEFAULT '', "
                + "birthday TEXT NOT NULL DEFAULT '', "
                + "blocked INTEGER NOT NULL DEFAULT 0)");
        createMessagesTable(database);
    }

    // Add new columns when the database version changes.
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createMessagesTable(database);
        }
        if (oldVersion < 3) {
            database.execSQL("ALTER TABLE contacts ADD COLUMN photo_uri TEXT NOT NULL DEFAULT ''");
        }
        if (oldVersion < 4) {
            database.execSQL("ALTER TABLE contacts ADD COLUMN starred INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE contacts ADD COLUMN emergency INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 5) {
            database.execSQL("ALTER TABLE contacts ADD COLUMN pronouns TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE contacts ADD COLUMN birthday TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE contacts ADD COLUMN blocked INTEGER NOT NULL DEFAULT 0");
        }
    }

    List<Contact> listContacts() {
        List<Contact> contacts = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("contacts", COLUMNS,
                null, null, null, null, "starred DESC, name COLLATE NOCASE ASC, id ASC")) {
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
                       String photoUri, String pronouns, String birthday) {
        return getWritableDatabase().insertOrThrow("contacts", null,
                values(name, phone, email, address, notes, photoUri, pronouns, birthday));
    }

    boolean updateContact(long id, String name, String phone, String email,
                          String address, String notes, String photoUri, String pronouns,
                          String birthday) {
        return getWritableDatabase().update("contacts",
                values(name, phone, email, address, notes, photoUri, pronouns, birthday),
                "id = ?", new String[]{String.valueOf(id)}) == 1;
    }

    boolean deleteContact(long id) {
        return getWritableDatabase().delete("contacts", "id = ?",
                new String[]{String.valueOf(id)}) == 1;
    }

    boolean setStarred(long id, boolean starred) {
        ContentValues values = new ContentValues();
        values.put("starred", starred ? 1 : 0);
        return getWritableDatabase().update("contacts", values, "id = ?",
                new String[]{String.valueOf(id)}) == 1;
    }

    boolean setEmergency(long id, boolean emergency) {
        ContentValues values = new ContentValues();
        values.put("emergency", emergency ? 1 : 0);
        return getWritableDatabase().update("contacts", values, "id = ?",
                new String[]{String.valueOf(id)}) == 1;
    }

    boolean setBlocked(long id, boolean blocked) {
        ContentValues values = new ContentValues();
        values.put("blocked", blocked ? 1 : 0);
        return getWritableDatabase().update("contacts", values, "id = ?",
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

    Message latestMessage(long contactId) {
        try (Cursor cursor = getReadableDatabase().query("messages",
                new String[]{"id", "contact_id", "body", "direction", "timestamp", "status"},
                "contact_id = ?", new String[]{String.valueOf(contactId)}, null, null,
                "timestamp DESC, id DESC", "1")) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new Message(cursor.getLong(0), cursor.getLong(1), cursor.getString(2),
                    cursor.getInt(3), cursor.getLong(4), cursor.getInt(5));
        }
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
                                        String address, String notes, String photoUri,
                                        String pronouns, String birthday) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("phone", phone);
        values.put("email", email);
        values.put("address", address);
        values.put("notes", notes);
        values.put("photo_uri", photoUri);
        values.put("pronouns", pronouns);
        values.put("birthday", birthday);
        return values;
    }

    private static Contact readContact(Cursor cursor) {
        return new Contact(cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                cursor.getString(3), cursor.getString(4), cursor.getString(5),
                cursor.getString(6), cursor.getString(9), cursor.getString(10),
                cursor.getInt(7) != 0, cursor.getInt(8) != 0, cursor.getInt(11) != 0);
    }
}
