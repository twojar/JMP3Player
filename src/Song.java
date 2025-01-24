import com.mpatric.mp3agic.Mp3File;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;

//Class used to describe a song
public class Song {
    private String songTitle;
    private String songArtist;
    private String songLength;
    private String filePath;
    private Mp3File mp3File;
    private double frameRatePerMilliseconds;

    public Song(String filePath) {
        this.filePath = filePath;
        try{
            mp3File = new Mp3File(filePath);
            frameRatePerMilliseconds = (double) mp3File.getFrameCount() / mp3File.getLengthInMilliseconds();
            songLength = convertToSongLengthFormat();
            // use the jaudiotagger library to create an audiofile obj to read mp3 file's info
            AudioFile audiofile = AudioFileIO.read(new File(filePath));

            //read through the metadata of the audio file
            Tag tag = audiofile.getTag();
            if (tag != null){
                songTitle = tag.getFirst(FieldKey.TITLE);
                songArtist = tag.getFirst(FieldKey.ARTIST);
            }
            else{
                // can't read through metadata
                songTitle = "Unknown";
                songArtist = "Unknown";

            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private String convertToSongLengthFormat(){
        long minutes = mp3File.getLengthInMilliseconds() / 60000;
        long seconds = mp3File.getLengthInMilliseconds() % 60;
        String formattedTime = String.format("%02d:%02d", minutes, seconds);
        return formattedTime;
    }

    // getters
    public String getSongTitle() {
        return songTitle;
    }

    public String getSongArtist() {
        return songArtist;
    }

    public String getSongLength() {
        return songLength;
    }

    public String getFilePath() {
        return filePath;
    }

    public Mp3File getMp3File() {
        return mp3File;
    }

    public double getFrameRatePerMilliseconds() {
        return frameRatePerMilliseconds;
    }
}
