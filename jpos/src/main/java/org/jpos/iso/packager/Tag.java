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

import org.jpos.iso.ISOUtil;

/**
 * ASCII TLV Tag.
 * <p>
 * Format:
 * tag id + value length + value
 * <ul >
 *  <li>tag id - identifier of the tag, should have length 2</li>
 *  <li>value length - length of the tag value, left zeropaded decimal 3 digit number</li>
 *  <li>value - value of the tag</li>
 * </ul>
 *
 * @author Grzegorz Wieczorek
 */
public class Tag {

    final int tagLen = 0x02;
    final int lenLen = 0x03;

    private String tagId;
    private String value;

    /**
     *
     *
     * @param tagId tag identifier
     * @param value tag value
     */
    public Tag(String tagId, String value) {
        if (tagId == null || tagId.length() != tagLen)
            throw new IllegalArgumentException("Invalid tag " + tagId);
        this.tagId = tagId;
        this.value = value;
    }

    /**
     * Form BER TLV for this tag.
     *
     * @return BER TLV string
     */
    public String getTLV() {
        if (value == null)
            return tagId + ISOUtil.zeropad(0, lenLen);
        return tagId + ISOUtil.zeropad(value.length(), lenLen) + value;
    }

    /**
     * Gets tag identifier.
     *
     * @return tag identifier
     */
    public String getTagId() {
        return tagId;
    }

    /**
     * Sets tag identifier.
     *
     * @param tagId tag identifier
     */
    public void setTagId(String tagId) {
        if (tagId == null || tagId.length() != tagLen)
            throw new IllegalArgumentException("Invalid tag " + tagId);
        this.tagId = tagId;
    }

    /**
     * Gets tag value.
     *
     * @return tag value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets tag value.
     *
     * @param value tag value
     */
    public void setValue(String value) {
        this.value = value;
    }

}
