package vars.avplayer.jfx;

/**
 * Created by brian on 4/29/14.
 */
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ObservableMap;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;
import org.bushe.swing.event.EventBus;
import org.mbari.awt.image.ImageUtilities;
import org.mbari.movie.Timecode;
//import vars.annotation.ui.Lookup;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class JFXMovieFrameController implements Initializable {

    public static final Double DEFAULT_FRAME_RATE = 30D;

    @FXML
    private AnchorPane anchorPane;

    @FXML
    private TextField maxTimecodeTextField;

    @FXML
    private Slider scrubber;

    @FXML
    private MediaView mediaView;

    @FXML
    private TextField timecodeTextField;

    @FXML
    private Button playButton;

    private JFrame frame;
    private volatile MediaPlayer mediaPlayer;
    private final Timecode timecode = new Timecode();
    private Duration duration;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    /**
     * This needs to be called immediately after the object is intilized
     * @param frame The frame that contains the JavaFX node
     */
    protected void setFrame(final JFrame frame) {
        this.frame = frame;
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Platform.runLater(() -> {
                    getMediaView().setFitWidth(frame.getWidth());
                    getMediaView().setFitHeight(frame.getHeight());
                });
            }
        });
    }

    public AnchorPane getAnchorPane() {
        return anchorPane;
    }

    public MediaView getMediaView() {
        return mediaView;
    }

    public void setMediaLocation(String mediaLocation) {
        resetMediaView();

        Media media = new Media(mediaLocation);
        final ObservableMap<String,Object> metadata = media.getMetadata();
        timecode.setFrameRate((Double) metadata.getOrDefault("framerate", DEFAULT_FRAME_RATE));
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);

        // ---  Configure play button
        playButton.setOnAction((e) -> {
            MediaPlayer.Status status = mediaPlayer.getStatus();
            System.out.println(status);
            if (status == MediaPlayer.Status.UNKNOWN ||  status == MediaPlayer.Status.HALTED) {
                // Do nothing
                return;
            }

            if (status == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
            }
            else if (status == MediaPlayer.Status.PAUSED || status == MediaPlayer.Status.READY ||
                    status == MediaPlayer.Status.STOPPED) {
                mediaPlayer.play();
            }

        });

        // --- Configure MediaPlayer
        mediaPlayer.currentTimeProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                updateValues();
            }
        });

        mediaPlayer.setOnPlaying(() -> { playButton.setText("||"); });

        mediaPlayer.setOnPaused(() -> { playButton.setText(">"); });

        mediaPlayer.setOnReady(() -> {
            Media m = mediaPlayer.getMedia();
            duration = m.getDuration();
            Timecode tc = new Timecode(duration.toSeconds() * timecode.getFrameRate());
            maxTimecodeTextField.setText(tc.toString());
            updateValues();
            SwingUtilities.invokeLater(() -> frame.setSize(m.getWidth(), m.getHeight()));
        });


        mediaPlayer.setOnEndOfMedia(() -> { playButton.setText(">"); });

        // --- Configure Scrubber
        scrubber.valueProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (scrubber.isValueChanging()) {
                    // multiply duration by percentage calculated by slider position
                    mediaPlayer.seek(duration.multiply(scrubber.getValue() / 100D));
                }
            }
        });

    }

    private void resetMediaView() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaView.setMediaPlayer(null);
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    public BufferedImage frameCapture(File target) throws IOException, InterruptedException {

        BufferedImage bufferedImage = null;
        if (mediaView != null) {
            Platform.runLater(() -> {
                WritableImage image = mediaView.snapshot(new SnapshotParameters(), null);
                System.out.println("Saving image to " + target.getAbsolutePath());
                try {
                    ImageUtilities.saveImage(SwingFXUtils.fromFXImage(image, null), target);
                } catch (IOException e) {
                    // TODO fix exception handling
                    System.out.println("Failed to write " + target.getAbsolutePath());
                }
            });

            /*
                We are writing the image asynchronously (i.e. Platform.runLater()). Since we're not using any RX
                frameworks, we have to poll the image to see if it's done being written. If we try to read it while
                it's in a partially written state then we will get:

                java.lang.IndexOutOfBoundsException: null
                    at java.io.RandomAccessFile.readBytes(Native Method) ~[na:1.8.0_05]
                    at java.io.RandomAccessFile.read(RandomAccessFile.java:349) ~[na:1.8.0_05]
                    at javax.imageio.stream.FileImageInputStream.read(FileImageInputStream.java:117) ~[na:1.8.0_05]
                    at ...

             */
            Exception failure = null;
            for (int i = 0; i < 8; i++) {
                Thread.sleep(400L);
                try {
                    bufferedImage = vars.shared.ui.ImageUtilities.watchForAndReadNewImage(target);
                    break;
                }
                catch (IndexOutOfBoundsException e) {
                    failure = e;
                }
            }

            if (bufferedImage == null && failure != null) {
                // TODO report error
                //EventBus.publish(Lookup.TOPIC_NONFATAL_ERROR, failure);
            }


        }

        return bufferedImage;
    }

    protected  void updateValues() {
        if (timecodeTextField != null && scrubber != null && mediaPlayer != null) {
            Platform.runLater(() -> {
                Duration currentTime = mediaPlayer.getCurrentTime();
                double frames = currentTime.toSeconds() * timecode.getFrameRate();
                timecode.setFrames(frames);
                timecodeTextField.setText(timecode.toString());
                scrubber.setDisable(duration.isUnknown());
                if (!scrubber.isDisabled() && duration.greaterThan(Duration.ZERO) && !scrubber.isValueChanging()) {
                    scrubber.setValue(currentTime.divide(duration.toMillis()).toMillis() * 100D);
                }
            });
        }
    }
}

