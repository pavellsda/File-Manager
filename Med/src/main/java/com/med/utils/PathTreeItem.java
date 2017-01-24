package com.med.utils;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Created by pavellsda on 21.01.17.
 */
public final class PathTreeItem extends TreeItem<String> {
    public Path myPath;


    public PathTreeItem(Path path) {
        super(path.toString());
        this.myPath = path;

        if (!path.toString().endsWith(File.separator)) {
            String value = path.toString();
            int indexOf = value.lastIndexOf(File.separator);
            if (indexOf > 0) {
                this.setValue(value.substring(indexOf + 1));
            } else {
                this.setValue(value);
            }
        }

        CompletableFuture
                .supplyAsync(() -> Utils.getSystemIcon(path))
                .whenComplete((res, e) -> Platform.runLater(() -> this.setGraphic(new ImageView(res))));


        addEventHandler(TreeItem.branchExpandedEvent(), (EventHandler<TreeModificationEvent<String>>) ev -> {
            PathTreeItem source = (PathTreeItem) ev.getSource();
            if (!source.myPath.equals(myPath)) return;
            if (source.getChildren().isEmpty()) {
                if (!source.isLeaf()) {
                    try {
                        DirectoryStream<Path> dir = Files.newDirectoryStream(myPath);
                        for (Path file : dir) {
                            if (Files.isDirectory(file)) {
                                source.getChildren().add(new PathTreeItem(file));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
