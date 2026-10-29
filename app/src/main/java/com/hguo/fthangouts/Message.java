package com.hguo.fthangouts;

final class Message {
    static final int RECEIVED = 0;
    static final int SENT = 1;
    static final int PENDING = 0;
    static final int COMPLETE = 1;
    static final int FAILED = 2;

    final long id;
    final long contactId;
    final String body;
    final int direction;
    final long timestamp;
    final int status;

    Message(long id, long contactId, String body, int direction, long timestamp, int status) {
        this.id = id;
        this.contactId = contactId;
        this.body = body;
        this.direction = direction;
        this.timestamp = timestamp;
        this.status = status;
    }
}
