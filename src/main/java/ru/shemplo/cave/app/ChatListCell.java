package ru.shemplo.cave.app;

import javafx.scene.control.ListCell;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import ru.shemplo.cave.app.entity.ChatMessage;
import ru.shemplo.cave.app.styles.FontStyles;

public class ChatListCell extends ListCell <ChatMessage> {
    
    public ChatListCell () {
        setFont (FontStyles.NORMAL_16_FONT);
        setBackground (Background.EMPTY);
        setTextFill (Color.WHITESMOKE);
        setGraphicTextGap (8);
    }
    
    @Override
    protected void updateItem (ChatMessage item, boolean empty) {
        if (item == getItem ()) { return; }
        
        super.updateItem (item, empty);
        
        if (item == null || empty) {
            super.setGraphic (null);
            super.setText (null);
            
            return;
        }
        
        super.setText (String.format ("< %s > %s", item.getAuthor (), item.getText ()));
    }
    
}
