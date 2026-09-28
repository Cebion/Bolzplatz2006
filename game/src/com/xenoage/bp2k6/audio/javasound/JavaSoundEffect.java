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

import com.xenoage.bp2k6.audio.*;


/**
 * Implementation of a sound effect
 * based on Java Sound with Vorbis SPI.
 *
 * This class holds a reference to a sound buffer, its
 * position, velocity and volume. A sound effect is not
 * streamed but completely stored in a Java Sound buffer.
 * TODO
 *
 * @author Andreas Wenger
 */
public class JavaSoundEffect
  implements SoundEffect
{

  //buffer for sound data
  private JavaSoundBuffer soundBuffer;
  
  //sound effect properties
  private boolean loop = false;
  private float volume = 1;
  

  //friend-constructor: creation is only possible with SoundEngine
  JavaSoundEffect(JavaSoundBuffer soundBuffer)
  {
    this.soundBuffer = soundBuffer;
  }
  
  
  /**
   * Plays the given sound effect.
   */
  public void play()
  {
    //check if buffer is valid
    short[] samples = soundBuffer.getSamples();
    if (samples == null || samples.length == 0)
      return;
    
    //apply volume
    if (volume < 0.0002f)
      volume = 0.0002f;
    else if (volume > 1)
      volume = 1;
    
    //all sound effects are mixed into a single audio line
    JavaSoundEngine.getMixer().play(this, samples, volume);
  }
  
  
  /**
   * Stops this sound effect.
   */
  public void stop()
  {
    JavaSoundEngine.getMixer().stop(this);
  }
  

  /**
   * Sets the repeat mode on/off.
   */
  public void setLoop(boolean loop)
  {
    this.loop = loop;
  }
  

  /**
   * Sets the position of the sound effect. Must be called
   * before calling <code>play()</code>, otherwise it
   * has no effect.
   */
  public void setPosition(float x, float y, float z)
  {
    //TODO
    //not supported in this sound engine.
  }
  

  /**
   * Sets the volume of the sound effect. Must be called
   * before calling <code>play()</code>, otherwise it
   * has no effect.
   * @param volume   value between 0.0f (silence) and 1.0f (max)
   */
  public void setVolume(float volume)
  {
    this.volume = volume;
  }
  

  /**
   * Clears this sound effect.
   * This method has no effect, because the Java Sound
   * buffer is not cleared.
   */
  public void clear()
  {
  }

}