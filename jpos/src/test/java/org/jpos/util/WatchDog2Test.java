package org.jpos.util;

import org.jpos.iso.ISOUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link org.jpos.util.WatchDog2}.
 */
public class WatchDog2Test {

    static final int TASK_DURATION = 100;
    static final int TASK_PERIOD   = 50;

    TestTask testTask;

    class TestTask implements Runnable {

        int duration;
        int checkCNT = 0;

        TestTask(int duration) {
            this.duration = duration;
        }

        @Override
        public void run() {
            ISOUtil.sleep(duration);
            checkCNT++;
        }

        int getCheckValue() {
        return checkCNT;
        }
    }

    @BeforeEach
    void setUp() {
        testTask = new TestTask(TASK_DURATION);
    }

    @AfterEach
    public void tearDown() {
        testTask = null;
    }

  /**
   * Test testExecuteSingleTask.
   *
   * <p>Test ma za zadanie sprawdzić czy po starcie {@link pl.visiona.hi.util.WatchDog}
   * odczekaniu czasu po jakim uruchomi się zadanie {@link #TASK_PERIOD} oraz
   * czasu trwania zadania {@link #TASK_DURATION} zadanie wykona się przynajmniej raz.
   */
  @Test
  void testExecuteSingleTask() {
    WatchDog2 wd = new WatchDog2(testTask, TASK_PERIOD);
    ISOUtil.sleep(3*TASK_DURATION);
    wd.deactivate();
    assertTrue(testTask.getCheckValue() > 1);
  }

  /**
   * Test testExecuteParallelTask.
   *
   * <p>Test ma za zadanie sprawdzić czy po starcie wielu instancji
   * {@link pl.visiona.hi.util.WatchDog}. Zadania zostaną wykonane równolegle,
   * aby tak było każdy task z każdego WatchDog musi wykonać się conajmniej raz.
   */
  @Test
  void testExecuteParallelTask() throws Exception {
    TestTask t2 = new TestTask(TASK_PERIOD);
    WatchDog2 wd = new WatchDog2(testTask,TASK_PERIOD);
    WatchDog2 wd2 = new WatchDog2(t2,TASK_PERIOD);
    ISOUtil.sleep(3*TASK_DURATION);
    wd.deactivate();
    wd2.deactivate();
    assertTrue(testTask.getCheckValue() > 1);
    assertTrue(t2.getCheckValue() > 1);
  }

  /**
   * Test testImmediateDeactivate.
   *
   * <p>Test ma za zadanie sprawdzić czy po starcie {@link pl.visiona.hi.util.WatchDog}
   * i niezwłocznym wykonaniu metody {@link  pl.visiona.hi.util.WatchDog#deactivate()}
   * zadania zostaną anulowane i nie wykonają się ani razu.
   */
  @Test
  void testImmediateDeactivate() throws Exception {
    WatchDog2 wd = new WatchDog2(testTask, TASK_PERIOD);
    wd.deactivate();
    ISOUtil.sleep(3*TASK_DURATION);
    assertTrue(testTask.getCheckValue() < 1);
  }

  /**
   * Test testDelayDeactivate.
   *
   * <p>Test ma za zadanie sprawdzić czy po starcie {@link pl.visiona.hi.util.WatchDog}
   * odczekaniu takiego czasu abu uruchomiło się zadanie
   * (czas &gt {@link #TASK_PERIOD} && czas &lt {@link #TASK_PERIOD})
   * zadanie wykona się dokładnie jeden raz.
   */
  @Test
  void testDelayDeactivate() {
    int delay = (TASK_PERIOD >> 1) + TASK_PERIOD;
    WatchDog2 wd = new WatchDog2(testTask, TASK_PERIOD);
    ISOUtil.sleep(delay);
    wd.deactivate();
    ISOUtil.sleep(2*TASK_PERIOD);
    assertEquals(1, testTask.getCheckValue());
  }

}
