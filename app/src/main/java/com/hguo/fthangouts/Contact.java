package com.hguo.fthangouts;

final class Contact {
    final long id;
    final String name;
    final String phone;
    final String email;
    final String address;
    final String notes;
    final String photoUri;

    Contact(long id, String name, String phone, String email, String address, String notes,
            String photoUri) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.notes = notes;
        this.photoUri = photoUri;
    }
}
