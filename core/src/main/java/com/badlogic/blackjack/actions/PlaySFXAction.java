package com.badlogic.blackjack.actions;

import com.badlogic.blackjack.audio.AudioManager;
import com.badlogic.blackjack.audio.SoundType;

public class PlaySFXAction implements Action
{
    private final AudioManager audioManager;
    private final SoundType sfx;
    private final float volume;

    public PlaySFXAction(AudioManager audioManager, SoundType sfx, float volume)
    {
        this.audioManager = audioManager;
        this.sfx = sfx;
        this.volume = volume;
    }

    public PlaySFXAction(AudioManager audioManager, SoundType sfx)
    {
        this(audioManager, sfx, 1.0f);
    }

    @Override
    public boolean update(float delta)
    {
        audioManager.playSound(sfx, volume);
        return true;
    }
}
