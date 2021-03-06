package com.med.utils;

/**
 * Created by pavellsda on 21.01.17.
 */

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public class Utils {

    public static Image getSystemIcon(Path item) {
        ImageIcon icon = (ImageIcon) FileSystemView.getFileSystemView().getSystemIcon(item.toFile());
        java.awt.Image image = icon.getImage();
        Image ret = null;
        try {
            ret = createImage(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static Image createImage(java.awt.Image image) throws IOException {
        if (!(image instanceof RenderedImage)) {
            BufferedImage bufferedImage = new BufferedImage(image.getWidth(null),
                    image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            Graphics g = bufferedImage.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            image = bufferedImage;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write((RenderedImage) image, "png", out);
        out.flush();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        return new javafx.scene.image.Image(in);
    }

    public static void alert(String text) {
        Stage stage = new Stage(StageStyle.UTILITY);
        stage.setTitle("alert");
        Group group = new Group(new Text(25,25,text));
        stage.setScene(new Scene(group, 350, 100));
        stage.show();

    }



}