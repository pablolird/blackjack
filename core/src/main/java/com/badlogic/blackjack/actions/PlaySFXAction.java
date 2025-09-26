package com.badlogic.blackjack.actions;

import com.badlogic.blackjack.AudioManager;
import com.badlogic.gdx.audio.Sound;

public class PlaySFXAction implements Action
{
    private final AudioManager audioManager;
    private final Sound sfx;
    private final float volume;

    public PlaySFXAction(AudioManager audioManager, Sound sfx, float volume)
    {
        this.audioManager = audioManager;
        this.sfx = sfx;
        this.volume = volume;
    }

    public PlaySFXAction(AudioManager audioManager, Sound sfx)
    {
        this.audioManager = audioManager;
        this.sfx = sfx;
        this.volume = 1.0f;
    }

    @Override
    public boolean update(float delta)
    {
        audioManager.playSound(sfx, volume);
        return true;
    }
}
