package com.med.utils;

import javafx.application.Platform;
import javafx.scene.control.TableCell;
import javafx.scene.image.*;
import javafx.scene.input.TransferMode;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by pavellsda on 21.01.17.
 */
public final class TableCellExtension extends TableCell<Path, Path> {
    private Integer iconImageWidth = null;
    private Map<Path, javafx.scene.image.Image> iconImgCache;

    public TableCellExtension() {
        setOnDragEntered(ev -> {
            System.out.println("c dr en");
            if (ev.getDragboard().hasFiles()) {
                if (getItem() != null && Files.isDirectory(getItem())) {
                    setStyle("-fx-background-color: cyan;");
                }
            }
        });
        setOnDragExited(ev -> {
            if (ev.getDragboard().hasFiles()) {
                if (getItem() != null && Files.isDirectory(getItem())) {
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });
        setOnDragOver(ev -> {
            if (ev.getDragboard().hasFiles()) {
                ev.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                ev.consume();
            }
        });
        setOnDragDropped(ev -> {
            if (ev.getDragboard().hasFiles()) {
                System.out.println("dropped:" + ev.getDragboard().getFiles());
            }
        });
    }
    @Override
    protected void updateItem(Path item, boolean empty) {
        if (item != null) {
            ImageView imageView = new ImageView();
            if (iconImgCache.get(item) != null) {

                imageView.setImage(iconImgCache.get(item));
            } else {

                if (iconImageWidth == null) {
                    ImageIcon icon = (ImageIcon) FileSystemView.getFileSystemView().getSystemIcon(item.toFile());
                    java.awt.Image image = icon.getImage();
                    iconImageWidth = image.getWidth(null);
                }
                imageView.setFitWidth(iconImageWidth);

                Executor exec = Executors.newSingleThreadExecutor();
                CompletableFuture
                        .supplyAsync(() -> Utils.getSystemIcon(item), exec)
                        .whenComplete((res, e) -> {
                            iconImgCache.put(item, res);
                            Platform.runLater(() -> imageView.setImage(res));
                        });
            }
            setGraphic(imageView);
            setText(item.getFileName().toString());
            setItem(item);
        } else {
            setGraphic(null);
            setText(null);
            setItem(null);
        }
    }
    public void setIconImgCache(Map<Path, Image> iconImgCache){
        this.iconImgCache = iconImgCache;
    }


}
