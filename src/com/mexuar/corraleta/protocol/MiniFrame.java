/*
 * AndroVoIP -- VoIP for Android.
 *
 * Copyright (C), 2006, Mexuar Technologies Ltd.
 * 
 * AndroVoIP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * AndroVoIP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with AndroVoIP.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mexuar.corraleta.protocol;

import com.mexuar.corraleta.util.*;
import java.io.*;

/**
 * Representation of a miniframe.
 *
 * <pre>
 *                      1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |F|     Source call number      |           time-stamp          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                                                               |
 * :                             Data                              :
 * |                                                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * @author <a href="mailto:thp@westhawk.co.uk">Tim Panton</a>
 * @version $Revision: 1.15 $ $Date: 2006/05/12 14:14:13 $
 */
public class MiniFrame extends Frame {
    private ByteBuffer _buff;

    /**
     * The outbound constructor.
     *
     * @param call The Call object
     */
    public MiniFrame(Call call) {
        _call = call;
        _fullBit = false;
    }


    /**
     * The inbound constructor.
     *
     * @param call The Call object
     * @param bs The incoming message bytes
     * @throws IllegalArgumentException The bytes do not represent a
     * miniframe
     */
    public MiniFrame(Call call, byte[] bs)
    throws IllegalArgumentException {
        ByteBuffer buf = ByteBuffer.wrap(bs);
        _sCall = buf.getShort();
        if (_sCall < 0) {
            _sCall = 0x7fff & _sCall;
            _fullBit = true;
            throw new IllegalArgumentException("Not a miniframe, but fullframe.");
        } else {
            _fullBit = false;
        }
        setTimestampVal(buf.getChar());
        _data = buf.slice();
        _call = call;
    }


    /**
     * ack is called to send any required response. This method is
     * empty.
     */
    void ack() {
    }


    /**
     * Sends a specified buffer. The buffer represents the Data field in
     * the frame. If sets the header fields and calls sendMe() that will
     * do the actual sending.
     *
     * @param buff The buff (data)
     * @see #sendMe()
     */
    public void sendMe(byte[] buff) {
        _buff = ByteBuffer.allocate(buff.length + 4);
        _buff.putChar((char) _sCall);
        _buff.putChar((char) (0xffff & getTimestampVal()));
        _buff.put(buff);
        sendMe();
    }


    /**
     * used by data suppliers for outbound messages... put your data
     * on the end of this...
     *
     * @return ByteBuffer
     */
    ByteBuffer getBuffer() {
        return _buff;
    }


    /**
     * arrived is called when a packet arrives.
     *
     * @throws IAX2ProtocolException
     */
    void arrived() throws IAX2ProtocolException {
        int fsz = _call.getFrameSz();
        byte[] bs = new byte[fsz];
        long ts = this.getTimestampVal();
        int dr = _data.remaining();
        if (dr < fsz) {
            Log.warn("buffer too short: " + dr + " not " + fsz);
        } else {
            _data.get(bs);
            try {
                _call.audioWrite(bs, ts);
            }
            catch (IOException ex) {
                Log.warn(ex.getMessage());
            }
        }
        Log.verb("got minframe " + ts);
    }


    /**
     * Sends this frame.
     *
     * @see #sendMe(byte[])
     * @see Call#send(ByteBuffer)
     */
    void sendMe() {
        _call.send(_buff);
    }

}

