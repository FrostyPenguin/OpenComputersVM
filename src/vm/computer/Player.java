package vm.computer;

import vm.Main;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URISyntaxException;

public class Player {
    public MediaPlayer mediaPlayer;
    
    public void play() {
        mediaPlayer.play();
    }
    
    public void stop() {
        mediaPlayer.stop();
    }
    
    public void setRepeating(boolean value) {
        mediaPlayer.setCycleCount(value ? MediaPlayer.INDEFINITE : 1);
    }
    
    public Player(String soundName) {
        try {
            mediaPlayer = new MediaPlayer(new Media(Main.class.getResource("resources/sounds/" + soundName).toURI().toString()));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
