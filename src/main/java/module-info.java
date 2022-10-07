module com.ysh.mysimplemusicplayer {
    requires javafx.controls;
    requires javafx.fxml;
    requires rxcontrols;
    requires javafx.media;

    opens com.ysh.mysimplemusicplayer.controller to javafx.fxml;
    exports com.ysh.mysimplemusicplayer;
}
