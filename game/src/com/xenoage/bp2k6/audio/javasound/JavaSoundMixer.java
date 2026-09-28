/**
 * Bolzplatz 2006
 * Copyright (C) 2006 by Xenoage Software
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package com.xenoage.bp2k6.audio.javasound;

import com.xenoage.bp2k6.util.Logging;

import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;


/**
 * Software mixer for all sound effects.
 *
 * All playing sound effects are mixed into a single
 * audio line, instead of opening one line per sound effect.
 * On systems where every line is a separate audio server
 * stream (e.g. PipeWire), dozens of open lines slow down
 * the whole game.
 */
class JavaSoundMixer
  extends Thread
{

  //format of all mixed sounds: 44.1 kHz, 16 bit, stereo
  public static final float SAMPLE_RATE = 44100;
  public static final AudioFormat FORMAT = new AudioFormat(
    AudioFormat.Encoding.PCM_SIGNED, SAMPLE_RATE, 16, 2, 4, SAMPLE_RATE, false);

  //number of frames mixed at once
  private static final int CHUNK_FRAMES = 512;

  private SourceDataLine line;
  private ArrayList<Voice> voices = new ArrayList<Voice>();


  /**
   * A sound effect that is currently playing.
   */
  private class Voice
  {
    JavaSoundEffect effect;
    short[] samples;
    int position = 0;
    float volume;
  }


  /**
   * Creates the mixer and opens its audio line.
   */
  public JavaSoundMixer()
  {
    super("Sound Effect Mixer");
    setDaemon(true);
    try
    {
      line = (SourceDataLine) AudioSystem.getLine(
        new DataLine.Info(SourceDataLine.class, FORMAT));
      line.open(FORMAT, CHUNK_FRAMES * 4 * 8);
      line.start();
      start();
    }
    catch (Exception ex)
    {
      Logging.log(Logging.LEVEL_WARNINGS, this,
        "Could not open audio line for sound effects:");
      Logging.log(Logging.LEVEL_WARNINGS, this, ex);
      line = null;
    }
  }


  /**
   * Plays the given sound effect from the beginning.
   * If it is already playing, it is restarted.
   */
  public synchronized void play(JavaSoundEffect effect, short[] samples, float volume)
  {
    stop(effect);
    Voice voice = new Voice();
    voice.effect = effect;
    voice.samples = samples;
    voice.volume = volume;
    voices.add(voice);
  }


  /**
   * Stops the given sound effect, if it is playing.
   */
  public synchronized void stop(JavaSoundEffect effect)
  {
    for (int i = voices.size() - 1; i >= 0; i--)
      if (voices.get(i).effect == effect)
        voices.remove(i);
  }


  /**
   * Mixes the next chunk of all playing sounds into the given buffer.
   */
  private synchronized void mix(int[] mixBuffer)
  {
    java.util.Arrays.fill(mixBuffer, 0);
    for (int i = voices.size() - 1; i >= 0; i--)
    {
      Voice voice = voices.get(i);
      int count = Math.min(mixBuffer.length, voice.samples.length - voice.position);
      for (int s = 0; s < count; s++)
        mixBuffer[s] += (int) (voice.samples[voice.position + s] * voice.volume);
      voice.position += count;
      if (voice.position >= voice.samples.length)
        voices.remove(i);
    }
  }


  @Override public void run()
  {
    int[] mixBuffer = new int[CHUNK_FRAMES * 2];
    byte[] lineBuffer = new byte[CHUNK_FRAMES * 4];
    while (true)
    {
      mix(mixBuffer);
      for (int s = 0; s < mixBuffer.length; s++)
      {
        int v = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mixBuffer[s]));
        lineBuffer[s * 2] = (byte) v;
        lineBuffer[s * 2 + 1] = (byte) (v >> 8);
      }
      //blocks until the line has room, which paces this loop
      line.write(lineBuffer, 0, lineBuffer.length);
    }
  }


  /**
   * Converts 16 bit signed little endian PCM data with the given sample rate
   * and channel count into stereo samples in the format of this mixer.
   */
  public static short[] convert(byte[] data, int length, float sampleRate, int channels)
  {
    int frames = length / (2 * channels);
    int outFrames = (int) ((long) frames * SAMPLE_RATE / sampleRate);
    short[] ret = new short[outFrames * 2];
    for (int f = 0; f < outFrames; f++)
    {
      int src = (int) ((long) f * frames / outFrames);
      for (int c = 0; c < 2; c++)
      {
        int srcChannel = (channels == 1 ? 0 : c);
        int b = (src * channels + srcChannel) * 2;
        ret[f * 2 + c] = (short) ((data[b] & 0xff) | (data[b + 1] << 8));
      }
    }
    return ret;
  }

}
