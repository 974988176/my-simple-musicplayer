package com.ysh.mysimplemusicplayer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MusicPlayerApp extends Application {

    private double offsetX;
    private double offsetY;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(this.getClass().getResource("/fxml/player.fxml"));

        Scene scene = new Scene(root, null);
        primaryStage.setScene(scene);
        primaryStage.setFullScreenExitHint("");

        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setTitle("JavaFX Music");
        String iconPath = getClass().getResource("/img/logo.png").toExternalForm();
        primaryStage.getIcons().add(new Image(iconPath));
        primaryStage.show();

        // 鼠标按下时记录按下时鼠标距离程序窗口的坐标
        scene.setOnMousePressed(event -> {
            offsetX = event.getSceneX();
            offsetY = event.getSceneY();
        });

        // 拖动软件时,设置程序整个窗口的位置,减去之前鼠标点击的偏差
        scene.setOnMouseDragged(event -> {
            primaryStage.setX(event.getScreenX() - offsetX);
            primaryStage.setY(event.getScreenY() - offsetY);
        });

    }
}
