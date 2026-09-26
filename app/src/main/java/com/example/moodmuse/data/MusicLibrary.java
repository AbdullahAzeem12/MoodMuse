package com.example.moodmuse.data;

import com.example.moodmuse.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicLibrary {

    private static final Map<String, List<Song>> musicMap = new HashMap<>();

    static {
        // Happy Songs
        List<Song> happySongs = new ArrayList<>();
        happySongs.add(new Song("Happy Day", "Artist A", R.drawable.happy_song_1));
        happySongs.add(new Song("Good Vibes", "Artist B", R.drawable.happy_song_2));
        musicMap.put("Happy", happySongs);

        // Sad Songs
        List<Song> sadSongs = new ArrayList<>();
        sadSongs.add(new Song("Rainy Evening", "Artist C", R.drawable.sad_song_1));
        sadSongs.add(new Song("Lost in Thought", "Artist D", R.drawable.sad_song_2));
        musicMap.put("Sad", sadSongs);

        // Angry Songs
        List<Song> angrySongs = new ArrayList<>();
        angrySongs.add(new Song("Rage", "Artist E", R.drawable.angry_song_1));
        angrySongs.add(new Song("Unleashed", "Artist F", R.drawable.angry_song_2));
        musicMap.put("Angry", angrySongs);

        // Surprised Songs
        List<Song> surprisedSongs = new ArrayList<>();
        surprisedSongs.add(new Song("Unexpected", "Artist G", R.drawable.surprised_song_1));
        surprisedSongs.add(new Song("Aha!", "Artist H", R.drawable.surprised_song_2));
        musicMap.put("Surprised", surprisedSongs);

        // Neutral Songs
        List<Song> neutralSongs = new ArrayList<>();
        neutralSongs.add(new Song("City Ambience", "Artist I", R.drawable.neutral_song_1));
        neutralSongs.add(new Song("Focus", "Artist J", R.drawable.neutral_song_2));
        musicMap.put("Neutral", neutralSongs);
        
        // Excited Songs
        List<Song> excitedSongs = new ArrayList<>();
        excitedSongs.add(new Song("Anticipation", "Artist K", R.drawable.excited_song_1));
        excitedSongs.add(new Song("Go!", "Artist L", R.drawable.excited_song_2));
        musicMap.put("Excited", excitedSongs);
        
        // Calm Songs
        List<Song> calmSongs = new ArrayList<>();
        calmSongs.add(new Song("Serenity", "Artist M", R.drawable.calm_song_1));
        calmSongs.add(new Song("Peaceful", "Artist N", R.drawable.calm_song_2));
        musicMap.put("Calm", calmSongs);
    }

    public static List<Song> getSongsForEmotion(String emotion) {
        return musicMap.getOrDefault(emotion, new ArrayList<>());
    }
}
