package com.med;/**
 * Created by pavellsda on 18.01.17.
 */

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class MainWindow extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("FileManager");

        initRootLayout();
    }

    /**
     * Инициализируем корневой макет приложения
     */
    private void initRootLayout() {
        try {
            // Загружаем корневой макет из fxml файла.
            String fxmlFile = "/view/mainwindow.fxml";
            FXMLLoader loader = new FXMLLoader();
            Parent root = loader.load(getClass().getResourceAsStream(fxmlFile));
            // Отображаем сцену, содержащую корневой макет.
            Scene scene = new Scene(root);
            scene.getStylesheets().add((getClass().getResource("/css/theme.css")).toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.getIcons().add(new Image("/img/icon.png"));
            primaryStage.setResizable(false);
            primaryStage.show();
            //AquaFx.style();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Возвращает главную сцену.
     * @return
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}