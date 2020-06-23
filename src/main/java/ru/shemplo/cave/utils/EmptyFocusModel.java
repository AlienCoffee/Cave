package ru.shemplo.cave.utils;

import javafx.scene.control.FocusModel;

public class EmptyFocusModel <T> extends FocusModel <T> {

    @Override
    protected int getItemCount () {
        return 0;
    }

    @Override
    protected T getModelItem (int index) {
        return null;
    }
    
}
