package vm.computer;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import vm.Main;

import java.net.URISyntaxException;

public class Player {
	public MediaPlayer
		computerRunning = fromResource("computer_running.mp3"),
		powerButtonClicked = fromResource("click.mp3");
	
	public MediaPlayer[] HDDPlayers = new MediaPlayer[7];

	public Player() {
		computerRunning.setOnEndOfMedia(() -> computerRunning.seek(Duration.ZERO));

		for (int i = 0; i < HDDPlayers.length; i++)
			HDDPlayers[i] = fromResource("hdd_access" + i + ".mp3");
	}

	public void stop(MediaPlayer what) {
		what.stop();
		what.seek(Duration.ZERO);
	}
	
	public void play(MediaPlayer what) {
		stop(what);
		what.play();
	}
	
	public static MediaPlayer fromResource(String name) {
		try {
			return new MediaPlayer(new Media(Main.class.getResource("resources/sounds/" + name).toURI().toString()));
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
}
