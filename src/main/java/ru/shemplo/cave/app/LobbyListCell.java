package ru.shemplo.cave.app;

import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import ru.shemplo.cave.app.entity.Player;
import ru.shemplo.cave.app.resources.LevelTextures;
import ru.shemplo.cave.app.styles.FontStyles;

public class LobbyListCell extends ListCell <Player> {
    
    private final ImageView imageIV = new ImageView ();
    
    public LobbyListCell () {
        setFont (FontStyles.NORMAL_16_FONT);
        setBackground (Background.EMPTY);
        setTextFill (Color.WHITESMOKE);
        imageIV.setFitHeight (20);
        imageIV.setFitWidth (20);
        setGraphicTextGap (8);
    }
    
    @Override
    protected void updateItem (Player item, boolean empty) {
        if (item == getItem ()) { return; }
        
        super.updateItem (item, empty);
        
        if (item == null || empty) {
            super.setGraphic (null);
            super.setText (null);
            
            return;
        }
        
        super.setText (item.getLogin ());
        
        imageIV.setImage (item.isReady () ? LevelTextures.tickIcon : LevelTextures.crossIcon);
        super.setGraphic (imageIV);
    }
    
}
