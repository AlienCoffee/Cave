package ru.shemplo.cave.app.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public class ChatMessage {
    
    private final String author, text;
    
    private final long created;
    
}
