package com.badlogic.blackjack;

import com.badlogic.gdx.Audio;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

public class AudioManager
{
    private final Assets assets;
    private float masterSoundVolume = 1.0f;
    private float masterMusicVolume = 1.0f;
    private Music currentMusic;

    public AudioManager(Assets assets)
    {
        this.assets = assets;
    }

    public void playSound(Sound sound, float volume)
    {
        sound.play(this.masterSoundVolume * volume);
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
