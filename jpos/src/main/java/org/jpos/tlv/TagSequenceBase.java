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

package org.jpos.tlv;

import org.jpos.iso.ISOBinaryField;
import org.jpos.iso.ISOComponent;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOField;
import org.jpos.iso.ISOMsg;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Vishnu Pillai
 */
public abstract class TagSequenceBase implements TagSequence {

    private final String tag;
    private final Map<String, List<TagValue>> tagMap = new TreeMap<>();
    private final List<TagValue> orderedList = new LinkedList<>();

    public TagSequenceBase() {
        this.tag = "Root";
    }

    protected TagSequenceBase(String tag) {
        this.tag = tag;
    }

    @Override
    public boolean isComposite() {
        return true;
    }

    @Override
    public Map<String, List<TagValue>> getChildren() {
        return tagMap;
    }

    @Override
    public synchronized void add(TagValue tagValue) {
        String tag = tagValue.getTag();
        List<TagValue> values = tagMap.get(tag);
        if (values == null) {
            values = new LinkedList();
            tagMap.put(tag, values);
        }
        values.add(tagValue);
        orderedList.add(tagValue);
    }

    @Override
    public List<TagValue> getOrderedList() {
        return orderedList;
    }

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public synchronized boolean hasTag(String tag) {
        return tagMap.containsKey(tag);
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public synchronized TagValue getFirst(String tag) {
        TagValue tagValue = null;
        LinkedList<TagValue> values = (LinkedList<TagValue>) tagMap.get(tag);
        if (values != null) {
            tagValue = values.peekFirst();
        }
        return tagValue;
    }

    @Override
    public synchronized List<TagValue> get(String tag) {
        return tagMap.get(tag);
    }

    @Override
    public synchronized Map<String, List<TagValue>> getAll() {
        return tagMap;
    }

    @Override
    public synchronized void writeTo(ISOMsg isoMsg) {
        int maxField = isoMsg.getMaxField();
        List<TagValue> tagValueList = getOrderedList();
        int fieldNumber = 0;
        for (TagValue tagValue : tagValueList) {
            fieldNumber++;
            Object value = tagValue.getValue();
            if (value == null)
                continue;

            ISOComponent subField;
            if (value instanceof byte[]) {
                subField = new ISOBinaryField(fieldNumber + maxField, (byte[]) value);
            } else if (value instanceof String) {
                subField = new ISOField(fieldNumber + maxField, (String) value);
            } else if (value instanceof TagSequence) {
                TagSequence subSequence = (TagSequence) tagValue;
                subField = new ISOMsg(fieldNumber + maxField);
                subSequence.writeTo((ISOMsg) subField);
            } else if (value instanceof ISOMsg) {
                ISOMsgTagValue subSequence = (ISOMsgTagValue) tagValue;
                subField = subSequence.getValue();
                subField.setFieldNumber(fieldNumber + maxField);
            } else {
                throw new IllegalArgumentException("Unknown TagValue subclass: "
                        + tagValue.getClass()
                );
            }
            try {
                isoMsg.set(new ISOTaggedField(tagValue.getTag(), subField));
            } catch (ISOException ex) {} //NOPMD: never happends
        }
    }

    @Override
    public synchronized void readFrom(ISOMsg isoMsg) {
        int maxField = isoMsg.getMaxField();
        int minField = -1;
        for (int i = 0; i <= maxField; i++) {
            ISOComponent child = isoMsg.getComponent(i);
            if (child instanceof ISOTaggedField) {
                minField = i;
                break;
            }
        }
        if (minField == -1) {
            //No TaggedFields to read
            return;
        }
        for (int i = minField; i <= maxField; i++) {
            ISOComponent child = isoMsg.getComponent(i);
            if (!(child instanceof ISOTaggedField))
                throw new IllegalArgumentException(String.format(
                        "Children after first %1$s should be instance of %1$s." +
                        " But field %2$d is not an %1$s", ISOTaggedField.class.getSimpleName(), i)
                );

            TagValue tagValue;
            ISOTaggedField taggedSubField = (ISOTaggedField) child;
            ISOComponent delegate = taggedSubField.getDelegate();
            if (delegate instanceof ISOMsg) {
                Map subChildren = delegate.getChildren();
                boolean allTaggedValue = true;
                for (Object subChild : subChildren.values()) {
                    if (!(subChild instanceof ISOTaggedField)) {
                        allTaggedValue = false;
                        break;
                    }
                }
                if (allTaggedValue) {
                    tagValue = createTagValueSequence(taggedSubField.getTag());
                    ((TagSequence) tagValue).readFrom((ISOMsg) delegate);
                } else {
                    tagValue = new ISOMsgTagValue(getTag(), isoMsg);
                }
            } else if (delegate instanceof ISOBinaryField) {
                tagValue = createBinaryTagValuePair(taggedSubField.getTag(), taggedSubField.getBytes());
            } else if (delegate instanceof ISOField) {
                tagValue = createLiteralTagValuePair(taggedSubField.getTag(), taggedSubField.getValue().toString());
            } else {
                throw new IllegalArgumentException("Unknown ISOComponent subclass"
                        + " in ISOTaggedField: " + delegate.getClass()
                );
            }
            this.add(tagValue);
        }

    }

    protected abstract TagSequence createTagValueSequence(String tag);

    protected abstract TagValue createLiteralTagValuePair(String tag, String value);

    protected abstract TagValue createBinaryTagValuePair(String tag, byte[] value);

}
