package com.ysh.mysimplemusicplayer.controller;

import com.leewyatt.rxcontrols.controls.RXAudioSpectrum;
import com.leewyatt.rxcontrols.controls.RXLrcView;
import com.leewyatt.rxcontrols.controls.RXMediaProgressBar;
import com.leewyatt.rxcontrols.controls.RXToggleButton;
import com.leewyatt.rxcontrols.pojo.LrcDoc;
import com.ysh.mysimplemusicplayer.component.MusicListCell;
import com.ysh.mysimplemusicplayer.utils.EncodingDetect;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerController {
    private final float[] emptyFlocatArr = new float[128];
    private final int INIT_MUSIC_SIZE = 2;
    private final SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
    @FXML
    private RXAudioSpectrum audioSpectrum;
    private final AudioSpectrumListener audioSpectrumListener = (timestamp, duration, magnitudes, phases) -> audioSpectrum.setMagnitudes(magnitudes);
    @FXML
    private RXLrcView lrcView;
    @FXML
    private ToggleButton playBtn;

    /**
     * 设置当前播放进度条事件
     */
    @FXML
    private RXMediaProgressBar progressBar;
    @FXML
    private ListView<File> listView;
    @FXML
    private Label timeLabel;

    @FXML
    private StackPane soundBtn;
    @FXML
    private StackPane skinBtn;
    @FXML
    private URL location;
    @FXML
    private AnchorPane drawerPane;
    @FXML
    private BorderPane sliderPane;
    private Timeline showAnim;
    private Timeline hideAnim;
    private ContextMenu soundPopup;
    private ContextMenu skinPopup;
    private MediaPlayer mediaPlayer;
    /**
     * 进度条被点击或拖动事件
     */
    private final EventHandler<MouseEvent> progressBarHandle = event -> {
        if (mediaPlayer != null) {
            mediaPlayer.seek(progressBar.getCurrentTime());
            changeTimeLabel(progressBar.getCurrentTime());
        }
    };
    private final ChangeListener<Duration> durationChangeListener = (observable1, oldValue1, newValue1) -> {
        progressBar.setCurrentTime(newValue1); //  设置当前进度

        changeTimeLabel(newValue1);
    };
    private Slider soundSlider;

    /**
     * 改变当前播放进度时间文本
     *
     * @param newValue1 当前播放时间
     */
    private void changeTimeLabel(Duration newValue1) {
        // 设置当前播放时间文本
        String currentTime = sdf.format(newValue1.toMillis());
        String totalTime = sdf.format(mediaPlayer.getBufferProgressTime().toMillis());
        timeLabel.setText(currentTime + " / " + totalTime);
    }

    @FXML
    void initialize() {
        Arrays.fill(emptyFlocatArr, -60.0f);
        initSliderAnim();
        initSoundPopup();
        initSkinPopup();
        initListView();
        initProgressBar();
    }


    private void initListView() {
        // 设置自定义歌曲列表的样式,左边歌曲名称,右侧按钮
        listView.setCellFactory(param -> new MusicListCell());
        // 默认添加几个歌曲
        List<File> musicList = initMusicList();
        listView.getItems().addAll(musicList);

        // 设置点击歌名事件,播放歌曲
        listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newMusicFile) -> {
            if (newMusicFile != null) {
                // 如果之前已经有播放器了,这是切歌,需要先销毁之前的播放器
                if (mediaPlayer != null) {
                    disposeMediaPlayer();
                }
                initMediaPlayer(newMusicFile);
            } else {
                // 如果当前选择的是空，就是当前播放的歌曲被删除了
                disposeMediaPlayer();
            }
        });
    }

    private void initMediaPlayer(File newMusicFile) {
        // 销毁完,再重新创建一个播放器
        mediaPlayer = new MediaPlayer(new Media(newMusicFile.toURI().toString()));
        // 设置播放器的声音,注意,这是0-1,所以要/100
        mediaPlayer.setVolume(soundSlider.getValue() / 100);

        // 设置歌词,如果歌词存在,就设置,并引入工具库自动检测歌词编码
        String lrcPath = newMusicFile.getAbsolutePath().replaceAll("mp3$", "lrc");
        File lrcFile = new File(lrcPath);
        if (lrcFile.exists()) {
            try {
                byte[] bytes = Files.readAllBytes(lrcFile.toPath());
                lrcView.setLrcDoc(LrcDoc.parseLrcDoc(new String(bytes, EncodingDetect.detect(bytes))));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // 设置歌词进度
            lrcView.currentTimeProperty().bind(mediaPlayer.currentTimeProperty());
        }

        // 设置频谱可视化
        mediaPlayer.setAudioSpectrumListener(audioSpectrumListener);

        // 设置进度条
        progressBar.durationProperty().bind(mediaPlayer.getMedia().durationProperty());

        // 播放进度修改
        mediaPlayer.currentTimeProperty().addListener(durationChangeListener);

        // 播放按钮改为正在播放
        playBtn.setSelected(true);

        // 当这首歌播放成功后,自动播放下一首
        mediaPlayer.setOnEndOfMedia(this::playNextMusic);

        // 开始播放
        mediaPlayer.play();
    }

    /**
     * 停止，销毁，置null
     * 取消事件绑定，防止内存泄露
     */
    private void disposeMediaPlayer() {
        mediaPlayer.stop();
        lrcView.setLrcDoc(null);
        lrcView.currentTimeProperty().unbind();
        lrcView.setCurrentTime(Duration.ZERO);
        mediaPlayer.setAudioSpectrumListener(null);
        audioSpectrum.setMagnitudes(emptyFlocatArr);
        progressBar.durationProperty().unbind();
        progressBar.setCurrentTime(Duration.ZERO);
        mediaPlayer.currentTimeProperty().removeListener(durationChangeListener);
        mediaPlayer.setOnEndOfMedia(null);
        timeLabel.setText("00:00 / 00:00");
        playBtn.setSelected(false);
        mediaPlayer.dispose();
        mediaPlayer = null;
    }

    /**
     * 单击或拖动进度条改变歌曲进度
     */
    private void initProgressBar() {
        progressBar.setOnMouseClicked(progressBarHandle);
        progressBar.setOnMouseDragged(progressBarHandle);
    }

    /**
     * 初始化添加几首歌曲
     *
     * @return
     */
    private List<File> initMusicList() {
        String filepath = "E:\\4.音乐";
        List<File> musicList = new ArrayList<>();
        File file = new File(filepath);
        if (file.isDirectory()) {
            String[] filelist = file.list();
            if (filelist != null) {
                for (String fileName : filelist) {
                    if (fileName.endsWith("mp3")) {
                        File musicFile = new File(filepath + "\\" + fileName);
                        if (musicList.size() <= INIT_MUSIC_SIZE) {
                            musicList.add(musicFile);
                        }
                    }
                }
            }
        }
        return musicList;
    }

    /**
     * 初始设置皮肤弹窗
     */
    private void initSkinPopup() {
        skinPopup = new ContextMenu(new SeparatorMenuItem());
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/skin.fxml"));
        Parent skinRoot;
        try {
            skinRoot = fxmlLoader.load();
            ObservableMap<String, Object> namespace = fxmlLoader.getNamespace();
            ToggleGroup skinGroup = (ToggleGroup) namespace.get("skinGroup");

            // 添加点击皮肤换肤功能
            skinGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
                // 获取点击的是哪个按钮
                RXToggleButton toggleButton = (RXToggleButton) skinGroup.getSelectedToggle();
                // 之前设置了按钮的文本是css文件的名字
                String cssName = toggleButton.getText();
                String cssUrl = getClass().getResource("/css/" + cssName + ".css").toExternalForm();
                // 获取根节点,这里其实是player.fxml中的根节点,BorderPane,重置样式,就是换肤
                drawerPane.getScene().getRoot().getStylesheets().setAll(cssUrl);
                skinRoot.getScene().getRoot().getStylesheets().setAll(cssUrl);
                soundPopup.getScene().getRoot().getStylesheets().setAll(cssUrl);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        skinPopup.getScene().setRoot(skinRoot);
    }

    /**
     * 初始设置调节音量弹窗
     */
    private void initSoundPopup() {
        // 传递SeparatorMenuItem是为了防止contextMenu的size>0,否则不显示
        soundPopup = new ContextMenu(new SeparatorMenuItem());
        Parent soundRoot;
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/sound.fxml"));
            soundRoot = fxmlLoader.load();
            ObservableMap<String, Object> namespace = fxmlLoader.getNamespace();
            soundSlider = (Slider) namespace.get("soundSlider");
            Label soundNumLabel = (Label) namespace.get("soundNum");
            soundNumLabel.textProperty().bind(soundSlider.valueProperty().asString("%.0f%%")); // 格式化字符串显示,2个%才是真%
            // 绑定滑块的值，改变播放器声音大小
            soundSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(newValue.doubleValue() / 100);
                }
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // 最终，使用setRoot，上面的分割符也无效了
        soundPopup.getScene().setRoot(soundRoot);
        // 当声音面板显示时，绑定鼠标滚轮事件，调节音量
        soundRoot.setOnScroll(this::setVolumeByScroll);
    }

    /**
     * 初始设置歌曲列表页按钮动画
     */
    private void initSliderAnim() {
        showAnim = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(sliderPane.translateXProperty(), 0)));
        hideAnim = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(sliderPane.translateXProperty(), 300)));
        hideAnim.setOnFinished(event -> drawerPane.setVisible(false));
    }

    /**
     * 隐藏歌曲列表页
     *
     * @param event
     */
    @FXML
    void onHideSliderPaneAction(MouseEvent event) {
        showAnim.stop();
        hideAnim.play();
    }

    /**
     * 显示歌曲列表页
     *
     * @param event
     */
    @FXML
    void onShowSliderPaneAction(MouseEvent event) {
        drawerPane.setVisible(true);
        hideAnim.stop();
        showAnim.play();
    }

    /**
     * 退出程序
     *
     * @param event
     */
    @FXML
    void onCloseAction(MouseEvent event) {
        Platform.exit();
    }

    /**
     * 全屏
     *
     * @param event
     */
    @FXML
    void onFullAction(MouseEvent event) {
        Stage stage = findStage();
        stage.setFullScreen(!stage.isFullScreen());
    }

    /**
     * 最小化
     *
     * @param event
     */
    @FXML
    void onMiniAction(MouseEvent event) {
        Stage stage = findStage();
        stage.setIconified(true);
    }

    /**
     * 点击音量按钮,显示音量弹窗
     *
     * @param event
     */
    @FXML
    void onSoundPopupAction(MouseEvent event) {
        // 获取声音按钮位置
        Bounds bounds = soundBtn.localToScreen(soundBtn.getBoundsInLocal());

        soundPopup.show(findStage(), bounds.getMinX() - 20, bounds.getMinY() - 165);
    }


    /**
     * 点击换肤按钮,显示换肤弹窗
     *
     * @param event
     */
    @FXML
    void onSkinAction(MouseEvent event) {
        Bounds bounds = skinBtn.localToScreen(skinBtn.getBoundsInLocal());
        skinPopup.show(findStage(), bounds.getMaxX() - 135, bounds.getMaxY() + 10);
    }

    /**
     * 获取当前场景,也就是MusicPlayerApp中的primaryStage
     *
     * @return
     */
    private Stage findStage() {
        return (Stage) drawerPane.getScene().getWindow();
    }


    /**
     * 点击添加歌曲按钮事件
     *
     * @param event
     */
    @FXML
    void onAddMusicAction(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("mp3文件", "*.mp3"));
        ObservableList<File> items = listView.getItems();
        List<File> files = fileChooser.showOpenMultipleDialog(findStage());
        if (files != null) {
            // 如果列表中没有的才添加
            files.forEach(file -> {
                if (!items.contains(file)) {
                    items.add(file);
                }
            });
        }

    }


    /**
     * 播放和暂停音乐
     *
     * @param event
     */
    @FXML
    void onPlayAction(ActionEvent event) {
        int size = listView.getItems().size();
        if (size == 0) {
            return;
        }

        if (mediaPlayer == null) {
            int firstMusicIndex = 0;
            File firstMusicFile = listView.getItems().get(firstMusicIndex);
            // 手动选中第一首
            listView.getSelectionModel().select(firstMusicIndex);
            initMediaPlayer(firstMusicFile);
        }

        if (playBtn.isSelected()) {
            mediaPlayer.play();
        } else {
            mediaPlayer.pause();
        }
    }

    /**
     * 点击播放下一首
     *
     * @param event
     */
    @FXML
    void onPlayNextAction(MouseEvent event) {
        playNextMusic();
    }

    /**
     * 播放下一首
     */
    private void playNextMusic() {
        int size = listView.getItems().size();
        if (size < 2) {
            return;
        }
        int index = listView.getSelectionModel().getSelectedIndex();
        // 如果已经是最后一个了,则返回第一个
        index = (index == size - 1) ? 0 : index + 1;
        listView.getSelectionModel().select(index);
    }

    /**
     * 播放上一首
     *
     * @param event
     */
    @FXML
    void onPlayPrevAction(MouseEvent event) {
        int size = listView.getItems().size();
        if (size < 2) {
            return;
        }
        int index = listView.getSelectionModel().getSelectedIndex();
        // 如果已经是第一个了,则返回最后一个
        index = (index == 0) ? size - 1 : index - 1;
        listView.getSelectionModel().select(index);
    }


    @FXML
    void onVolumeScrollAction(ScrollEvent event) {
        if (soundBtn.isHover()) {
            setVolumeByScroll(event);
        }
    }

    private void setVolumeByScroll(ScrollEvent event) {
        // 只有在鼠标放在声音按钮上 或 声音弹窗显示的时候，滚动鼠标滚轮才生效
        if (soundBtn.isHover() || soundPopup.isShowing()){
            // 向上是32.0 下是 -32.0，想要鼠标滚动只调节5
            double current = soundSlider.getValue();
            soundSlider.setValue(current + (event.getDeltaY() / 32 * 5));
        }
    }
}
