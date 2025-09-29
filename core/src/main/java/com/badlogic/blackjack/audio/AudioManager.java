package com.badlogic.blackjack.audio;

import com.badlogic.blackjack.Assets;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import java.util.Map;
import java.util.HashMap;

public class AudioManager
{
    private final Assets assets;
    private float masterSoundVolume = 1.0f;
    private float masterMusicVolume = 1.0f;
    private Music currentMusic;
    private final Map<SoundType, Sound> soundMap;

    public AudioManager(Assets assets)
    {
        this.assets = assets;
        soundMap = new HashMap<>();
        soundMap.put(SoundType.CARD_DEAL, assets.dealCardSFX);
        soundMap.put(SoundType.BET, assets.betSFX);
    }

    public void playSound(SoundType type, float volume)
    {
        Sound soundToPlay = soundMap.get(type);
        if (soundToPlay != null)
        {
            soundToPlay.play(masterSoundVolume * volume);
        }

    }

    public void playMusic(Music music, float volume)
    {
        if(this.currentMusic != null)
        {
            this.currentMusic.stop();
        }
        this.currentMusic = music;
        this.currentMusic.setLooping(true);
        this.currentMusic.setVolume(masterMusicVolume * volume);
        this.currentMusic.play();
    }

    public void playMusic(Music music)
    {
        playMusic(music, masterMusicVolume);
    }

    public void stopMusic()
    {
        if(this.currentMusic != null)
        {
            this.currentMusic.stop();
        }
    }

    public void setMasterSoundVolume(float volume)
    {
        this.masterSoundVolume = volume;
    }

    public void setMasterMusicVolume(float volume)
    {
        this.masterMusicVolume = volume;
    }
}
