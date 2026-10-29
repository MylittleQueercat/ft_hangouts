package com.hguo.fthangouts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.SmsManager;

import java.text.DateFormat;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int LIST = 0;
    private static final int DETAILS = 1;
    private static final int FORM = 2;
    private static final int CONVERSATION = 3;
    private static final int SMS_PERMISSION_REQUEST = 42;
    private static final int CALL_PERMISSION_REQUEST = 43;
    private static final int PHOTO_REQUEST = 44;
    private static final int[] HEADER_COLORS = {
            Color.WHITE, Color.rgb(231, 239, 232),
            Color.rgb(244, 239, 228), Color.rgb(231, 234, 238)
    };
    private static final int[] HEADER_COLOR_LABELS = {
            R.string.color_white, R.string.color_sage,
            R.string.color_sand, R.string.color_slate
    };
    private static final String PREF_HEADER_COLOR = "header_color_v2";
    private static final String PREF_BACKGROUNDED_AT = "backgrounded_at";
    private static final int PAGE_COLOR = Color.WHITE;
    private static final int INK_COLOR = Color.rgb(44, 51, 46);
    private static final int MUTED_COLOR = Color.rgb(119, 128, 121);
    private static final int CARD_COLOR = Color.rgb(247, 248, 247);
    private static final int BORDER_COLOR = Color.rgb(220, 224, 220);
    private static final int ACTION_COLOR = Color.rgb(12, 23, 16);

    private ContactDatabase database;
    private int headerColor;
    private LinearLayout headerView;
    private int screen = LIST;
    private long selectedId = -1;
    private EditText nameField;
    private EditText phoneField;
    private EditText emailField;
    private EditText addressField;
    private EditText notesField;
    private EditText messageField;
    private String photoUri = "";
    private ImageView photoPreview;
    private Button removePhotoButton;
    private final BroadcastReceiver messageChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (screen == LIST) {
                renderList();
            } else if (screen == CONVERSATION
                    && intent.getLongExtra(SmsEvents.EXTRA_CONTACT_ID, -1) == selectedId) {
                renderConversation();
            }
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        database = new ContactDatabase(this);
        headerColor = getPreferences(MODE_PRIVATE).getInt(PREF_HEADER_COLOR, HEADER_COLORS[0]);
        if (state != null) {
            screen = state.getInt("screen", LIST);
            selectedId = state.getLong("selectedId", -1);
            photoUri = state.getString("photoUri", "");
        }
        render();
        if (state != null && screen == FORM) {
            nameField.setText(state.getString("name", ""));
            phoneField.setText(state.getString("phone", ""));
            emailField.setText(state.getString("email", ""));
            addressField.setText(state.getString("address", ""));
            notesField.setText(state.getString("notes", ""));
        } else if (state != null && screen == CONVERSATION) {
            messageField.setText(state.getString("draft", ""));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        long backgroundedAt = getPreferences(MODE_PRIVATE).getLong(PREF_BACKGROUNDED_AT, 0);
        if (backgroundedAt != 0) {
            getPreferences(MODE_PRIVATE).edit().remove(PREF_BACKGROUNDED_AT).apply();
            String time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(new java.util.Date(backgroundedAt));
            Toast.makeText(this, getString(R.string.last_backgrounded_at, time),
                    Toast.LENGTH_LONG).show();
        }
        IntentFilter filter = new IntentFilter(SmsEvents.ACTION_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(messageChangedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(messageChangedReceiver, filter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (screen == LIST) {
            renderList();
        } else if (screen == CONVERSATION) {
            renderConversation();
        }
    }

    @Override
    protected void onStop() {
        unregisterReceiver(messageChangedReceiver);
        if (!isChangingConfigurations()) {
            getPreferences(MODE_PRIVATE).edit()
                    .putLong(PREF_BACKGROUNDED_AT, System.currentTimeMillis()).apply();
        }
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == SMS_PERMISSION_REQUEST && screen == CONVERSATION) {
            renderConversation();
            if (!hasPermission(Manifest.permission.SEND_SMS)
                    || !hasPermission(Manifest.permission.RECEIVE_SMS)) {
                Toast.makeText(this, R.string.sms_permissions_needed, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == CALL_PERMISSION_REQUEST && screen == DETAILS) {
            if (hasPermission(Manifest.permission.CALL_PHONE)) {
                Contact contact = database.getContact(selectedId);
                if (contact != null) {
                    placeCall(contact);
                }
            } else {
                Toast.makeText(this, R.string.call_permission_needed, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PHOTO_REQUEST || resultCode != RESULT_OK || data == null
                || data.getData() == null || screen != FORM) {
            return;
        }
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (showPhoto(photoPreview, uri.toString())) {
                photoUri = uri.toString();
                removePhotoButton.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, R.string.photo_unavailable, Toast.LENGTH_LONG).show();
            }
        } catch (SecurityException exception) {
            Toast.makeText(this, R.string.photo_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putInt("screen", screen);
        state.putLong("selectedId", selectedId);
        if (screen == FORM) {
            state.putString("photoUri", photoUri);
            state.putString("name", nameField.getText().toString());
            state.putString("phone", phoneField.getText().toString());
            state.putString("email", emailField.getText().toString());
            state.putString("address", addressField.getText().toString());
            state.putString("notes", notesField.getText().toString());
        } else if (screen == CONVERSATION) {
            state.putString("draft", messageField.getText().toString());
        }
        super.onSaveInstanceState(state);
    }

    @Override
    public void onBackPressed() {
        if (screen == LIST) {
            super.onBackPressed();
        } else if ((screen == FORM || screen == CONVERSATION) && selectedId >= 0) {
            showDetails(selectedId);
        } else {
            showList();
        }
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }

    private void render() {
        if (screen == DETAILS) {
            renderDetails();
        } else if (screen == FORM) {
            renderForm();
        } else if (screen == CONVERSATION) {
            renderConversation();
        } else {
            renderList();
        }
    }

    private void showList() {
        screen = LIST;
        selectedId = -1;
        renderList();
    }

    private void showDetails(long id) {
        selectedId = id;
        screen = DETAILS;
        renderDetails();
    }

    private void showForm(long id) {
        selectedId = id;
        Contact contact = id >= 0 ? database.getContact(id) : null;
        photoUri = contact == null ? "" : contact.photoUri;
        screen = FORM;
        renderForm();
    }

    private void showConversation(long id) {
        selectedId = id;
        screen = CONVERSATION;
        messageField = null;
        renderConversation();
        if (!hasPermission(Manifest.permission.SEND_SMS)
                || !hasPermission(Manifest.permission.RECEIVE_SMS)) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS,
                    Manifest.permission.RECEIVE_SMS}, SMS_PERMISSION_REQUEST);
        }
    }

    private void renderList() {
        LinearLayout page = page(R.string.contacts, false);
        Button add = button(R.string.add_contact);
        add.setOnClickListener(view -> showForm(-1));

        java.util.List<Contact> contacts = database.listContacts();
        if (contacts.isEmpty()) {
            TextView empty = text(R.string.no_contacts, 16, MUTED_COLOR);
            empty.setPadding(0, dp(36), 0, 0);
            page.addView(empty);
        }
        for (Contact contact : contacts) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(18), dp(16), dp(18));
            row.setBackground(rounded(CARD_COLOR, 18));
            View avatar = avatar(contact, dp(54));
            LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(54), dp(54));
            avatarParams.rightMargin = dp(14);
            row.addView(avatar, avatarParams);
            LinearLayout summary = new LinearLayout(this);
            summary.setOrientation(LinearLayout.VERTICAL);
            TextView name = text(contact.name, 18, INK_COLOR);
            name.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            summary.addView(name);
            summary.addView(text(contact.phone, 15, MUTED_COLOR));
            row.addView(summary, new LinearLayout.LayoutParams(0, -2, 1));
            TextView arrow = text("›", 26, MUTED_COLOR);
            row.addView(arrow);
            row.setOnClickListener(view -> showDetails(contact.id));
            page.addView(row, blockParams(dp(10)));
        }
        addFooterAction(page, add);
    }

    private void renderDetails() {
        Contact contact = database.getContact(selectedId);
        if (contact == null) {
            showList();
            return;
        }
        LinearLayout page = page(R.string.contact_details, true);
        LinearLayout hero = new LinearLayout(this);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(rounded(CARD_COLOR, 18));
        View avatar = avatar(contact, dp(80));
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(80), dp(80));
        avatarParams.rightMargin = dp(18);
        hero.addView(avatar, avatarParams);
        LinearLayout identity = new LinearLayout(this);
        identity.setOrientation(LinearLayout.VERTICAL);
        TextView name = text(contact.name, 23, INK_COLOR);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        identity.addView(name);
        identity.addView(text(contact.phone, 16, MUTED_COLOR));
        hero.addView(identity);
        page.addView(hero, blockParams(dp(4)));

        LinearLayout information = new LinearLayout(this);
        information.setOrientation(LinearLayout.VERTICAL);
        information.setPadding(dp(18), dp(4), dp(18), dp(20));
        information.setBackground(bordered(Color.WHITE, 18));
        detail(information, R.string.email, contact.email);
        detail(information, R.string.address, contact.address);
        detail(information, R.string.notes, contact.notes);
        page.addView(information, blockParams(dp(16)));

        Button messages = button(R.string.messages);
        messages.setOnClickListener(view -> showConversation(contact.id));
        page.addView(messages, blockParams(dp(16)));

        Button call = secondaryButton(R.string.call_contact);
        call.setOnClickListener(view -> callContact(contact));
        page.addView(call, blockParams(dp(12)));

        Button edit = secondaryButton(R.string.edit_contact);
        edit.setOnClickListener(view -> showForm(contact.id));
        page.addView(edit, blockParams(dp(16)));

        Button delete = destructiveButton(R.string.delete_contact);
        delete.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle(R.string.delete_contact)
                .setMessage(getString(R.string.delete_confirmation, contact.name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    database.deleteContact(contact.id);
                    showList();
                })
                .show());
        page.addView(delete, blockParams(dp(12)));
    }

    private void renderForm() {
        Contact contact = selectedId >= 0 ? database.getContact(selectedId) : null;
        if (selectedId >= 0 && contact == null) {
            showList();
            return;
        }
        LinearLayout page = page(contact == null ? R.string.add_contact : R.string.edit_contact, true);
        photoPreview = photoView(dp(112));
        showPhoto(photoPreview, photoUri);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dp(112), dp(112));
        previewParams.topMargin = dp(4);
        page.addView(photoPreview, previewParams);
        Button choosePhoto = secondaryButton(R.string.choose_photo);
        choosePhoto.setOnClickListener(view -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            pick.setType("image/*");
            pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            try {
                startActivityForResult(pick, PHOTO_REQUEST);
            } catch (ActivityNotFoundException exception) {
                Toast.makeText(this, R.string.photo_unavailable, Toast.LENGTH_LONG).show();
            }
        });
        page.addView(choosePhoto, blockParams(dp(12)));
        removePhotoButton = secondaryButton(R.string.remove_photo);
        removePhotoButton.setVisibility(photoUri.isEmpty() ? View.GONE : View.VISIBLE);
        removePhotoButton.setOnClickListener(view -> {
            photoUri = "";
            photoPreview.setImageDrawable(null);
            photoPreview.setVisibility(View.GONE);
            removePhotoButton.setVisibility(View.GONE);
        });
        page.addView(removePhotoButton, blockParams(dp(8)));
        nameField = field(page, R.string.name, contact == null ? "" : contact.name,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        phoneField = field(page, R.string.phone, contact == null ? "" : contact.phone,
                InputType.TYPE_CLASS_PHONE);
        emailField = field(page, R.string.email, contact == null ? "" : contact.email,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        addressField = field(page, R.string.address, contact == null ? "" : contact.address,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        notesField = field(page, R.string.notes, contact == null ? "" : contact.notes,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        Button save = button(R.string.save);
        save.setOnClickListener(view -> saveContact());
        addFooterAction(page, save);
    }

    private void saveContact() {
        String name = nameField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();
        if (name.isEmpty()) {
            nameField.setError(getString(R.string.required));
            nameField.requestFocus();
            return;
        }
        if (phone.isEmpty()) {
            phoneField.setError(getString(R.string.required));
            phoneField.requestFocus();
            return;
        }
        String email = emailField.getText().toString().trim();
        String address = addressField.getText().toString().trim();
        String notes = notesField.getText().toString().trim();
        if (selectedId < 0) {
            selectedId = database.createContact(name, phone, email, address, notes, photoUri);
        } else if (!database.updateContact(selectedId, name, phone, email, address, notes,
                photoUri)) {
            Toast.makeText(this, R.string.contact_missing, Toast.LENGTH_SHORT).show();
            showList();
            return;
        }
        showDetails(selectedId);
    }

    private void renderConversation() {
        Contact contact = database.getContact(selectedId);
        if (contact == null) {
            showList();
            return;
        }
        String draft = messageField == null ? "" : messageField.getText().toString();
        LinearLayout page = page(R.string.conversation, true);
        TextView contactName = text(contact.name, 21, INK_COLOR);
        contactName.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        page.addView(contactName);
        page.addView(text(contact.phone, 14, MUTED_COLOR));

        if (!hasPermission(Manifest.permission.RECEIVE_SMS)) {
            TextView permissionNotice = text(R.string.receive_permission_needed, 14, Color.RED);
            page.addView(permissionNotice, blockParams(dp(12)));
        }

        java.util.List<Message> messages = database.listMessages(contact.id);
        if (messages.isEmpty()) {
            page.addView(text(R.string.no_messages, 15, Color.DKGRAY), blockParams(dp(24)));
        }
        for (Message message : messages) {
            LinearLayout bubble = new LinearLayout(this);
            bubble.setOrientation(LinearLayout.VERTICAL);
            bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
            bubble.setBackground(rounded(message.direction == Message.SENT
                    ? Color.rgb(232, 242, 233) : CARD_COLOR, 16));
            String who = message.direction == Message.SENT
                    ? getString(R.string.you_to_contact, contact.name)
                    : getString(R.string.contact_to_you, contact.name);
            TextView sender = text(who, 13, INK_COLOR);
            sender.setTypeface(null, 1);
            bubble.addView(sender);
            bubble.addView(text(message.body, 17, INK_COLOR));
            String time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(new java.util.Date(message.timestamp));
            if (message.direction == Message.SENT) {
                int status = message.status == Message.FAILED ? R.string.failed
                        : message.status == Message.PENDING ? R.string.sending : R.string.sent;
                time += " · " + getString(status);
            }
            bubble.addView(text(time, 12, MUTED_COLOR));
            int width = Math.min((int) (getResources().getDisplayMetrics().widthPixels * 0.82),
                    dp(520));
            LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(width, -2);
            bubbleParams.topMargin = dp(12);
            bubbleParams.gravity = message.direction == Message.SENT
                    ? Gravity.END : Gravity.START;
            page.addView(bubble, bubbleParams);
        }

        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(8), dp(6), dp(8), dp(6));
        composer.setBackground(bordered(Color.WHITE, 16));
        messageField = new EditText(this);
        messageField.setHint(R.string.message_hint);
        messageField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        messageField.setMinLines(2);
        messageField.setMaxLines(4);
        messageField.setText(draft);
        messageField.setBackgroundColor(Color.TRANSPARENT);
        composer.addView(messageField, new LinearLayout.LayoutParams(0, -2, 1));
        Button send = button(R.string.send);
        send.setOnClickListener(view -> sendMessage(contact));
        composer.addView(send);
        addFooterContent(page, composer);
        ScrollView scroll = (ScrollView) page.getParent();
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void sendMessage(Contact contact) {
        String body = messageField.getText().toString().trim();
        if (body.isEmpty()) {
            messageField.setError(getString(R.string.required));
            return;
        }
        if (!hasPermission(Manifest.permission.SEND_SMS)) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST);
            return;
        }
        long id = database.addMessage(contact.id, body, Message.SENT,
                System.currentTimeMillis(), Message.PENDING);
        try {
            SmsManager manager = SmsManager.getDefault();
            ArrayList<String> parts = manager.divideMessage(body);
            ArrayList<PendingIntent> callbacks = new ArrayList<>();
            for (int i = 0; i < parts.size(); i++) {
                Intent result = new Intent(this, SmsSentReceiver.class);
                result.putExtra(SmsEvents.EXTRA_MESSAGE_ID, id);
                result.putExtra(SmsEvents.EXTRA_CONTACT_ID, contact.id);
                callbacks.add(PendingIntent.getBroadcast(this, (int) (id * 100 + i), result,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            }
            if (parts.size() == 1) {
                manager.sendTextMessage(contact.phone, null, body, callbacks.get(0), null);
            } else {
                manager.sendMultipartTextMessage(contact.phone, null, parts, callbacks, null);
            }
            messageField.setText("");
        } catch (RuntimeException exception) {
            database.markMessageSent(id, false);
            Toast.makeText(this, R.string.send_failed, Toast.LENGTH_LONG).show();
        }
        renderConversation();
    }

    private boolean hasPermission(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void callContact(Contact contact) {
        if (!hasPermission(Manifest.permission.CALL_PHONE)) {
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, CALL_PERMISSION_REQUEST);
            return;
        }
        placeCall(contact);
    }

    private void placeCall(Contact contact) {
        Intent call = new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", contact.phone, null));
        try {
            startActivity(call);
        } catch (ActivityNotFoundException | SecurityException exception) {
            Toast.makeText(this, R.string.call_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private ImageView photoView(int size) {
        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(rounded(CARD_COLOR, 20));
        image.setClipToOutline(true);
        image.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        image.setContentDescription(getString(R.string.contact_photo));
        return image;
    }

    private View avatar(Contact contact, int size) {
        if (!contact.photoUri.isEmpty()) {
            ImageView image = photoView(size);
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(CARD_COLOR);
            image.setBackground(shape);
            if (showPhoto(image, contact.photoUri)) {
                return image;
            }
        }
        String initial = contact.name.isEmpty() ? "?" : contact.name.substring(0,
                contact.name.offsetByCodePoints(0, 1)).toUpperCase(Locale.getDefault());
        TextView label = text(initial, size >= dp(70) ? 30 : 22, INK_COLOR);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        label.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.WHITE);
        label.setBackground(circle);
        return label;
    }

    private boolean showPhoto(ImageView image, String uriText) {
        if (uriText == null || uriText.isEmpty()) {
            image.setImageDrawable(null);
            image.setVisibility(View.GONE);
            return false;
        }
        Uri uri = Uri.parse(uriText);
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream stream = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(stream, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                image.setVisibility(View.GONE);
                return false;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            while (bounds.outWidth / options.inSampleSize > 512
                    || bounds.outHeight / options.inSampleSize > 512) {
                options.inSampleSize *= 2;
            }
            Bitmap bitmap;
            try (InputStream stream = getContentResolver().openInputStream(uri)) {
                bitmap = BitmapFactory.decodeStream(stream, null, options);
            }
            if (bitmap == null) {
                image.setVisibility(View.GONE);
                return false;
            }
            image.setImageBitmap(bitmap);
            image.setVisibility(View.VISIBLE);
            return true;
        } catch (IOException | SecurityException exception) {
            image.setVisibility(View.GONE);
            return false;
        }
    }

    private LinearLayout page(int title, boolean back) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(PAGE_COLOR);
        setContentView(outer);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(10), dp(20), dp(12));
        header.setBackgroundColor(headerColor);
        headerView = header;

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        if (back) {
            TextView backButton = text("‹", 30, INK_COLOR);
            backButton.setContentDescription(getString(R.string.back));
            backButton.setGravity(Gravity.CENTER);
            backButton.setBackground(rounded(CARD_COLOR, 100));
            backButton.setOnClickListener(view -> onBackPressed());
            toolbar.addView(backButton, new LinearLayout.LayoutParams(dp(44), dp(44)));
        } else {
            ImageView logo = new ImageView(this);
            logo.setImageResource(R.drawable.ic_launcher);
            logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
            logo.setBackground(rounded(ACTION_COLOR, 100));
            logo.setClipToOutline(true);
            logo.setContentDescription(getString(R.string.app_name));
            toolbar.addView(logo, new LinearLayout.LayoutParams(dp(44), dp(44)));
        }
        toolbar.addView(new View(this), new LinearLayout.LayoutParams(0, dp(1), 1));
        TextView menuButton = text("⋮", 26, INK_COLOR);
        menuButton.setContentDescription(getString(R.string.change_header_color));
        menuButton.setGravity(Gravity.CENTER);
        menuButton.setBackground(rounded(CARD_COLOR, 100));
        menuButton.setOnClickListener(view -> showHeaderColorMenu(menuButton));
        toolbar.addView(menuButton, new LinearLayout.LayoutParams(dp(44), dp(44)));
        header.addView(toolbar);

        TextView heading = text(title, 28, INK_COLOR);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        header.addView(heading, blockParams(dp(24)));
        TextView subtitle = text(subtitleForScreen(), 15, MUTED_COLOR);
        header.addView(subtitle, blockParams(dp(4)));
        outer.addView(header, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        outer.setOnApplyWindowInsetsListener((view, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            header.setPadding(dp(20), top + dp(10), dp(20), dp(12));
            outer.setPadding(0, 0, 0, insets.getStableInsetBottom());
            return insets;
        });
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(10), dp(20), dp(24));
        scroll.addView(content);
        outer.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        return content;
    }

    private int subtitleForScreen() {
        if (screen == LIST) {
            return R.string.contacts_subtitle;
        }
        if (screen == FORM) {
            return selectedId < 0 ? R.string.add_contact_subtitle : R.string.edit_contact_subtitle;
        }
        if (screen == CONVERSATION) {
            return R.string.conversation_subtitle;
        }
        return R.string.contact_details_subtitle;
    }

    private void addFooterAction(LinearLayout page, Button action) {
        action.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(54)));
        addFooterContent(page, action);
    }

    private void addFooterContent(LinearLayout page, View content) {
        ScrollView scroll = (ScrollView) page.getParent();
        LinearLayout outer = (LinearLayout) scroll.getParent();
        LinearLayout footer = new LinearLayout(this);
        footer.setPadding(dp(20), dp(12), dp(20), dp(14));
        footer.setBackgroundColor(Color.WHITE);
        footer.setElevation(dp(6));
        if (content.getLayoutParams() == null) {
            content.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        }
        footer.addView(content);
        outer.addView(footer, new LinearLayout.LayoutParams(-1, -2));
    }

    private void showHeaderColorMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        for (int i = 0; i < HEADER_COLORS.length; i++) {
            popup.getMenu().add(0, i, i, HEADER_COLOR_LABELS[i]);
        }
        popup.setOnMenuItemClickListener(item -> {
            headerColor = HEADER_COLORS[item.getItemId()];
            getPreferences(MODE_PRIVATE).edit().putInt(PREF_HEADER_COLOR, headerColor).apply();
            headerView.setBackgroundColor(headerColor);
            return true;
        });
        popup.show();
    }

    private void detail(LinearLayout page, int label, String value) {
        TextView heading = text(label, 13, INK_COLOR);
        page.addView(heading, blockParams(dp(14)));
        TextView body = text(value.isEmpty() ? getString(R.string.not_provided) : value,
                18, INK_COLOR);
        page.addView(body);
    }

    private EditText field(LinearLayout page, int label, String value, int inputType) {
        TextView heading = text(label, 14, INK_COLOR);
        page.addView(heading, blockParams(dp(12)));
        EditText input = new EditText(this);
        input.setSingleLine((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        input.setInputType(inputType);
        input.setHint(label);
        input.setText(value);
        input.setTextSize(17);
        input.setTextColor(INK_COLOR);
        input.setBackground(bordered(Color.WHITE, 12));
        input.setPadding(dp(14), dp(10), dp(14), dp(10));
        input.setMinHeight(dp(54));
        page.addView(input, new LinearLayout.LayoutParams(-1, -2));
        return input;
    }

    private Button button(int label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setMinHeight(dp(52));
        button.setBackground(rounded(ACTION_COLOR, 100));
        button.setStateListAnimator(null);
        button.setElevation(0);
        return button;
    }

    private Button secondaryButton(int label) {
        Button button = button(label);
        button.setBackground(bordered(Color.WHITE, 100));
        button.setTextColor(INK_COLOR);
        return button;
    }

    private Button destructiveButton(int label) {
        Button button = button(label);
        button.setBackground(rounded(Color.rgb(250, 238, 238), 100));
        button.setTextColor(Color.rgb(160, 42, 54));
        return button;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private GradientDrawable bordered(int color, int radius) {
        GradientDrawable shape = rounded(color, radius);
        shape.setStroke(dp(1), BORDER_COLOR);
        return shape;
    }

    private TextView text(int label, int size, int color) {
        return text(getString(label), size, color);
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout.LayoutParams blockParams(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = topMargin;
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
