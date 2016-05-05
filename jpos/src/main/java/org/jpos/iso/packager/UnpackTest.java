/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jpos.iso.packager;

import org.jpos.iso.ISOComponent;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.ISOUtil;
import org.jpos.tlv.ISOTaggedField;

/** 
 *
 * @author demsey
 */
public class UnpackTest {

    final static String CSSHI_PACKAGER_URI = "jar:packager/iso93csshi.xml";

    public static void main(String[] args) throws ISOException {
        final ISOPackager CSSHI_PACKAGER = new GenericPackager(CSSHI_PACKAGER_URI);
        ISOMsg msg = new ISOMsg("1200");
        msg.setPackager(CSSHI_PACKAGER);

        msg.set(2, "66677788822200041");
        msg.set(4, "000000015499");
        msg.set(7, "0428220821");
        msg.set(49, "985");
//        ISOComponent m123 = new ISOMsg(123);
//        msg.set(m123);
//        m123.set(new ISOTaggedField("008",m123));
        msg.set("123.8", "Kot");
        msg.set("123.10.1", "a");
        msg.set("123.10.2", "cdef");

        byte[] bytes = msg.pack();
        System.out.println();
        System.out.println(ISOUtil.hexdump(bytes));
        System.out.println();
        System.out.println(ISOUtil.hexString(bytes));

        bytes = ISOUtil.hex2byte("31323030D200000000008000000000000000002031373636363737373838383232323030303431303030303030303135343939303432383232303832313938353035343130303431616364656620202020202020202020202020202020202020202020202020202020202020202020202030383030334B6F74");
        msg = new ISOMsg();
        msg.setPackager(CSSHI_PACKAGER);
        msg.unpack(bytes);
        System.out.println();
        msg.dump(System.out, ">>>");
    }
}
