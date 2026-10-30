// This class holds the data for one contact.
package com.hguo.fthangouts;

final class Contact {
    final long id;
    final String name;
    final String phone;
    final String email;
    final String address;
    final String notes;
    final String photoUri;
    final String pronouns;
    final String birthday;
    final boolean starred;
    final boolean emergency;
    final boolean blocked;

    Contact(long id, String name, String phone, String email, String address, String notes,
            String photoUri, String pronouns, String birthday, boolean starred,
            boolean emergency, boolean blocked) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.notes = notes;
        this.photoUri = photoUri;
        this.pronouns = pronouns;
        this.birthday = birthday;
        this.starred = starred;
        this.emergency = emergency;
        this.blocked = blocked;
    }
}
