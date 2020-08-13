package org.sn.core.db2;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sn.common.application.Application;
import org.sn.common.application.ApplicationFactory;
import org.sn.common.application.SnApplicationContext;
import org.sn.common.storage.leveldb.LevelDbDataSourceImpl;
import org.sn.common.utils.FileUtil;
import org.sn.core.Constant;
import org.sn.core.config.DefaultConfig;
import org.sn.core.config.args.Args;
import org.sn.core.db.CheckTmpStore;
import org.sn.core.db2.RevokingDbWithCacheNewValueTest.TestRevokingSnStore;
import org.sn.core.db2.RevokingDbWithCacheNewValueTest.TestSnapshotManager;
import org.sn.core.db2.SnapshotRootTest.ProtoCapsuleTest;
import org.sn.core.db2.core.ISession;
import org.sn.core.db2.core.SnapshotManager;
import org.sn.core.exception.BadItemException;
import org.sn.core.exception.ItemNotFoundException;

@Slf4j
public class SnapshotManagerTest {

  private SnapshotManager revokingDatabase;
  private SnApplicationContext context;
  private Application appT;
  private TestRevokingSnStore tronDatabase;

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", "output_revokingStore_test"},
        Constant.TEST_CONF);
    context = new SnApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    tronDatabase = new TestRevokingSnStore("testSnapshotManager-test");
    revokingDatabase.add(tronDatabase.getRevokingDB());
    revokingDatabase.setCheckTmpStore(context.getBean(CheckTmpStore.class));
  }

  @After
  public void removeDb() {
    Args.clearParam();
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    tronDatabase.close();
    FileUtil.deleteDir(new File("output_revokingStore_test"));
    revokingDatabase.getCheckTmpStore().getDbSource().closeDB();
    tronDatabase.close();
  }

  @Test
  public synchronized void testRefresh()
      throws BadItemException, ItemNotFoundException {
    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    revokingDatabase.setMaxFlushCount(0);
    revokingDatabase.setUnChecked(false);
    revokingDatabase.setMaxSize(5);
    ProtoCapsuleTest protoCapsule = new ProtoCapsuleTest("refresh".getBytes());
    for (int i = 1; i < 11; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("refresh" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        tronDatabase.put(protoCapsule.getData(), testProtoCapsule);
        tmpSession.commit();
      }
    }

    revokingDatabase.flush();
    Assert.assertEquals(new ProtoCapsuleTest("refresh10".getBytes()),
        tronDatabase.get(protoCapsule.getData()));
  }

  @Test
  public synchronized void testClose() {
    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    revokingDatabase.setMaxFlushCount(0);
    revokingDatabase.setUnChecked(false);
    revokingDatabase.setMaxSize(5);
    ProtoCapsuleTest protoCapsule = new ProtoCapsuleTest("close".getBytes());
    for (int i = 1; i < 11; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("close" + i).getBytes());
      try (ISession _ = revokingDatabase.buildSession()) {
        tronDatabase.put(protoCapsule.getData(), testProtoCapsule);
      }
    }
    Assert.assertEquals(null,
        tronDatabase.get(protoCapsule.getData()));

  }
}
