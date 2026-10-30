// This activity builds the screens and handles user actions.
// It also manages permissions, themes, photos, calls, and messages.
package com.hguo.fthangouts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
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
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.telephony.SmsManager;

import java.text.DateFormat;
import java.text.Collator;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MainActivity extends Activity {
    private static final int LIST = 0;
    private static final int DETAILS = 1;
    private static final int FORM = 2;
    private static final int CONVERSATION = 3;
    private static final int SMS_PERMISSION_REQUEST = 42;
    private static final int CALL_PERMISSION_REQUEST = 43;
    private static final int PHOTO_REQUEST = 44;
    private static final int[] HEADER_THEME_START = {
            Color.WHITE, Color.rgb(245, 243, 255),
            Color.rgb(255, 247, 237), Color.rgb(236, 254, 255)
    };
    private static final int[] HEADER_THEME_END = {
            Color.WHITE, Color.rgb(237, 233, 254),
            Color.rgb(252, 231, 243), Color.rgb(224, 242, 254)
    };
    private static final int[] HEADER_THEME_ACCENT = {
            Color.rgb(124, 58, 237), Color.rgb(109, 40, 217),
            Color.rgb(219, 39, 119), Color.rgb(8, 145, 178)
    };
    private static final int[] THEME_SECONDARY = {
            Color.rgb(236, 72, 153), Color.rgb(167, 139, 250),
            Color.rgb(249, 115, 22), Color.rgb(59, 130, 246)
    };
    private static final int[] THEME_CARD = {
            Color.rgb(245, 243, 255), Color.rgb(245, 243, 255),
            Color.rgb(255, 241, 242), Color.rgb(236, 254, 255)
    };
    private static final int[] THEME_SENT = {
            Color.rgb(237, 233, 254), Color.rgb(237, 233, 254),
            Color.rgb(252, 231, 243), Color.rgb(207, 250, 254)
    };
    private static final int[] HEADER_THEME_ICONS = {
            R.drawable.ic_sparkle, R.drawable.ic_theme_flower,
            R.drawable.ic_theme_sunrise, R.drawable.ic_theme_waves
    };
    private static final int[] HEADER_THEME_LABELS = {
            R.string.theme_classic, R.string.theme_lavender,
            R.string.theme_dawn, R.string.theme_ocean
    };
    private static final int[] HEADER_THEME_DESCRIPTIONS = {
            R.string.theme_classic_description, R.string.theme_lavender_description,
            R.string.theme_dawn_description, R.string.theme_ocean_description
    };
    private static final String PREF_HEADER_THEME = "header_theme_v1";
    private static final String PREF_HEADER_COLOR = "header_color_v5";
    private static final String PREF_BACKGROUNDED_AT = "backgrounded_at";
    private static final int PAGE_COLOR = Color.rgb(250, 250, 250);
    private static final int SURFACE_COLOR = Color.WHITE;
    private static final int INK_COLOR = Color.rgb(24, 24, 27);
    private static final int MUTED_COLOR = Color.rgb(113, 113, 122);
    private static final int BORDER_COLOR = Color.rgb(228, 228, 231);
    private static final int ERROR_COLOR = Color.rgb(220, 38, 38);
    private static final int ERROR_FILL = Color.rgb(254, 242, 242);
    private static final int RECEIVED_COLOR = Color.WHITE;

    private ContactDatabase database;
    private int headerTheme;
    private int actionColor = HEADER_THEME_ACCENT[0];
    private int accentColor = THEME_SECONDARY[0];
    private int cardColor = THEME_CARD[0];
    private int sentColor = THEME_SENT[0];
    private ScrollView contentScroll;
    private int screen = LIST;
    private long selectedId = -1;
    private EditText nameField;
    private EditText phoneField;
    private EditText emailField;
    private EditText addressField;
    private EditText notesField;
    private EditText pronounsField;
    private EditText birthdayField;
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
        getWindow().setNavigationBarColor(PAGE_COLOR);
        database = new ContactDatabase(this);
        headerTheme = getPreferences(MODE_PRIVATE).getInt(PREF_HEADER_THEME, -1);
        if (headerTheme < 0 || headerTheme >= HEADER_THEME_START.length) {
            int oldColor = getPreferences(MODE_PRIVATE).getInt(PREF_HEADER_COLOR, Color.WHITE);
            headerTheme = oldColor == Color.rgb(245, 243, 255) ? 1
                    : oldColor == Color.rgb(248, 247, 250) ? 2
                    : oldColor == Color.rgb(244, 244, 245) ? 3 : 0;
            getPreferences(MODE_PRIVATE).edit().putInt(PREF_HEADER_THEME, headerTheme).apply();
        }
        setThemePalette(headerTheme);
        getWindow().setStatusBarColor(HEADER_THEME_START[headerTheme]);
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
            pronounsField.setText(state.getString("pronouns", ""));
            birthdayField.setText(state.getString("birthday", ""));
        } else if (state != null && screen == CONVERSATION) {
            messageField.setText(state.getString("draft", ""));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Show the saved time when the app returns to the foreground.
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
        // Save the time when the app leaves the foreground.
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
                if (!showPhoto(photoPreview, photoUri)) {
                    showPhotoPlaceholder();
                }
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
            state.putString("pronouns", pronounsField.getText().toString());
            state.putString("birthday", birthdayField.getText().toString());
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

    // Select the screen that matches the current app state.
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
        Contact contact = database.getContact(id);
        if (contact != null && contact.blocked) {
            Toast.makeText(this, R.string.blocked_in_app, Toast.LENGTH_LONG).show();
            return;
        }
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

    // Read contacts from SQLite and build the contact list.
    private void renderList() {
        LinearLayout page = page(R.string.contacts, false);
        Button add = button(R.string.add_contact);
        withIcon(add, R.drawable.ic_add, Color.WHITE);
        add.setOnClickListener(view -> showForm(-1));

        java.util.List<Contact> contacts = database.listContacts();
        Collator collator = Collator.getInstance(Locale.getDefault());
        collator.setStrength(Collator.SECONDARY);
        contacts.sort((first, second) -> {
            if (first.starred != second.starred) {
                return first.starred ? -1 : 1;
            }
            char firstLetter = indexLetter(first.name);
            char secondLetter = indexLetter(second.name);
            if (firstLetter != secondLetter) {
                return Character.compare(firstLetter == '#' ? '[' : firstLetter,
                        secondLetter == '#' ? '[' : secondLetter);
            }
            int nameOrder = collator.compare(first.name, second.name);
            return nameOrder != 0 ? nameOrder : Long.compare(first.id, second.id);
        });
        Map<Character, View> firstForLetter = new HashMap<>();
        LinearLayout sectionRow = new LinearLayout(this);
        sectionRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView sectionTitle = text(R.string.your_circle, 12, MUTED_COLOR);
        sectionTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        sectionTitle.setLetterSpacing(0.11f);
        sectionRow.addView(sectionTitle, new LinearLayout.LayoutParams(0, -2, 1));
        TextView section = text(getResources().getQuantityString(R.plurals.contact_count,
                contacts.size(), contacts.size()), 11, actionColor);
        section.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        section.setLetterSpacing(0.08f);
        section.setPadding(dp(11), dp(7), dp(11), dp(7));
        section.setBackground(bordered(SURFACE_COLOR, 100));
        sectionRow.addView(section);
        page.addView(sectionRow, blockParams(dp(14)));
        if (contacts.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24), dp(42), dp(24), dp(42));
            empty.setBackground(bordered(SURFACE_COLOR, 12));
            TextView mark = text("+", 32, actionColor);
            mark.setGravity(Gravity.CENTER);
            mark.setBackground(rounded(cardColor, 100));
            empty.addView(mark, new LinearLayout.LayoutParams(dp(58), dp(58)));
            TextView message = text(R.string.no_contacts, 17, INK_COLOR);
            message.setGravity(Gravity.CENTER);
            message.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            empty.addView(message, blockParams(dp(18)));
            page.addView(empty, blockParams(dp(14)));
        }
        boolean pinnedHeaderShown = false;
        char currentLetter = '\0';
        for (Contact contact : contacts) {
            char letter = indexLetter(contact.name);
            if (contact.starred && !pinnedHeaderShown) {
                page.addView(sectionLabel(R.string.pinned_favorites), blockParams(dp(20)));
                pinnedHeaderShown = true;
            } else if (!contact.starred && letter != currentLetter) {
                TextView letterHeading = text(String.valueOf(letter), 12, actionColor);
                letterHeading.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                page.addView(letterHeading, blockParams(dp(18)));
            }
            if (!contact.starred) {
                currentLetter = letter;
            }
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(15), dp(14), dp(15), dp(14));
            row.setBackground(bordered(SURFACE_COLOR, 12));
            row.setElevation(dp(1));
            LinearLayout topLine = new LinearLayout(this);
            topLine.setGravity(Gravity.CENTER_VERTICAL);
            View avatar = avatar(contact, dp(50));
            LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(50), dp(50));
            avatarParams.rightMargin = dp(14);
            topLine.addView(avatar, avatarParams);
            LinearLayout summary = new LinearLayout(this);
            summary.setOrientation(LinearLayout.VERTICAL);
            TextView name = text(contact.name, 17, INK_COLOR);
            name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            summary.addView(name);
            LinearLayout phoneLine = new LinearLayout(this);
            phoneLine.setGravity(Gravity.CENTER_VERTICAL);
            ImageView phoneIcon = new ImageView(this);
            phoneIcon.setImageResource(R.drawable.ic_phone);
            phoneIcon.setImageTintList(ColorStateList.valueOf(MUTED_COLOR));
            LinearLayout.LayoutParams phoneIconParams =
                    new LinearLayout.LayoutParams(dp(13), dp(13));
            phoneIconParams.rightMargin = dp(5);
            phoneLine.addView(phoneIcon, phoneIconParams);
            phoneLine.addView(text(contact.phone, 14, MUTED_COLOR));
            summary.addView(phoneLine);
            topLine.addView(summary, new LinearLayout.LayoutParams(0, -2, 1));
            ImageView arrow = iconView(R.drawable.ic_chevron, actionColor);
            arrow.setPadding(dp(8), dp(8), dp(8), dp(8));
            arrow.setBackground(rounded(cardColor, 100));
            LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dp(30), dp(30));
            arrowParams.leftMargin = dp(8);
            topLine.addView(arrow, arrowParams);
            row.addView(topLine);
            if (contact.starred || contact.emergency || contact.blocked) {
                LinearLayout badges = new LinearLayout(this);
                badges.setOrientation(LinearLayout.HORIZONTAL);
                if (contact.starred) {
                    badges.addView(statusBadge(R.drawable.ic_star_filled,
                            R.string.starred_badge, actionColor, cardColor));
                }
                if (contact.emergency) {
                    LinearLayout.LayoutParams badgeParams =
                            new LinearLayout.LayoutParams(-2, -2);
                    if (contact.starred) {
                        badgeParams.leftMargin = dp(8);
                    }
                    badges.addView(statusBadge(R.drawable.ic_emergency,
                            R.string.emergency_badge, ERROR_COLOR, ERROR_FILL), badgeParams);
                }
                if (contact.blocked) {
                    LinearLayout.LayoutParams badgeParams =
                            new LinearLayout.LayoutParams(-2, -2);
                    if (contact.starred || contact.emergency) {
                        badgeParams.leftMargin = dp(8);
                    }
                    badges.addView(statusBadge(R.drawable.ic_block,
                            R.string.blocked_badge, MUTED_COLOR, PAGE_COLOR), badgeParams);
                }
                row.addView(badges, blockParams(dp(10)));
            }
            View divider = new View(this);
            divider.setBackgroundColor(BORDER_COLOR);
            LinearLayout.LayoutParams dividerParams =
                    new LinearLayout.LayoutParams(-1, dp(1));
            dividerParams.topMargin = dp(12);
            row.addView(divider, dividerParams);
            LinearLayout preview = new LinearLayout(this);
            preview.setGravity(Gravity.CENTER_VERTICAL);
            ImageView previewIcon = iconView(R.drawable.ic_message, MUTED_COLOR);
            LinearLayout.LayoutParams previewIconParams =
                    new LinearLayout.LayoutParams(dp(16), dp(16));
            previewIconParams.rightMargin = dp(8);
            preview.addView(previewIcon, previewIconParams);
            Message latest = database.latestMessage(contact.id);
            String previewBody = latest == null ? getString(R.string.no_messages)
                    : latest.direction == Message.SENT
                    ? getString(R.string.you_preview, latest.body) : latest.body;
            TextView previewText = text(previewBody, 13, MUTED_COLOR);
            previewText.setSingleLine(true);
            previewText.setEllipsize(TextUtils.TruncateAt.END);
            preview.addView(previewText, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(preview, blockParams(dp(10)));
            row.setOnClickListener(view -> showDetails(contact.id));
            page.addView(row, blockParams(dp(12)));
            firstForLetter.putIfAbsent(letter, row);
        }
        addAlphabetSidebar(page, firstForLetter);
        addFooterAction(page, add);
    }

    private char indexLetter(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return '#';
        }
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
        int first = Character.toUpperCase(normalized.codePointAt(0));
        return first >= 'A' && first <= 'Z' ? (char) first : '#';
    }

    private void addAlphabetSidebar(LinearLayout page, Map<Character, View> firstForLetter) {
        ScrollView listScroll = (ScrollView) page.getParent();
        FrameLayout frame = (FrameLayout) listScroll.getParent();
        ScrollView indexScroll = new ScrollView(this);
        indexScroll.setFillViewport(true);
        indexScroll.setVerticalScrollBarEnabled(false);
        LinearLayout letters = new LinearLayout(this);
        letters.setOrientation(LinearLayout.VERTICAL);
        letters.setGravity(Gravity.CENTER_VERTICAL);
        for (char letter = 'A'; letter <= 'Z' + 1; letter++) {
            char current = letter == 'Z' + 1 ? '#' : letter;
            View destination = firstForLetter.get(current);
            TextView item = text(String.valueOf(current), 11,
                    destination == null ? Color.rgb(190, 190, 198) : actionColor);
            item.setGravity(Gravity.CENTER);
            if (destination != null) {
                item.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                item.setContentDescription(getString(R.string.jump_to_letter, current));
                item.setOnClickListener(view -> listScroll.smoothScrollTo(0,
                        Math.max(0, destination.getTop() - dp(8))));
            }
            letters.addView(item, new LinearLayout.LayoutParams(dp(32), dp(18)));
        }
        indexScroll.addView(letters);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(34), -1, Gravity.END);
        params.rightMargin = dp(2);
        params.topMargin = dp(12);
        params.bottomMargin = dp(12);
        frame.addView(indexScroll, params);
    }

    // Show one contact and its available actions.
    private void renderDetails() {
        Contact contact = database.getContact(selectedId);
        if (contact == null) {
            showList();
            return;
        }
        LinearLayout page = page(R.string.contact_details, true);
        LinearLayout hero = new LinearLayout(this);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(20), dp(22), dp(20), dp(22));
        hero.setBackground(rounded(cardColor, 12));
        View avatar = avatar(contact, dp(80));
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(80), dp(80));
        avatarParams.rightMargin = dp(18);
        hero.addView(avatar, avatarParams);
        LinearLayout identity = new LinearLayout(this);
        identity.setOrientation(LinearLayout.VERTICAL);
        TextView name = text(contact.name, 23, INK_COLOR);
        name.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        identity.addView(name);
        identity.addView(text(contact.phone, 16, MUTED_COLOR));
        hero.addView(identity);
        page.addView(hero, blockParams(dp(12)));

        page.addView(sectionLabel(R.string.contact_priority), blockParams(dp(20)));
        LinearLayout priorityActions = new LinearLayout(this);
        priorityActions.setOrientation(LinearLayout.HORIZONTAL);
        Button star = secondaryButton(contact.starred ? R.string.pinned_short : R.string.pin_short);
        star.setTextSize(13);
        star.setSelected(contact.starred);
        if (contact.starred) {
            star.setBackground(rounded(cardColor, 12));
        }
        withIcon(star, contact.starred ? R.drawable.ic_star_filled : R.drawable.ic_star,
                actionColor);
        star.setContentDescription(getString(contact.starred
                ? R.string.remove_star : R.string.star_and_pin));
        star.setOnClickListener(view -> {
            if (database.setStarred(contact.id, !contact.starred)) {
                renderDetails();
            } else {
                showList();
            }
        });
        priorityActions.addView(star, new LinearLayout.LayoutParams(0, dp(58), 1));

        Button emergency = secondaryButton(R.string.emergency_short);
        emergency.setTextSize(13);
        emergency.setSelected(contact.emergency);
        if (contact.emergency) {
            emergency.setBackground(rounded(ERROR_FILL, 12));
            emergency.setTextColor(ERROR_COLOR);
        }
        withIcon(emergency, R.drawable.ic_emergency,
                contact.emergency ? ERROR_COLOR : MUTED_COLOR);
        emergency.setContentDescription(getString(contact.emergency
                ? R.string.remove_emergency : R.string.set_emergency));
        emergency.setOnClickListener(view -> {
            if (database.setEmergency(contact.id, !contact.emergency)) {
                renderDetails();
            } else {
                showList();
            }
        });
        LinearLayout.LayoutParams emergencyParams = new LinearLayout.LayoutParams(0, dp(58), 1);
        emergencyParams.leftMargin = dp(10);
        priorityActions.addView(emergency, emergencyParams);
        page.addView(priorityActions, blockParams(dp(8)));
        page.addView(text(R.string.star_help, 12, MUTED_COLOR), blockParams(dp(8)));

        page.addView(sectionLabel(R.string.contact_info), blockParams(dp(20)));
        LinearLayout information = new LinearLayout(this);
        information.setOrientation(LinearLayout.VERTICAL);
        information.setPadding(dp(20), dp(8), dp(20), dp(22));
        information.setBackground(bordered(SURFACE_COLOR, 12));
        detail(information, R.string.email, contact.email, R.drawable.ic_email);
        detailDivider(information);
        detail(information, R.string.address, contact.address, R.drawable.ic_location);
        detailDivider(information);
        detail(information, R.string.notes, contact.notes, R.drawable.ic_note);
        detailDivider(information);
        detail(information, R.string.pronouns, contact.pronouns, R.drawable.ic_person);
        detailDivider(information);
        detail(information, R.string.birthday, contact.birthday, R.drawable.ic_calendar);
        page.addView(information, blockParams(dp(8)));

        page.addView(sectionLabel(R.string.quick_actions), blockParams(dp(20)));
        Button messages = button(R.string.messages);
        withIcon(messages, R.drawable.ic_message, Color.WHITE);
        messages.setOnClickListener(view -> showConversation(contact.id));
        page.addView(messages, blockParams(dp(8)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button call = secondaryButton(R.string.call_short);
        withIcon(call, R.drawable.ic_phone, actionColor);
        call.setOnClickListener(view -> {
            if (contact.blocked) {
                Toast.makeText(this, R.string.blocked_in_app, Toast.LENGTH_LONG).show();
            } else {
                callContact(contact);
            }
        });
        actions.addView(call, new LinearLayout.LayoutParams(0, dp(54), 1));

        Button edit = secondaryButton(R.string.edit_short);
        withIcon(edit, R.drawable.ic_edit, actionColor);
        edit.setOnClickListener(view -> showForm(contact.id));
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(0, dp(54), 1);
        editParams.leftMargin = dp(10);
        actions.addView(edit, editParams);
        page.addView(actions, blockParams(dp(12)));

        Button block = destructiveButton(contact.blocked ? R.string.unblock_contact
                : R.string.block_contact);
        withIcon(block, R.drawable.ic_block, ERROR_COLOR);
        block.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle(contact.blocked ? R.string.unblock_contact : R.string.block_contact)
                .setMessage(contact.blocked ? R.string.unblock_explanation
                        : R.string.block_explanation)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(contact.blocked ? R.string.unblock_contact
                        : R.string.block_contact, (dialog, which) -> {
                    if (database.setBlocked(contact.id, !contact.blocked)) {
                        renderDetails();
                    }
                })
                .show());
        page.addView(block, blockParams(dp(12)));

        Button delete = destructiveButton(R.string.delete_contact);
        withIcon(delete, R.drawable.ic_delete, Color.rgb(220, 38, 38));
        delete.setOnClickListener(view -> new AlertDialog.Builder(this)
                .setTitle(R.string.delete_contact)
                .setMessage(getString(R.string.delete_confirmation, contact.name))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    database.deleteContact(contact.id);
                    showList();
                })
                .show());
        page.addView(delete, blockParams(dp(22)));
    }

    // Build the form for a new or existing contact.
    private void renderForm() {
        Contact contact = selectedId >= 0 ? database.getContact(selectedId) : null;
        if (selectedId >= 0 && contact == null) {
            showList();
            return;
        }
        boolean landscape = isLandscape();
        LinearLayout page = page(contact == null ? R.string.add_contact : R.string.edit_contact, true);
        LinearLayout photoCard = new LinearLayout(this);
        photoCard.setGravity(Gravity.CENTER_VERTICAL);
        photoCard.setPadding(dp(landscape ? 8 : 14), dp(landscape ? 8 : 14),
                dp(landscape ? 8 : 14), dp(landscape ? 8 : 14));
        photoCard.setBackground(bordered(SURFACE_COLOR, 12));
        int photoSize = dp(landscape ? 48 : 76);
        photoPreview = photoView(photoSize);
        if (!showPhoto(photoPreview, photoUri)) {
            showPhotoPlaceholder();
        }
        LinearLayout.LayoutParams previewParams =
                new LinearLayout.LayoutParams(photoSize, photoSize);
        previewParams.rightMargin = dp(14);
        photoCard.addView(photoPreview, previewParams);
        LinearLayout photoActions = new LinearLayout(this);
        photoActions.setOrientation(LinearLayout.VERTICAL);
        TextView photoLabel = text(R.string.contact_photo, 14, INK_COLOR);
        photoLabel.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        if (!landscape) {
            photoActions.addView(photoLabel);
        }
        Button choosePhoto = secondaryButton(R.string.choose_photo);
        withIcon(choosePhoto, R.drawable.ic_camera, actionColor);
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
        if (landscape) {
            choosePhoto.setMinHeight(dp(44));
        }
        photoActions.addView(choosePhoto, blockParams(dp(landscape ? 0 : 6)));
        photoCard.addView(photoActions, new LinearLayout.LayoutParams(0, -2, 1));
        page.addView(photoCard, blockParams(dp(12)));
        removePhotoButton = secondaryButton(R.string.remove_photo);
        removePhotoButton.setVisibility(photoUri.isEmpty() ? View.GONE : View.VISIBLE);
        removePhotoButton.setOnClickListener(view -> {
            photoUri = "";
            showPhotoPlaceholder();
            removePhotoButton.setVisibility(View.GONE);
        });
        page.addView(removePhotoButton, blockParams(dp(10)));
        page.addView(sectionLabel(R.string.basic_info), blockParams(dp(20)));
        LinearLayout nameColumn = landscape ? formColumn() : page;
        LinearLayout phoneColumn = landscape ? formColumn() : page;
        nameField = field(nameColumn, R.string.name, contact == null ? "" : contact.name,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        phoneField = field(phoneColumn, R.string.phone, contact == null ? "" : contact.phone,
                InputType.TYPE_CLASS_PHONE);
        if (landscape) {
            addFormRow(page, nameColumn, phoneColumn);
        }
        page.addView(sectionLabel(R.string.more_details), blockParams(dp(20)));
        LinearLayout emailColumn = landscape ? formColumn() : page;
        LinearLayout pronounsColumn = landscape ? formColumn() : page;
        emailField = field(emailColumn, R.string.email, contact == null ? "" : contact.email,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        pronounsField = field(pronounsColumn, R.string.pronouns,
                contact == null ? "" : contact.pronouns, InputType.TYPE_CLASS_TEXT);
        if (landscape) {
            addFormRow(page, emailColumn, pronounsColumn);
        }
        LinearLayout addressColumn = landscape ? formColumn() : page;
        LinearLayout birthdayColumn = landscape ? formColumn() : page;
        addressField = field(addressColumn, R.string.address, contact == null ? "" : contact.address,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        birthdayField = field(birthdayColumn, R.string.birthday,
                contact == null ? "" : contact.birthday, InputType.TYPE_CLASS_DATETIME);
        birthdayField.setInputType(InputType.TYPE_NULL);
        birthdayField.setFocusable(false);
        birthdayField.setOnClickListener(view -> showBirthdayPicker());
        if (landscape) {
            addFormRow(page, addressColumn, birthdayColumn);
        }
        notesField = field(page, R.string.notes, contact == null ? "" : contact.notes,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        Button save = button(R.string.save);
        withIcon(save, R.drawable.ic_check, Color.WHITE);
        save.setOnClickListener(view -> saveContact());
        addFooterAction(page, save);
    }

    private LinearLayout formColumn() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        return column;
    }

    private void addFormRow(LinearLayout page, LinearLayout first, LinearLayout second) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(first, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(0, -2, 1);
        secondParams.leftMargin = dp(12);
        row.addView(second, secondParams);
        page.addView(row);
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
        String pronouns = pronounsField.getText().toString().trim();
        String birthday = birthdayField.getText().toString().trim();
        // Save the form in SQLite, then show the saved contact.
        if (selectedId < 0) {
            selectedId = database.createContact(name, phone, email, address, notes, photoUri,
                    pronouns, birthday);
        } else if (!database.updateContact(selectedId, name, phone, email, address, notes,
                photoUri, pronouns, birthday)) {
            Toast.makeText(this, R.string.contact_missing, Toast.LENGTH_SHORT).show();
            showList();
            return;
        }
        showDetails(selectedId);
    }

    private void showBirthdayPicker() {
        Calendar date = Calendar.getInstance();
        String value = birthdayField.getText().toString();
        if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            date.set(Calendar.YEAR, Integer.parseInt(value.substring(0, 4)));
            date.set(Calendar.MONTH, Integer.parseInt(value.substring(5, 7)) - 1);
            date.set(Calendar.DAY_OF_MONTH, Integer.parseInt(value.substring(8, 10)));
        }
        DatePickerDialog dialog = new DatePickerDialog(this, (picker, year, month, day) ->
                birthdayField.setText(String.format(Locale.ROOT, "%04d-%02d-%02d",
                        year, month + 1, day)), date.get(Calendar.YEAR),
                date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.clear_birthday),
                (choice, which) -> birthdayField.setText(""));
        dialog.show();
    }

    // Read and display the messages for the selected contact.
    private void renderConversation() {
        Contact contact = database.getContact(selectedId);
        if (contact == null) {
            showList();
            return;
        }
        if (contact.blocked) {
            showDetails(contact.id);
            return;
        }
        String draft = messageField == null ? "" : messageField.getText().toString();
        LinearLayout page = page(R.string.conversation, true);
        LinearLayout recipient = new LinearLayout(this);
        recipient.setGravity(Gravity.CENTER_VERTICAL);
        recipient.setPadding(dp(16), dp(14), dp(16), dp(14));
        recipient.setBackground(bordered(SURFACE_COLOR, 12));
        View recipientAvatar = avatar(contact, dp(48));
        LinearLayout.LayoutParams recipientAvatarParams =
                new LinearLayout.LayoutParams(dp(48), dp(48));
        recipientAvatarParams.rightMargin = dp(14);
        recipient.addView(recipientAvatar, recipientAvatarParams);
        LinearLayout recipientText = new LinearLayout(this);
        recipientText.setOrientation(LinearLayout.VERTICAL);
        TextView contactName = text(contact.name, 18, INK_COLOR);
        contactName.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        recipientText.addView(contactName);
        recipientText.addView(text(contact.phone, 14, MUTED_COLOR));
        recipient.addView(recipientText);
        page.addView(recipient, blockParams(dp(12)));

        if (!hasPermission(Manifest.permission.RECEIVE_SMS)) {
            TextView permissionNotice = text(R.string.receive_permission_needed, 14,
                    Color.rgb(220, 38, 38));
            page.addView(permissionNotice, blockParams(dp(12)));
        }

        java.util.List<Message> messages = database.listMessages(contact.id);
        if (messages.isEmpty()) {
            TextView empty = text(R.string.no_messages, 15, MUTED_COLOR);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(36), dp(20), dp(36));
            page.addView(empty, blockParams(dp(20)));
        }
        for (Message message : messages) {
            LinearLayout bubble = new LinearLayout(this);
            bubble.setOrientation(LinearLayout.VERTICAL);
            bubble.setPadding(dp(16), dp(12), dp(16), dp(12));
            bubble.setBackground(rounded(message.direction == Message.SENT
                    ? sentColor : RECEIVED_COLOR, 12));
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
            bubbleParams.topMargin = dp(14);
            bubbleParams.gravity = message.direction == Message.SENT
                    ? Gravity.END : Gravity.START;
            page.addView(bubble, bubbleParams);
        }

        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(10), dp(7), dp(7), dp(7));
        composer.setBackground(bordered(SURFACE_COLOR, 12));
        messageField = new EditText(this);
        messageField.setHint(R.string.message_hint);
        messageField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        messageField.setMinLines(isLandscape() ? 1 : 2);
        messageField.setMaxLines(isLandscape() ? 2 : 4);
        messageField.setText(draft);
        messageField.setBackgroundColor(Color.TRANSPARENT);
        messageField.setTextColor(INK_COLOR);
        messageField.setHintTextColor(MUTED_COLOR);
        composer.addView(messageField, new LinearLayout.LayoutParams(0, -2, 1));
        Button send = button(R.string.send);
        withIcon(send, R.drawable.ic_send, Color.WHITE);
        send.setOnClickListener(view -> sendMessage(contact));
        composer.addView(send);
        addFooterContent(page, composer);
        ScrollView scroll = (ScrollView) page.getParent();
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    // Save a message before asking Android to send it.
    private void sendMessage(Contact contact) {
        Contact current = database.getContact(contact.id);
        if (current == null || current.blocked) {
            Toast.makeText(this, R.string.blocked_in_app, Toast.LENGTH_LONG).show();
            showDetails(contact.id);
            return;
        }
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
        image.setBackground(rounded(cardColor, 12));
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
            shape.setColor(cardColor);
            image.setBackground(shape);
            if (showPhoto(image, contact.photoUri)) {
                return image;
            }
        }
        String initial = contact.name.isEmpty() ? "?" : contact.name.substring(0,
                contact.name.offsetByCodePoints(0, 1)).toUpperCase(Locale.getDefault());
        TextView label = text(initial, size >= dp(70) ? 30 : 22, INK_COLOR);
        label.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        label.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        int[] avatarColors = {
                sentColor, cardColor, softTint(actionColor), softTint(accentColor)
        };
        circle.setColor(avatarColors[(contact.name.hashCode() & Integer.MAX_VALUE)
                % avatarColors.length]);
        label.setBackground(circle);
        return label;
    }

    private int softTint(int color) {
        return Color.rgb((Color.red(color) + 255 * 7) / 8,
                (Color.green(color) + 255 * 7) / 8,
                (Color.blue(color) + 255 * 7) / 8);
    }

    private void showPhotoPlaceholder() {
        photoPreview.setImageResource(R.drawable.ic_camera);
        photoPreview.setImageTintList(ColorStateList.valueOf(actionColor));
        int padding = dp(isLandscape() ? 10 : 24);
        photoPreview.setPadding(padding, padding, padding, padding);
        photoPreview.setVisibility(View.VISIBLE);
    }

    // Load a smaller image to limit memory use.
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
            image.setImageTintList(null);
            image.setPadding(0, 0, 0, 0);
            image.setImageBitmap(bitmap);
            image.setVisibility(View.VISIBLE);
            return true;
        } catch (IOException | SecurityException exception) {
            image.setVisibility(View.GONE);
            return false;
        }
    }

    // Build the shared page header and content area.
    private LinearLayout page(int title, boolean back) {
        boolean landscape = isLandscape();
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setBackgroundColor(PAGE_COLOR);
        setContentView(outer);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(landscape ? 16 : 20), dp(landscape ? 4 : 8),
                dp(landscape ? 16 : 20), dp(landscape ? 4 : 17));
        header.setBackground(headerBackground(headerTheme));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        if (back) {
            ImageView backButton = iconView(R.drawable.ic_back, INK_COLOR);
            backButton.setContentDescription(getString(R.string.back));
            backButton.setPadding(dp(12), dp(12), dp(12), dp(12));
            backButton.setBackground(bordered(SURFACE_COLOR, 100));
            backButton.setOnClickListener(view -> onBackPressed());
            toolbar.addView(backButton, new LinearLayout.LayoutParams(dp(44), dp(44)));
        } else {
            ImageView logo = new ImageView(this);
            logo.setImageResource(R.drawable.ic_brand_mark);
            logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            logo.setPadding(dp(7), dp(7), dp(7), dp(7));
            logo.setBackground(rounded(actionColor, 100));
            logo.setClipToOutline(true);
            logo.setContentDescription(getString(R.string.app_name));
            toolbar.addView(logo, new LinearLayout.LayoutParams(dp(44), dp(44)));
        }
        if (landscape) {
            TextView compactTitle = text(title, 21, INK_COLOR);
            compactTitle.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
            compactTitle.setSingleLine(true);
            compactTitle.setEllipsize(TextUtils.TruncateAt.END);
            compactTitle.setPadding(dp(16), 0, dp(12), 0);
            toolbar.addView(compactTitle, new LinearLayout.LayoutParams(0, -2, 1));
        } else {
            toolbar.addView(new View(this), new LinearLayout.LayoutParams(0, dp(1), 1));
        }
        ImageView menuButton = iconView(R.drawable.ic_more, INK_COLOR);
        menuButton.setContentDescription(getString(R.string.change_header_color));
        menuButton.setPadding(dp(12), dp(12), dp(12), dp(12));
        menuButton.setBackground(bordered(SURFACE_COLOR, 100));
        menuButton.setOnClickListener(view -> showHeaderThemeDialog());
        toolbar.addView(menuButton, new LinearLayout.LayoutParams(dp(44), dp(44)));
        header.addView(toolbar);

        if (!landscape) {
            LinearLayout headingRow = new LinearLayout(this);
            headingRow.setGravity(Gravity.CENTER_VERTICAL);
            TextView heading = text(title, 30, INK_COLOR);
            heading.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
            headingRow.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
            if (screen == LIST) {
                ImageView ornament = iconView(HEADER_THEME_ICONS[headerTheme],
                        HEADER_THEME_ACCENT[headerTheme]);
                ornament.setPadding(dp(10), dp(10), dp(10), dp(10));
                ornament.setBackground(bordered(SURFACE_COLOR, 100));
                headingRow.addView(ornament, new LinearLayout.LayoutParams(dp(40), dp(40)));
            }
            header.addView(headingRow, blockParams(dp(18)));
            TextView subtitle = text(subtitleForScreen(), 14, MUTED_COLOR);
            header.addView(subtitle, blockParams(dp(5)));
            View accent = new View(this);
            accent.setBackground(rounded(HEADER_THEME_ACCENT[headerTheme], 100));
            LinearLayout.LayoutParams accentParams = new LinearLayout.LayoutParams(dp(32), dp(3));
            accentParams.topMargin = dp(15);
            header.addView(accent, accentParams);
        }
        outer.addView(header, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        contentScroll = scroll;
        scroll.setFillViewport(true);
        outer.setOnApplyWindowInsetsListener((view, insets) -> {
            int top = insets.getSystemWindowInsetTop();
            header.setPadding(dp(landscape ? 16 : 20), top + dp(landscape ? 4 : 8),
                    dp(landscape ? 16 : 20), dp(landscape ? 4 : 17));
            outer.setPadding(0, 0, 0, insets.getStableInsetBottom());
            return insets;
        });
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(landscape ? 16 : 20), dp(landscape ? 8 : 12),
                screen == LIST ? dp(50) : dp(landscape ? 16 : 20),
                dp(landscape ? 14 : 28));
        scroll.addView(content);
        if (screen == LIST) {
            FrameLayout frame = new FrameLayout(this);
            frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
            outer.addView(frame, new LinearLayout.LayoutParams(-1, 0, 1));
        } else {
            outer.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        }
        return content;
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
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
        if (isLandscape()) {
            action.setMinHeight(dp(44));
        }
        action.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(isLandscape() ? 44 : 54)));
        addFooterContent(page, action);
    }

    private void addFooterContent(LinearLayout page, View content) {
        ScrollView scroll = (ScrollView) page.getParent();
        View parent = (View) scroll.getParent();
        LinearLayout outer = (LinearLayout) (parent instanceof FrameLayout
                ? parent.getParent() : parent);
        LinearLayout footer = new LinearLayout(this);
        footer.setPadding(dp(isLandscape() ? 16 : 20), dp(isLandscape() ? 5 : 12),
                dp(isLandscape() ? 16 : 20), dp(isLandscape() ? 5 : 14));
        footer.setBackgroundColor(SURFACE_COLOR);
        footer.setElevation(dp(2));
        if (content.getLayoutParams() == null) {
            content.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        }
        footer.addView(content);
        outer.addView(footer, new LinearLayout.LayoutParams(-1, -2));
    }

    private GradientDrawable headerBackground(int theme) {
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{HEADER_THEME_START[theme], HEADER_THEME_END[theme]});
        return shape;
    }

    private void applyHeaderTheme(int theme) {
        int scrollY = contentScroll == null ? 0 : contentScroll.getScrollY();
        String name = screen == FORM ? nameField.getText().toString() : null;
        String phone = screen == FORM ? phoneField.getText().toString() : null;
        String email = screen == FORM ? emailField.getText().toString() : null;
        String address = screen == FORM ? addressField.getText().toString() : null;
        String notes = screen == FORM ? notesField.getText().toString() : null;
        String pronouns = screen == FORM ? pronounsField.getText().toString() : null;
        String birthday = screen == FORM ? birthdayField.getText().toString() : null;
        headerTheme = theme;
        getPreferences(MODE_PRIVATE).edit().putInt(PREF_HEADER_THEME, theme).apply();
        setThemePalette(theme);
        getWindow().setStatusBarColor(HEADER_THEME_START[theme]);
        render();
        if (screen == FORM) {
            nameField.setText(name);
            phoneField.setText(phone);
            emailField.setText(email);
            addressField.setText(address);
            notesField.setText(notes);
            pronounsField.setText(pronouns);
            birthdayField.setText(birthday);
        }
        if (screen != CONVERSATION) {
            ScrollView scroll = contentScroll;
            scroll.post(() -> scroll.scrollTo(0, scrollY));
        }
    }

    private void setThemePalette(int theme) {
        actionColor = HEADER_THEME_ACCENT[theme];
        accentColor = THEME_SECONDARY[theme];
        cardColor = THEME_CARD[theme];
        sentColor = THEME_SENT[theme];
    }

    private void showHeaderThemeDialog() {
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.VERTICAL);
        choices.setPadding(dp(20), dp(4), dp(20), dp(8));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.change_header_color)
                .setView(choices)
                .setNegativeButton(R.string.cancel, null)
                .create();
        for (int i = 0; i < HEADER_THEME_START.length; i++) {
            final int theme = i;
            LinearLayout option = new LinearLayout(this);
            option.setGravity(Gravity.CENTER_VERTICAL);
            option.setPadding(dp(12), dp(10), dp(12), dp(10));
            option.setBackground(bordered(i == headerTheme ? cardColor : SURFACE_COLOR, 12));

            FrameLayout swatch = new FrameLayout(this);
            GradientDrawable preview = headerBackground(i);
            preview.setCornerRadius(dp(8));
            preview.setStroke(dp(1), BORDER_COLOR);
            swatch.setBackground(preview);
            ImageView motif = iconView(HEADER_THEME_ICONS[i], HEADER_THEME_ACCENT[i]);
            FrameLayout.LayoutParams motifParams = new FrameLayout.LayoutParams(
                    dp(20), dp(20), Gravity.CENTER);
            swatch.addView(motif, motifParams);
            LinearLayout.LayoutParams swatchParams =
                    new LinearLayout.LayoutParams(dp(48), dp(40));
            swatchParams.rightMargin = dp(12);
            option.addView(swatch, swatchParams);

            LinearLayout copy = new LinearLayout(this);
            copy.setOrientation(LinearLayout.VERTICAL);
            TextView label = text(HEADER_THEME_LABELS[i], 15, INK_COLOR);
            label.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            copy.addView(label);
            copy.addView(text(HEADER_THEME_DESCRIPTIONS[i], 12, MUTED_COLOR));
            option.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
            if (i == headerTheme) {
                ImageView selected = iconView(R.drawable.ic_check, HEADER_THEME_ACCENT[i]);
                option.addView(selected, new LinearLayout.LayoutParams(dp(20), dp(20)));
            }
            option.setOnClickListener(view -> {
                applyHeaderTheme(theme);
                dialog.dismiss();
            });
            choices.addView(option, blockParams(dp(8)));
        }
        dialog.show();
    }

    private TextView sectionLabel(int label) {
        TextView section = text(label, 12, MUTED_COLOR);
        section.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        section.setLetterSpacing(0.11f);
        return section;
    }

    private void detail(LinearLayout page, int label, String value, int icon) {
        LinearLayout labelLine = new LinearLayout(this);
        labelLine.setGravity(Gravity.CENTER_VERTICAL);
        ImageView symbol = iconView(icon, MUTED_COLOR);
        LinearLayout.LayoutParams symbolParams = new LinearLayout.LayoutParams(dp(16), dp(16));
        symbolParams.rightMargin = dp(8);
        labelLine.addView(symbol, symbolParams);
        TextView heading = text(label, 12, MUTED_COLOR);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        heading.setLetterSpacing(0.05f);
        labelLine.addView(heading);
        page.addView(labelLine, blockParams(dp(14)));
        TextView body = text(value.isEmpty() ? getString(R.string.not_provided) : value,
                17, INK_COLOR);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(-1, -2);
        bodyParams.leftMargin = dp(24);
        bodyParams.topMargin = dp(3);
        page.addView(body, bodyParams);
    }

    private void detailDivider(LinearLayout page) {
        View divider = new View(this);
        divider.setBackgroundColor(BORDER_COLOR);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(1));
        params.topMargin = dp(16);
        page.addView(divider, params);
    }

    private EditText field(LinearLayout page, int label, String value, int inputType) {
        TextView heading = text(label, 13, MUTED_COLOR);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        page.addView(heading, blockParams(dp(12)));
        EditText input = new EditText(this);
        input.setSingleLine((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        input.setInputType(inputType);
        input.setHint(label);
        input.setText(value);
        input.setTextSize(17);
        input.setTextColor(INK_COLOR);
        input.setHintTextColor(MUTED_COLOR);
        input.setBackground(bordered(SURFACE_COLOR, 12));
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
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        button.setMinHeight(dp(52));
        button.setBackground(primaryBackground());
        button.setStateListAnimator(null);
        button.setElevation(dp(2));
        return button;
    }

    private Button secondaryButton(int label) {
        Button button = button(label);
        button.setBackground(bordered(SURFACE_COLOR, 12));
        button.setTextColor(INK_COLOR);
        button.setElevation(0);
        return button;
    }

    private Button destructiveButton(int label) {
        Button button = button(label);
        button.setBackground(rounded(Color.rgb(254, 242, 242), 12));
        button.setTextColor(Color.rgb(220, 38, 38));
        button.setElevation(0);
        return button;
    }

    private GradientDrawable primaryBackground() {
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{actionColor, accentColor});
        shape.setCornerRadius(dp(12));
        return shape;
    }

    private void withIcon(Button button, int icon, int tint) {
        button.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
        button.setCompoundDrawableTintList(ColorStateList.valueOf(tint));
        button.setCompoundDrawablePadding(dp(8));
    }

    private ImageView iconView(int icon, int tint) {
        ImageView image = new ImageView(this);
        image.setImageResource(icon);
        image.setImageTintList(ColorStateList.valueOf(tint));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return image;
    }

    private LinearLayout statusBadge(int icon, int label, int foreground, int background) {
        LinearLayout badge = new LinearLayout(this);
        badge.setGravity(Gravity.CENTER_VERTICAL);
        badge.setPadding(dp(9), dp(5), dp(9), dp(5));
        badge.setBackground(rounded(background, 100));
        ImageView symbol = iconView(icon, foreground);
        LinearLayout.LayoutParams symbolParams = new LinearLayout.LayoutParams(dp(14), dp(14));
        symbolParams.rightMargin = dp(5);
        badge.addView(symbol, symbolParams);
        TextView caption = text(label, 10, foreground);
        caption.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        caption.setLetterSpacing(0.04f);
        badge.addView(caption);
        return badge;
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
