/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2016 Alejandro P. Revilla
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jpos.iso.packager;

import java.nio.CharBuffer;
import java.util.HashMap;

/**
 * ASCII TLV Tag Map.
 *
 * @author Grzegorz Wieczorek
 */
public class TagMap extends HashMap<String,Tag> {

    final static String EXCEPTION_PREFIX = "BAD TLV FORMAT:";

    final int tagLen = 0x02;
    final int lenLen = 0x03;

    /**
     * Unpack string in BER TLV.
     *
     * @param data string containing tags in BER TLV
     * @throws IllegalArgumentException
     */
    public void unpack(String data) {
        CharBuffer buffer = CharBuffer.wrap(data);
        Tag currentTag;
        while (buffer.hasRemaining()) {
            currentTag = getTLVMsg(buffer);
            if (currentTag != null)
                put(currentTag.getTagId(), currentTag);
        }
    }

    /**
     * Pack BER TLV Tags.
     *
     * @return string containing tags in BER TLV Format
     */
    public String pack() {
        StringBuilder sb = new StringBuilder();
        for (Tag tag : values())
            sb.append(tag.getTLV());
        return sb.toString();
    }

    /**
     * Adds a new tag to map.
     *
     * @param tagId tag identifier
     * @param value tag value
     */
    public void addTag(String tagId, String value) {
        put(tagId, new Tag(tagId, value));
    }

    /**
     * Gets the value of the tag with given tagId from map.
     *
     * @param tagId tag identifier
     * @return value tag value
     */
    public String getTagValue(String tagId){
        Tag t = get(tagId);
        return t == null ? null : t.getValue();
    }

    /**
     * Chceck if the tag with given tag identifier is in this tag map.
     *
     * @param tagId tag identifier
     * @return true if this map contains the tag, otherwise return false
     */
    public boolean hasTag(String tagId) {
        return containsKey(tagId);
    }

    private Tag getTLVMsg(CharBuffer buffer) {
        if (!buffer.hasRemaining())
            return null;
        if (buffer.remaining() < tagLen)
            throw new IllegalArgumentException(String.format(
                    "%s tag id requires %d characters", EXCEPTION_PREFIX, tagLen)
            );
        String tagId = getStr(buffer, tagLen);
        if (buffer.remaining() < lenLen)
            throw new IllegalArgumentException(String.format(
                    "%s tag length requires %d digits", EXCEPTION_PREFIX, lenLen)
            );
        int len = Integer.parseInt(getStr(buffer, lenLen));
        if (buffer.remaining() < len)
            throw new IllegalArgumentException(
                    String.format("%s tag '%s' length '%03d' exceeds available"
                            + " data length '%03d'.", EXCEPTION_PREFIX
                            , tagId, len, buffer.remaining()
                    )
            );
        String value = getStr(buffer, len);
        return new Tag(tagId, value);
    }

    private String getStr(CharBuffer buffer, int len) {
        char[] ca = new char[len];
        buffer.get(ca);
        return new String(ca);
    }

}
