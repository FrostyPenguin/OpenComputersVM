package vm.computer;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import vm.Main;

import java.net.URISyntaxException;
import java.util.ArrayList;

public class Player {
	private static final long HDDSoundNextDelay = 500;
	
	public MediaPlayer computerRunning, powerButtonClicked;
	public MediaPlayer[] HDDPlayers = new MediaPlayer[7];

	private ArrayList<MediaPlayer> list = new ArrayList<>();
	private long HDDSoundNextPlay = 0;
	private int HDDSoundIndex = 0;


	public Player() {
		computerRunning = fromResource("computer_running.mp3");
		powerButtonClicked = fromResource("click.mp3");
		computerRunning.setOnEndOfMedia(() -> computerRunning.seek(Duration.ZERO));

		for (int i = 0; i < HDDPlayers.length; i++)
			HDDPlayers[i] = fromResource("hdd_access" + i + ".mp3");
	}

	public MediaPlayer fromResource(String name) {
		try {
			MediaPlayer mediaPlayer = new MediaPlayer(new Media(Main.class.getResource("resources/sounds/" + name).toURI().toString()));
			list.add(mediaPlayer);
			return mediaPlayer;
		}
		catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void setVolume(double volume) {
		for (MediaPlayer player : list) {
			player.setVolume(volume);
		}
	}

	public void stop(MediaPlayer what) {
		what.stop();
		what.seek(Duration.ZERO);
	}
	
	public void play(MediaPlayer what) {
		stop(what);
		what.play();
	}
	
	public void playHDDSound() {
		long current = System.currentTimeMillis();
		if (HDDSoundNextPlay < current) {
			play(HDDPlayers[HDDSoundIndex]);

			HDDSoundNextPlay = current + HDDSoundNextDelay;
			if (++HDDSoundIndex >= HDDPlayers.length)
				HDDSoundIndex = 0;
		}
	}
}
