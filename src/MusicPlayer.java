import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

import java.io.*;
import java.util.ArrayList;

public class MusicPlayer extends PlaybackListener {

    //used to update isPaused more synchronously
    private static final Object playSignal = new Object();

    //need reference so that we can update gui in this class
    private MusicPlayerGUI musicPlayerGUI;

    //need a way to store song details, so im creating a song class
    private Song currentSong;
    public Song getCurrentSong(){
        return currentSong;
    }

    private ArrayList<Song> playlist;

    //need to keep track of the index of the playlist
    private int currentPlaylistIndex;

    //use JLayer lib to create an AdvancedPlayer obj which will handle playing the music
    private AdvancedPlayer advancedPlayer;

    //pause boolean flag to indicate whether the player has been paused
    private boolean isPaused;


    //flag to indicate if song is finished playing
    private boolean songFinished;

    //flag to indicate if pressed next or prev buttons
    private boolean pressedNext, pressedPrev;


    //stores the last frame (time) of the song (used for pausing and resuming)
    private int currentFrame;
    public void setCurrentFrame(int frame){
        currentFrame = frame;
    }

    //track how many ms has passed since playing the song (used for updating the slider)
    private int currentTimeInMilli;
    public void setCurrentTimeInMilli(int timeInMilli){
        currentTimeInMilli = timeInMilli;
    }
    public int getCurrentTimeInMilli(){
        return currentTimeInMilli;
    }

    //constructor
    public MusicPlayer(MusicPlayerGUI musicPlayerGUI) {
        this.musicPlayerGUI = musicPlayerGUI;
    }

    public void loadSong(Song song){
        currentSong = song;
        playlist = null;

        //  stop the song if possible
        if (!songFinished)
            stopSong();

        //play the current song if not null
        if (currentSong != null){
            // reset frame
            currentFrame = 0;

            //reset current time in millis
            currentPlaylistIndex = 0;

            //update gui
            musicPlayerGUI.enablePauseDisablePlay();
            musicPlayerGUI.updatePlaybackSlider(currentSong);
            musicPlayerGUI.updateSongTitleAndArtist(currentSong);


            playCurrentSong();
        }
    }

    public void loadPlaylist(File playlistFile){
        playlist = new ArrayList<>();

        // store the paths from the text file into the playlist array
        try{
            FileReader fileReader = new FileReader(playlistFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            // read each line from the txt file and store the text into the songPath variable
            String songPath;
            while((songPath = bufferedReader.readLine()) != null){
                // create song object based on song path
                Song song = new Song(songPath);

                // add to the playlist array
                playlist.add(song);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (playlist.size() > 0){
            // reset playback slider
            musicPlayerGUI.setPlaybackSliderValue(0);
            currentTimeInMilli = 0;

            // update current song to the first song in the playlist
            currentSong = playlist.get(0);

            // start from the beginning frame
            currentFrame = 0;

            //update gui
            musicPlayerGUI.enablePauseDisablePlay();
            musicPlayerGUI.updatePlaybackSlider(currentSong);
            musicPlayerGUI.updateSongTitleAndArtist(currentSong);

            //start song
            playCurrentSong();
        }
    }


    public void pauseSong(){
        if (advancedPlayer != null) {
            // update isPaused flag
            isPaused = true;

            //then stop the player
            stopSong();

        }
    }

    public void stopSong(){
        if (advancedPlayer != null) {
            advancedPlayer.stop();
            advancedPlayer.close();
            advancedPlayer = null;
        }

    }

    public void nextSong(){
        // no need to goto next song if no playlist
        if (playlist == null) return;

        // dont do anything if at end of playlist
        if (currentPlaylistIndex + 1 > playlist.size() - 1) return;

        //update flag
        pressedNext = true;

        //  stop the song if possible
        if (!songFinished)
            stopSong();

        // increase current playlist index
        currentPlaylistIndex++;

        // update current song
        currentSong = playlist.get(currentPlaylistIndex);

        //reset frame
        currentFrame = 0;

        //reset current time in millis
        currentTimeInMilli = 0;

        //update gui
        musicPlayerGUI.enablePauseDisablePlay();
        musicPlayerGUI.updatePlaybackSlider(currentSong);
        musicPlayerGUI.updateSongTitleAndArtist(currentSong);

        //play song
        playCurrentSong();
    }

    public void prevSong(){
        // no need to goto next song if no playlist
        if (playlist == null) return;

        // dont do anything if at start of playlist
        if (currentPlaylistIndex - 1 < 0) return;

        //update flag
        pressedPrev = true;

        //  stop the song if possible
        if (!songFinished)
            stopSong();

        // decrement current playlist index
        currentPlaylistIndex--;

        // update current song
        currentSong = playlist.get(currentPlaylistIndex);

        //reset frame
        currentFrame = 0;

        //reset current time in millis
        currentTimeInMilli = 0;

        //update gui
        musicPlayerGUI.enablePauseDisablePlay();
        musicPlayerGUI.updatePlaybackSlider(currentSong);
        musicPlayerGUI.updateSongTitleAndArtist(currentSong);

        //play song
        playCurrentSong();
    }

    public void playCurrentSong(){
        try{
            if (currentSong != null){
                //read mp3 audio data
                FileInputStream fileInputStream = new FileInputStream(currentSong.getFilePath());
                BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

                //create a new advanced player
                advancedPlayer = new AdvancedPlayer(bufferedInputStream);
                advancedPlayer.setPlayBackListener(this);

                // start music
                startMusicThread();

                // start playback slider thread
                startPlaybackSliderThread();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    //thread that will handle playing music
    private void startMusicThread(){
        new Thread(new Runnable() {
            public void run() {
                try{
                    if (isPaused){
                        synchronized (playSignal){
                            // update flag
                            isPaused = false;

                            //notify the other thread to continue (makes sure that isPaused is updated to false)
                            playSignal.notify();
                        }
                        //resume music from last frame
                        advancedPlayer.play(currentFrame, Integer.MAX_VALUE);
                    } else{
                        //play music from start
                        advancedPlayer.play();
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

    }

    //create thread that will handle updating the slider
    private void startPlaybackSliderThread(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isPaused){
                    try{
                        // wait till it gets notified by other thread to continue
                        // makes sure that isPaused boolean flag updates to false before continuing
                        synchronized (playSignal){
                            playSignal.wait();
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
                while(!isPaused && !songFinished && !pressedNext && !pressedPrev){
                    try{
                        //increment current time milli
                        currentTimeInMilli++;

                        //calculate into frame value
                        int calculatedFrame = (int) ((double) currentTimeInMilli *  2.08 * currentSong.getFrameRatePerMilliseconds());

                        //update gui
                        musicPlayerGUI.setPlaybackSliderValue(calculatedFrame);

                        //mimic 1 ms using Thread.sleep
                        Thread.sleep(1);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    public void playbackFinished(PlaybackEvent evt) {
        //this method gets called when the song finishes or if player gets closed
        System.out.println("Playback finished");
        System.out.println("Stopped at: " + evt.getFrame());
        if (isPaused){
            currentFrame += (int)  ((double)evt.getFrame() * currentSong.getFrameRatePerMilliseconds());
        } else{

            // don't need to execute rest of code if next or prev buttons are pressed
            if(pressedNext || pressedPrev) return;

            // when song ends
            songFinished = true;
            if (playlist == null){
                //update gui
                musicPlayerGUI.enablePlayDisablePause();
            }else{
                // last song in playlist
                if(currentPlaylistIndex == playlist.size() - 1){
                    //update gui
                    musicPlayerGUI.enablePlayDisablePause();
                }else{
                    // goto the next song in the playlist
                    nextSong();
                }
            }

        }
    }

    @Override
    public void playbackStarted(PlaybackEvent evt) {
        // this method gets called in the beginning of the song
        System.out.println("Playback started");
        songFinished = false;
        pressedNext = false;
        pressedPrev = false;

    }
}
