package com.ysh.mysimplemusicplayer.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

import java.io.File;

public class MusicListCell extends ListCell<File> {

    private final BorderPane borderPane;
    private final Label label;

    public MusicListCell() {
        borderPane = new BorderPane();
        label = new Label();
        BorderPane.setAlignment(label, Pos.CENTER_LEFT);
        Button button = new Button();
        button.getStyleClass().add("remove-btn");
        button.setGraphic(new Region());
        borderPane.setCenter(label);
        borderPane.setRight(button);
        button.setOnAction(event -> {
            // 删除当前按钮所在的行
            getListView().getItems().remove(getItem());
        });
    }

    @Override
    protected void updateItem(File item, boolean empty) {
        super.updateItem(item, empty);
        if (item==null || empty){
            setGraphic(null);
            setText("");
            return;
        }

        String name = item.getName();
        String musicName = name.substring(0, name.length() - 4);
        label.setText(musicName);
        setGraphic(borderPane);
    }
}
