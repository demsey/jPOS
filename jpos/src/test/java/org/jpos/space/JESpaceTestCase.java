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

package org.jpos.space;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;

import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.util.Profiler;
import org.jpos.iso.ISOUtil;
import org.jpos.util.TPS;

@SuppressWarnings("unchecked")
public class JESpaceTestCase extends TestCase {

    LocalSpace<String,Object> sp1;

    List<Long> t1 = new ArrayList<>();
    List<Long> t2 = new ArrayList<>();

    TPS tpsOut = new TPS(100L, false);
    TPS tpsIn  = new TPS(100L, false);

    class WriteSpaceTask implements Runnable {
        String key;

        WriteSpaceTask(String key) {
           this.key = key;
        }
        @Override
        public void run() {
            long stamp = System.nanoTime();
            for (int i = 0; i < COUNT; i++) {
                sp1.out(key, Boolean.TRUE);
                tpsOut.tick();
            }
            long stamp2 = System.nanoTime();
            t1.add(stamp2 - stamp);
            System.err.println("Write " + key + " out: "
                    + (stamp2 - stamp) / 1000000 + " " + tpsOut.toString()
            );
        }
    }

    class ReadSpaceTask implements Runnable {
        String key;

        ReadSpaceTask(String key) {
           this.key = key;
        }
        @Override
        public void run() {
            long stamp = System.nanoTime();
            for (int i = 0; i < COUNT; i++) {
                sp1.in(key);
                tpsIn.tick();
            }
            long stamp2 = System.nanoTime();
            t2.add(stamp2 - stamp);
            System.err.println("Read  " + key + "  in: "
                    + (stamp2 - stamp) / 1000000 + " " + tpsIn.toString()
            );
        }
    }


    public static final int COUNT = 100000;
    JESpace<String,Object> sp;
    @Override
    public void setUp () {
        sp = (JESpace<String,Object>) 
            JESpace.getSpace ("space-test", "build/resources/test/space-test");
    }
    public void testSimpleOut() throws Exception {
        Object o = Boolean.TRUE;
        sp.out ("testSimpleOut_Key", o);
        Object o1 = sp.in ("testSimpleOut_Key");
        assertTrue (o.equals (o1));
    }
    public void testOutRdpInpRdp() throws Exception {
        Object o = Boolean.TRUE;
        String k = "testOutRdpInpRdp_Key";
        sp.out (k, o);
        assertTrue (o.equals (sp.rdp (k)));
        assertTrue (o.equals (sp.rd  (k)));
        assertTrue (o.equals (sp.rd  (k, 1000)));
        assertTrue (o.equals (sp.inp (k)));
        assertTrue (sp.rdp (k) == null);
        assertTrue (sp.rd  (k, 100) == null);
    }
    public void testMultiKeyLoad() throws Exception {
        String s = "The quick brown fox jumped over the lazy dog";
        Profiler prof = new Profiler ();
        for (int i=0; i<COUNT; i++) {
            sp.out ("testMultiKeyLoad_Key" + Integer.toString (i), s);
            if (i % 100 == 0)
                prof.checkPoint ("out " + i);
        }
        // prof.dump (System.err, "MultiKeyLoad out >");
        prof = new Profiler ();
        for (int i=0; i<COUNT; i++) {
            assertTrue (s.equals (sp.in ("testMultiKeyLoad_Key" + Integer.toString (i))));
            if (i % 100 == 0)
                prof.checkPoint ("in " + i);
        }
        // prof.dump (System.err, "MultiKeyLoad in  >");
    }
    public void testSingleKeyLoad() throws Exception {
        String s = "The quick brown fox jumped over the lazy dog";
        String k = "testSingleKeyLoad_Key";
        Profiler prof = new Profiler ();
        for (int i=0; i<COUNT; i++) {
            sp.out (k, s);
            if (i % 100 == 0)
                prof.checkPoint ("out " + i);
        }
        // prof.dump (System.err, "SingleKeyLoad out >");
        prof = new Profiler ();
        for (int i=0; i<COUNT; i++) {
            assertTrue (s.equals (sp.in (k)));
            if (i % 100 == 0)
                prof.checkPoint ("in " + i);
        }
        // prof.dump (System.err, "SingleKeyLoad in  >");
        assertTrue (sp.rdp (k) == null);
    }
    public void testTemplate () throws Exception {
        String key = "TemplateTest_Key";
        sp.out (key, "Value 1");
        sp.out (key, "Value 2");
        sp.out (key, "Value 3");

        String k2r = (String)sp.rdp (new MD5Template (key, "Value 2"));
        assertEquals (k2r, "Value 2");

        String k2i = (String)sp.inp (new MD5Template (key, "Value 2"));
        assertEquals (k2i, "Value 2");
        assertEquals ("Value 1", (String) sp.inp (key));
        assertEquals ("Value 3", (String) sp.inp (key));
    }
    public void testPush() {
        sp.push ("PUSH", "ONE");
        sp.push ("PUSH", "TWO");
        sp.push ("PUSH", "THREE");
        sp.out  ("PUSH", "FOUR");
        assertEquals ("THREE", sp.rdp ("PUSH"));
        assertEquals ("THREE", sp.inp ("PUSH"));
        assertEquals ("TWO", sp.inp ("PUSH"));
        assertEquals ("ONE", sp.inp ("PUSH"));
        assertEquals ("FOUR", sp.inp ("PUSH"));
        assertNull (sp.rdp ("PUSH"));
    }
    public void testExist() {
        sp.out ("KEYA", Boolean.TRUE);
        sp.out ("KEYB", Boolean.TRUE);

        assertTrue (
            "existAny ([KEYA])",
            sp.existAny (new String[] { "KEYA" })
        );

        assertTrue (
            "existAny ([KEYB])",
            sp.existAny (new String[] { "KEYB" })
        );
        assertTrue (
            "existAny ([KEYA,KEYB])",
            sp.existAny (new String[] { "KEYA", "KEYB" })
        );
        assertFalse (
            "existAny ([KEYC,KEYD])",
            sp.existAny (new String[] { "KEYC", "KEYD" })
        );
    }
    public void testExistWithTimeout() {
        assertFalse (
            "existAnyWithTimeout ([KA,KB])",
            sp.existAny (new String[] { "KA", "KB" })
        );
        assertFalse (
            "existAnyWithTimeout ([KA,KB], delay)",
            sp.existAny (new String[] { "KA", "KB" }, 1000L)
        );
        new Thread() {
            public void run() {
                ISOUtil.sleep (1000L);
                sp.out ("KA", Boolean.TRUE);
            }
        }.start();
        long now = System.currentTimeMillis();
        assertTrue (
            "existAnyWithTimeout ([KA,KB], delay)",
            sp.existAny (new String[] { "KA", "KB" }, 2000L)
        );
        long elapsed = System.currentTimeMillis() - now;
        assertTrue ("delay was > 1000", elapsed > 900L);
        assertNotNull ("Entry should not be null", sp.inp ("KA"));
    }
    public void testByteArray() throws Exception {
        String S = "The quick brown fox jumped over the lazy dog";
        sp.out ("ByteArray", S.getBytes());
        assertEquals (S, new String ((byte[]) sp.inp ("ByteArray")));
    }
    public void testGC() throws Exception {
        sp.out ("A", "Entrywithtimeout", 1000L);
        sp.out ("B", "AnotherEntrywithtimeout", 1000L);
        sp.gc();
    }
    public void testPut () {
        sp.out ("PUT", "ONE");
        sp.out ("PUT", "TWO");
        sp.put ("PUT", "ZERO");
        assertEquals ("ZERO", sp.rdp ("PUT"));
        assertEquals ("ZERO", sp.inp ("PUT"));
        assertNull (sp.rdp ("PUT"));
    }
    public void testPersistentContext() throws Exception {
        Context ctx = new Context();
        ctx.put("P", "ABC", true);
        ISOMsg m = new ISOMsg("0800");
        m.set(11, "000001");
        ctx.put("ISOMSG", m, true);
        sp.out("CTX", ctx);
        assertNotNull("entry should not be null", sp.in("CTX"));
    }

    private void printAvg(List<Long> times, String prefix){
        long avg = 0;
        for (Long t : times)
            avg += t;
        if (avg != 0) {
            avg /= times.size();
            avg /= 1000000;
        }
        System.out.println(prefix + avg);
    }

    //@Test
    public void testReadPerformance() throws Throwable {
        sp1 = new TSpace<>();
    //    sp1 = new JESpace("space-test2", "build/resources/test/space-test");
        t1.clear();
        t2.clear();

        int size = 2;
        ExecutorService es = new ThreadPoolExecutor(size, Integer.MAX_VALUE,
                              30, TimeUnit.SECONDS, new SynchronousQueue());
        ((ThreadPoolExecutor) es).prestartAllCoreThreads();

        List<Future> fl = new ArrayList<>();
        for (int i = 0; i < size; i++)
            fl.add(es.submit(new WriteSpaceTask("PerformTask-" + i)));
        for (Future f : fl)
            f.get();
        printAvg(t1, "Avg. write: ");

        fl.clear();
        for (int i = 0; i < size; i++)
            fl.add(es.submit(new ReadSpaceTask("PerformTask-" + i)));
        for (Future f : fl)
            f.get();
        es.shutdown();
        printAvg(t2, "Avg. read : ");
    }

}
