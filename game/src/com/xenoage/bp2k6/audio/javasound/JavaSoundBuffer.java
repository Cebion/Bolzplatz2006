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
 * Implementation of a sound buffer
 * based on Java Sound with Vorbis SPI.
 * The sound is stored decoded, in the format of the
 * {@link JavaSoundMixer}.
 *
 * @author   Andreas Wenger
 */
public class JavaSoundBuffer
  implements SoundBuffer
{

  private String id;
  private short[] samples;

  
  /**
   * @param id    ID of the file (relative path)
   * @param samples  the decoded stereo samples
   */
  public JavaSoundBuffer(String id, short[] samples)
  {
    this.id = id;
    this.samples = samples;
  }

  
  /**
   * @return   the file ID of the sound
   */
  public String getID()
  {
    return id;
  }
  
  
  /**
   * Gets the decoded stereo samples of this SoundBuffer.
   */
  public short[] getSamples()
  {
    return samples;
  }
  
  
  /**
   * Clears this sound buffer.
   */
  public void clear()
  {
    samples = null;
  }

  
  @Override protected void finalize()
  {
    try { clear(); }
    catch (Exception ex) {}
  }

}
