package com.dumbster.smtp;

import java.util.List;

public record Context(List<Message> messages, Message message, Response response) {
}
