package com.med.utils;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.cell.TextFieldTreeCell;

/**
 * Created by pavellsda on 21.01.17.
 */
public final class TreeCellExtension extends TextFieldTreeCell<String> {
    private ContextMenu addMenu = new ContextMenu();

    public TreeCellExtension() {
        MenuItem propMenuItem = new MenuItem("Oppa");
        addMenu.getItems().add(propMenuItem);
        propMenuItem.setOnAction(ev -> {
            Utils.alert("Hello\nworld");
        });
    }
    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (!empty) {
            setContextMenu(addMenu);
        }
    }
}
