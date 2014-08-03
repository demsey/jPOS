/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2020 jPOS Software SRL
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

package org.jpos.q2.iso;

import bsh.BshClassManager;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.UtilEvalError;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jdom2.Element;
import org.jpos.iso.ISOComponent;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOField;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.MUX;
import org.jpos.iso.packager.XMLPackager;
import org.jpos.q2.Q2;
import org.jpos.util.Log;
import org.jpos.util.NameRegistrar;

/**
 *
 * @author demsey
 */
public class NetMngHelper {

    enum TemplateKey {
        SESSION,    // Start session message
        SIGN_ON,    // Sign-on message
        SIGN_OFF,   // Sign-off message
        ECHO,       // Test echo message
    }

    final static List<String> DEFAULT_ACCEPTED_RESULT_CODES =
        new ArrayList<>(
            Arrays.asList(
                    "00"    //ISO8583v1987
                   ,"800"   //ISO8583v1993
                   ,"8000"  //ISO8583v2003
            )
        );

    Map<TemplateKey, ISOMsg>    templates;
    String                      muxName;
    Interpreter                 bsh;
    boolean                     signed;
    List<String>                acptRCs;

    NetMngHelper(String muxName) {
        this.muxName = muxName;
        this.acptRCs = DEFAULT_ACCEPTED_RESULT_CODES;
    }

    void setAcceptedRCs(List<String> acptRCs) {
        if (acptRCs != null && !acptRCs.isEmpty())
            this.acptRCs = acptRCs;
    }

    void initBSH(Q2 q2, Log log, Element persist) throws UtilEvalError, EvalError {
        bsh = new Interpreter();
        BshClassManager bcm = bsh.getClassManager();
        bcm.setClassPath(q2.getLoader().getURLs());
        bcm.setClassLoader(q2.getLoader());
        bsh.set("qbean", this);
        bsh.set("log", log);
        String init = persist.getChildTextTrim("init");
        if (init != null)
            bsh.eval(init);
    }

    void initMessages(Element messages) throws ISOException {
        if (messages == null)
            return;

        ISOPackager packager = new XMLPackager();
        templates = new ConcurrentHashMap<>();
        Element tmpl;
        tmpl = messages.getChild("session");
        if (tmpl!=null)
            templates.put(TemplateKey.SESSION, initMessage(tmpl, packager));

        tmpl = messages.getChild("echo");
        if (tmpl!=null)
            templates.put(TemplateKey.ECHO, initMessage(tmpl, packager));

        tmpl = messages.getChild("signon");
        if (tmpl!=null)
            templates.put(TemplateKey.SIGN_ON, initMessage(tmpl, packager));

        tmpl = messages.getChild("signoff");
        if (tmpl!=null)
            templates.put(TemplateKey.SIGN_OFF, initMessage(tmpl, packager));
    }

    boolean containsSession() {
        return templates.containsKey(TemplateKey.SESSION);
    }

    boolean isSigned() {
        return signed;
    }

    void setSigned(boolean signed) {
        this.signed = signed;
    }

    private ISOMsg initMessage(Element e, ISOPackager packager)
            throws ISOException {
        if (e == null)
            return null;

        ISOMsg m = new ISOMsg();
        m.setPackager(packager);
        m.unpack(e.getTextTrim().getBytes(StandardCharsets.UTF_8));
        return m;
    }

    private void sendTemplateMessage(ISOMsg template, ISOResponseListener rl) {
        if (template == null)
            return;

        try {
            ISOMsg message = (ISOMsg) template.clone();
            message = applyRequestProps(message);
            MUX mux = (MUX) NameRegistrar.get(muxName);
            mux.request(message, 10_000L, rl, null);
        } catch (Exception ex) {
//            getLog().warn("problem at sending", ex);
        }
    }

    void sendSession() {
        if (!templates.containsKey(TemplateKey.SESSION))
            return;

        ISOResponseListener rl = new ISOResponseListener() {

            @Override
            public void responseReceived(ISOMsg resp, Object handBack) {
                if (resp != null && acptRCs.contains(resp.getString(39)))
                    sendSignOn();
            }

            @Override
            public void expired(Object handBack) {}

        };
        sendTemplateMessage(templates.get(TemplateKey.SESSION), rl);
    }

    void sendSignOn() {
        if (!templates.containsKey(TemplateKey.SIGN_ON))
            return;

        ISOResponseListener rl = new ISOResponseListener() {

            @Override
            public void responseReceived(ISOMsg resp, Object handBack) {
                signed = resp != null && acptRCs.contains(resp.getString(39));
            }

            @Override
            public void expired(Object handBack) {
                signed = false;
            }
        };
        sendTemplateMessage(templates.get(TemplateKey.SIGN_ON), rl);
    }

    void sendSignOff() {
        if (!templates.containsKey(TemplateKey.SIGN_OFF))
            return;

        ISOResponseListener rl = new ISOResponseListener() {

            @Override
            public void responseReceived(ISOMsg resp, Object handBack) {
                if (resp != null && acptRCs.contains(resp.getString(39)))
                    signed = false;
            }

            @Override
            public void expired(Object handBack) {}

        };
        sendTemplateMessage(templates.get(TemplateKey.SIGN_OFF), rl);
    }

    void sendEcho() {
        if (!templates.containsKey(TemplateKey.ECHO))
            return;

        ISOResponseListener rl = new ISOResponseListener() {

            @Override
            public void responseReceived(ISOMsg resp, Object handBack) {}

            @Override
            public void expired(Object handBack) {}

        };
        sendTemplateMessage(templates.get(TemplateKey.ECHO), rl);
    }

    ISOMsg applyRequestProps(ISOMsg m)
        throws ISOException, EvalError {

        int maxField = m.getMaxField();
        for (int i=0; i <= maxField; i++) {
            if (m.hasField(i)) {
                ISOComponent c = m.getComponent(i);
                if (c instanceof ISOMsg) {
                    applyRequestProps((ISOMsg) c);
                } else if (c instanceof ISOField) {
                    String value = (String) c.getValue();
                    if (value.isEmpty())
                        continue;
                    try {
                        switch (value.charAt(0)) {
                          case '!':
                            m.set(i, bsh.eval(value.substring(1)).toString());
                            break;
                          case '@':
                            m.set(i,
                                ISOUtil.hex2byte(bsh.eval(value.substring(1)).toString())
                            );
                            break;
                        }
                    } catch (NullPointerException e) {
                        m.unset(i);
                    } catch (StringIndexOutOfBoundsException e) {}
                }
            }
        }
        return m;
    }

}
