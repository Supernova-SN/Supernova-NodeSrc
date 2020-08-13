package org.sn.core.db;

import com.google.protobuf.ByteString;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sn.common.application.SnApplicationContext;
import org.sn.common.utils.ByteArray;
import org.sn.common.utils.FileUtil;
import org.sn.core.Constant;
import org.sn.core.capsule.AccountCapsule;
import org.sn.core.config.DefaultConfig;
import org.sn.core.config.args.Args;
import org.sn.protos.Protocol.AccountType;

public class AccountStoreTest {

  private static String dbPath = "output_AccountStore_test";
  private static String dbDirectory = "db_AccountStore_test";
  private static String indexDirectory = "index_AccountStore_test";
  private static SnApplicationContext context;
  private static AccountStore accountStore;
  private static final byte[] data = TransactionStoreTest.randomBytes(32);
  private static byte[] address = TransactionStoreTest.randomBytes(32);
  private static byte[] accountName = TransactionStoreTest.randomBytes(32);

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory
        },
        Constant.TEST_CONF
    );
    context = new SnApplicationContext(DefaultConfig.class);
  }

  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  @BeforeClass
  public static void init() {
    accountStore = context.getBean(AccountStore.class);
    AccountCapsule accountCapsule = new AccountCapsule(ByteString.copyFrom(address),
        ByteString.copyFrom(accountName),
        AccountType.forNumber(1));
    accountStore.put(data, accountCapsule);
  }

  @Test
  public void get() {
    //test get and has Method
    Assert
        .assertEquals(ByteArray.toHexString(address), ByteArray
            .toHexString(accountStore.get(data).getInstance().getAddress().toByteArray()))
    ;
    Assert
        .assertEquals(ByteArray.toHexString(accountName), ByteArray
            .toHexString(accountStore.get(data).getInstance().getAccountName().toByteArray()))
    ;
    Assert.assertTrue(accountStore.has(data));
  }
}