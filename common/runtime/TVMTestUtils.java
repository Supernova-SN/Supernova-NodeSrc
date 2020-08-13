package org.sn.common.runtime;

import static stest.sn.wallet.common.client.utils.PublicMethed.jsonStr2Abi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.sn.common.crypto.Hash;
import org.sn.common.storage.Deposit;
import org.sn.common.storage.DepositImpl;
import org.sn.core.Wallet;
import org.sn.core.capsule.BlockCapsule;
import org.sn.core.capsule.TransactionCapsule;
import org.sn.core.db.Manager;
import org.sn.core.db.TransactionTrace;
import org.sn.core.exception.ContractExeException;
import org.sn.core.exception.ContractValidateException;
import org.sn.core.exception.ReceiptCheckErrException;
import org.sn.core.exception.VMIllegalException;
import org.sn.protos.Contract;
import org.sn.protos.Contract.CreateSmartContract;
import org.sn.protos.Contract.TriggerSmartContract;
import org.sn.protos.Protocol.SmartContract;
import org.sn.protos.Protocol.Transaction;
import org.sn.protos.Protocol.Transaction.Contract.ContractType;
import stest.sn.wallet.common.client.Parameter.CommonConstant;
import stest.sn.wallet.common.client.WalletClient;
import stest.sn.wallet.common.client.utils.AbiUtil;


/**
 * Below functions mock the process to deploy, trigger a contract. Not consider of the transaction
 * process on real chain(such as db revoke...). Just use them to execute runtime actions and vm
 * commands.
 */
@Slf4j
public class TVMTestUtils {

  public static byte[] deployContractWholeProcessReturnContractAddress(String contractName,
      byte[] callerAddress,
      String ABI, String code, long value, long feeLimit, long consumeUserResourcePercent,
      String libraryAddressPair, DepositImpl deposit, BlockCapsule block)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    Transaction sn = generateDeploySmartContractAndGetTransaction(contractName, callerAddress, ABI,
        code, value, feeLimit, consumeUserResourcePercent, libraryAddressPair);
    processTransactionAndReturnRuntime(sn, deposit, block);
    return Wallet.generateContractAddress(sn);
  }

  public static byte[] deployContractWholeProcessReturnContractAddress(String contractName,
      byte[] callerAddress,
      String ABI, String code, long value, long feeLimit, long consumeUserResourcePercent,
      String libraryAddressPair, long tokenValue, long tokenId, DepositImpl deposit,
      BlockCapsule block)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    Transaction sn = generateDeploySmartContractAndGetTransaction(contractName, callerAddress, ABI,
        code, value, feeLimit, consumeUserResourcePercent, tokenValue, tokenId, libraryAddressPair);
    processTransactionAndReturnRuntime(sn, deposit, block);
    return Wallet.generateContractAddress(sn);
  }

  public static Runtime triggerContractWholeProcessReturnContractAddress(byte[] callerAddress,
      byte[] contractAddress, byte[] data, long callValue, long feeLimit, DepositImpl deposit,
      BlockCapsule block)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    Transaction sn = generateTriggerSmartContractAndGetTransaction(callerAddress, contractAddress,
        data, callValue, feeLimit);
    return processTransactionAndReturnRuntime(sn, deposit, block);
  }

  /**
   * return generated smart contract Transaction, just before we use it to broadcast and push
   * transaction
   */
  public static Transaction generateDeploySmartContractAndGetTransaction(String contractName,
      byte[] callerAddress,
      String ABI, String code, long value, long feeLimit, long consumeUserResourcePercent,
      String libraryAddressPair) {
    return generateDeploySmartContractAndGetTransaction(contractName, callerAddress, ABI, code,
        value, feeLimit, consumeUserResourcePercent,
        libraryAddressPair, 0);
  }

  public static Transaction generateDeploySmartContractAndGetTransaction(String contractName,
      byte[] callerAddress,
      String ABI, String code, long value, long feeLimit, long consumeUserResourcePercent,
      long tokenValue, long tokenId, String libraryAddressPair) {
    return generateDeploySmartContractAndGetTransaction(contractName, callerAddress, ABI, code,
        value, feeLimit, consumeUserResourcePercent,
        libraryAddressPair, 0, tokenValue, tokenId);
  }

  /**
   * return generated smart contract Transaction, just before we use it to broadcast and push
   * transaction
   */
  public static Transaction generateDeploySmartContractAndGetTransaction(String contractName,
      byte[] callerAddress,
      String ABI, String code, long value, long feeLimit, long consumeUserResourcePercent,
      String libraryAddressPair, long orginEngeryLimit) {

    CreateSmartContract contract = buildCreateSmartContract(contractName, callerAddress, ABI, code,
        value, consumeUserResourcePercent, libraryAddressPair, orginEngeryLimit);
    TransactionCapsule snCapWithoutFeeLimit = new TransactionCapsule(contract,
        ContractType.CreateSmartContract);
    Transaction.Builder transactionBuilder = snCapWithoutFeeLimit.getInstance().toBuilder();

    Transaction.raw.Builder rawBuilder = snCapWithoutFeeLimit.getInstance().getRawData()
        .toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transactionBuilder.setRawData(rawBuilder);
    Transaction sn = transactionBuilder.build();
    return sn;
  }

  public static Transaction generateDeploySmartContractAndGetTransaction(String contractName,
      byte[] callerAddress,
      String ABI, String code, long value, long feeLimit, long consumeUserResourcePercent,
      String libraryAddressPair, long orginEngeryLimit, long tokenValue, long tokenId) {

    CreateSmartContract contract = buildCreateSmartContract(contractName, callerAddress, ABI, code,
        value, consumeUserResourcePercent, libraryAddressPair, orginEngeryLimit, tokenValue,
        tokenId);
    TransactionCapsule snCapWithoutFeeLimit = new TransactionCapsule(contract,
        ContractType.CreateSmartContract);
    Transaction.Builder transactionBuilder = snCapWithoutFeeLimit.getInstance().toBuilder();

    Transaction.raw.Builder rawBuilder = snCapWithoutFeeLimit.getInstance().getRawData()
        .toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transactionBuilder.setRawData(rawBuilder);
    Transaction sn = transactionBuilder.build();
    return sn;
  }

  public static Transaction generateDeploySmartContractWithCreatorEnergyLimitAndGetTransaction(
      String contractName,
      byte[] callerAddress,
      String ABI, String code, long value, long feeLimit, long consumeUserResourcePercent,
      String libraryAddressPair, long creatorEnergyLimit) {

    CreateSmartContract contract = buildCreateSmartContractWithCreatorEnergyLimit(contractName,
        callerAddress, ABI, code,
        value, consumeUserResourcePercent, libraryAddressPair, creatorEnergyLimit);
    TransactionCapsule snCapWithoutFeeLimit = new TransactionCapsule(contract,
        ContractType.CreateSmartContract);
    Transaction.Builder transactionBuilder = snCapWithoutFeeLimit.getInstance().toBuilder();
    Transaction.raw.Builder rawBuilder = snCapWithoutFeeLimit.getInstance().getRawData()
        .toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transactionBuilder.setRawData(rawBuilder);
    Transaction sn = transactionBuilder.build();
    return sn;
  }

  /**
   * use given input Transaction,deposit,block and execute TVM  (for both Deploy and Trigger
   * contracts)
   */

  public static Runtime processTransactionAndReturnRuntime(Transaction sn,
      Deposit deposit, BlockCapsule block)
      throws ContractExeException, ContractValidateException, ReceiptCheckErrException, VMIllegalException {
    TransactionCapsule snCap = new TransactionCapsule(sn);
    deposit.commit();
    TransactionTrace trace = new TransactionTrace(snCap, deposit.getDbManager());
    // init
    trace.init(block);
    //exec
    trace.exec();

    trace.finalization();

    return trace.getRuntime();
  }

  public static Runtime processTransactionAndReturnRuntime(Transaction sn,
      Manager dbmanager, BlockCapsule block)
      throws ContractExeException, ContractValidateException, ReceiptCheckErrException, VMIllegalException {
    TransactionCapsule snCap = new TransactionCapsule(sn);

    TransactionTrace trace = new TransactionTrace(snCap, dbmanager);
    // init
    trace.init(block);
    //exec
    trace.exec();

    trace.finalization();

    return trace.getRuntime();
  }

  public static TransactionTrace processTransactionAndReturnTrace(Transaction sn,
      DepositImpl deposit, BlockCapsule block)
      throws ContractExeException, ContractValidateException, ReceiptCheckErrException, VMIllegalException {
    TransactionCapsule snCap = new TransactionCapsule(sn);
    deposit.commit();
    TransactionTrace trace = new TransactionTrace(snCap, deposit.getDbManager());
    // init
    trace.init(block);
    //exec
    trace.exec();

    trace.finalization();

    return trace;
  }


  public static TVMTestResult deployContractAndReturnTVMTestResult(String contractName,
      byte[] callerAddress,
      String ABI, String code, long value, long feeLimit, long consumeUserResourcePercent,
      String libraryAddressPair, Manager dbManager, BlockCapsule blockCap)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    Transaction sn = generateDeploySmartContractAndGetTransaction(contractName, callerAddress, ABI,
        code, value, feeLimit, consumeUserResourcePercent, libraryAddressPair);

    byte[] contractAddress = Wallet.generateContractAddress(sn);

    return processTransactionAndReturnTVMTestResult(sn, dbManager, blockCap)
        .setContractAddress(Wallet.generateContractAddress(sn));
  }

  public static TVMTestResult deployContractWithCreatorEnergyLimitAndReturnTVMTestResult(
      String contractName,
      byte[] callerAddress,
      String ABI, String code, long value, long feeLimit, long consumeUserResourcePercent,
      String libraryAddressPair, Manager dbManager, BlockCapsule blockCap, long creatorEnergyLimit)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    Transaction sn = generateDeploySmartContractWithCreatorEnergyLimitAndGetTransaction(
        contractName, callerAddress, ABI,
        code, value, feeLimit, consumeUserResourcePercent, libraryAddressPair, creatorEnergyLimit);

    byte[] contractAddress = Wallet.generateContractAddress(sn);

    return processTransactionAndReturnTVMTestResult(sn, dbManager, blockCap)
        .setContractAddress(Wallet.generateContractAddress(sn));
  }

  public static TVMTestResult triggerContractAndReturnTVMTestResult(byte[] callerAddress,
      byte[] contractAddress, byte[] data, long callValue, long feeLimit, Manager dbManager,
      BlockCapsule blockCap)
      throws ContractExeException, ReceiptCheckErrException, ContractValidateException, VMIllegalException {
    Transaction sn = generateTriggerSmartContractAndGetTransaction(callerAddress, contractAddress,
        data, callValue, feeLimit);
    return processTransactionAndReturnTVMTestResult(sn, dbManager, blockCap)
        .setContractAddress(contractAddress);
  }


  public static TVMTestResult processTransactionAndReturnTVMTestResult(Transaction sn,
      Manager dbManager, BlockCapsule blockCap)
      throws ContractExeException, ContractValidateException, ReceiptCheckErrException, VMIllegalException {
    TransactionCapsule snCap = new TransactionCapsule(sn);
    TransactionTrace trace = new TransactionTrace(snCap, dbManager);

    // init
    trace.init(blockCap);
    //exec
    trace.exec();

    trace.finalization();

    return new TVMTestResult(trace.getRuntime(), trace.getReceipt(), null);
  }

  public static CreateSmartContract buildCreateSmartContract(String contractName,
      byte[] address,
      String ABI, String code, long value, long consumeUserResourcePercent,
      String libraryAddressPair, long engeryLimit) {
    SmartContract.ABI abi = jsonStr2ABI(ABI);
    if (abi == null) {
      logger.error("abi is null");
      return null;
    }

    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(address));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    builder.setOriginEnergyLimit(engeryLimit);

    if (value != 0) {
      builder.setCallValue(value);
    }
    byte[] byteCode;
    if (null != libraryAddressPair) {
      byteCode = replaceLibraryAddress(code, libraryAddressPair);
    } else {
      byteCode = Hex.decode(code);
    }

    builder.setBytecode(ByteString.copyFrom(byteCode));
    return CreateSmartContract.newBuilder().setOwnerAddress(ByteString.copyFrom(address)).
        setNewContract(builder.build()).build();
  }

  public static CreateSmartContract buildCreateSmartContract(String contractName,
      byte[] address,
      String ABI, String code, long value, long consumeUserResourcePercent,
      String libraryAddressPair, long engeryLimit, long tokenValue, long tokenId) {
    SmartContract.ABI abi = jsonStr2ABI(ABI);
    if (abi == null) {
      logger.error("abi is null");
      return null;
    }

    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(address));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    builder.setOriginEnergyLimit(engeryLimit);

    if (value != 0) {
      builder.setCallValue(value);
    }
    byte[] byteCode;
    if (null != libraryAddressPair) {
      byteCode = replaceLibraryAddress(code, libraryAddressPair);
    } else {
      byteCode = Hex.decode(code);
    }

    builder.setBytecode(ByteString.copyFrom(byteCode));
    return CreateSmartContract.newBuilder().setOwnerAddress(ByteString.copyFrom(address)).
        setCallTokenValue(tokenValue).setTokenId(tokenId).
        setNewContract(builder.build()).build();
  }

  /**
   * create the Contract Instance for smart contract.
   */
  public static CreateSmartContract buildCreateSmartContract(String contractName,
      byte[] address,
      String ABI, String code, long value, long consumeUserResourcePercent,
      String libraryAddressPair) {
    return buildCreateSmartContract(contractName,
        address,
        ABI, code, value, consumeUserResourcePercent,
        libraryAddressPair, 0);
  }

  public static CreateSmartContract buildCreateSmartContractWithCreatorEnergyLimit(
      String contractName,
      byte[] address,
      String ABI, String code, long value, long consumeUserResourcePercent,
      String libraryAddressPair, long creatorEnergyLimit) {
    SmartContract.ABI abi = jsonStr2ABI(ABI);
    if (abi == null) {
      logger.error("abi is null");
      return null;
    }

    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(address));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    builder.setOriginEnergyLimit(creatorEnergyLimit);

    if (value != 0) {
      builder.setCallValue(value);
    }
    byte[] byteCode;
    if (null != libraryAddressPair) {
      byteCode = replaceLibraryAddress(code, libraryAddressPair);
    } else {
      byteCode = Hex.decode(code);
    }

    builder.setBytecode(ByteString.copyFrom(byteCode));
    return CreateSmartContract.newBuilder().setOwnerAddress(ByteString.copyFrom(address)).
        setNewContract(builder.build()).build();
  }


  public static Transaction generateTriggerSmartContractAndGetTransaction(
      byte[] callerAddress, byte[] contractAddress, byte[] data, long callValue, long feeLimit) {

    TriggerSmartContract contract = buildTriggerSmartContract(callerAddress, contractAddress, data,
        callValue);
    TransactionCapsule snCapWithoutFeeLimit = new TransactionCapsule(contract,
        ContractType.TriggerSmartContract);
    Transaction.Builder transactionBuilder = snCapWithoutFeeLimit.getInstance().toBuilder();
    Transaction.raw.Builder rawBuilder = snCapWithoutFeeLimit.getInstance().getRawData()
        .toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transactionBuilder.setRawData(rawBuilder);
    Transaction sn = transactionBuilder.build();
    return sn;
  }

  public static Transaction generateTriggerSmartContractAndGetTransaction(
      byte[] callerAddress, byte[] contractAddress, byte[] data, long callValue, long feeLimit,
      long tokenValue, long tokenId) {

    TriggerSmartContract contract = buildTriggerSmartContract(callerAddress, contractAddress, data,
        callValue, tokenValue, tokenId);
    TransactionCapsule snCapWithoutFeeLimit = new TransactionCapsule(contract,
        ContractType.TriggerSmartContract);
    Transaction.Builder transactionBuilder = snCapWithoutFeeLimit.getInstance().toBuilder();
    Transaction.raw.Builder rawBuilder = snCapWithoutFeeLimit.getInstance().getRawData()
        .toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transactionBuilder.setRawData(rawBuilder);
    Transaction sn = transactionBuilder.build();
    return sn;
  }


  public static TriggerSmartContract buildTriggerSmartContract(byte[] address,
      byte[] contractAddress, byte[] data, long callValue) {
    Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(address));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(data));
    builder.setCallValue(callValue);
    return builder.build();
  }

  public static TriggerSmartContract buildTriggerSmartContract(byte[] address,
      byte[] contractAddress, byte[] data, long callValue, long tokenValue, long tokenId) {
    Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(address));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(data));
    builder.setCallValue(callValue);
    builder.setCallTokenValue(tokenValue);
    builder.setTokenId(tokenId);
    return builder.build();
  }

  private static byte[] replaceLibraryAddress(String code, String libraryAddressPair) {

    String[] libraryAddressList = libraryAddressPair.split("[,]");

    for (int i = 0; i < libraryAddressList.length; i++) {
      String cur = libraryAddressList[i];

      int lastPosition = cur.lastIndexOf(":");
      if (-1 == lastPosition) {
        throw new RuntimeException("libraryAddress delimit by ':'");
      }
      String libraryName = cur.substring(0, lastPosition);
      String addr = cur.substring(lastPosition + 1);
      String libraryAddressHex;
      try {
        libraryAddressHex = (new String(Hex.encode(WalletClient.decodeFromBase58Check(addr)),
            "US-ASCII")).substring(2);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);  // now ignore
      }
      String repeated = new String(new char[40 - libraryName.length() - 2]).replace("\0", "_");
      String beReplaced = "__" + libraryName + repeated;
      Matcher m = Pattern.compile(beReplaced).matcher(code);
      code = m.replaceAll(libraryAddressHex);
    }

    return Hex.decode(code);
  }

  private static SmartContract.ABI.Entry.EntryType getEntryType(String type) {
    switch (type) {
      case "constructor":
        return SmartContract.ABI.Entry.EntryType.Constructor;
      case "function":
        return SmartContract.ABI.Entry.EntryType.Function;
      case "event":
        return SmartContract.ABI.Entry.EntryType.Event;
      case "fallback":
        return SmartContract.ABI.Entry.EntryType.Fallback;
      default:
        return SmartContract.ABI.Entry.EntryType.UNRECOGNIZED;
    }
  }

  private static SmartContract.ABI.Entry.StateMutabilityType getStateMutability(
      String stateMutability) {
    switch (stateMutability) {
      case "pure":
        return SmartContract.ABI.Entry.StateMutabilityType.Pure;
      case "view":
        return SmartContract.ABI.Entry.StateMutabilityType.View;
      case "nonpayable":
        return SmartContract.ABI.Entry.StateMutabilityType.Nonpayable;
      case "payable":
        return SmartContract.ABI.Entry.StateMutabilityType.Payable;
      default:
        return SmartContract.ABI.Entry.StateMutabilityType.UNRECOGNIZED;
    }
  }

  public static SmartContract.ABI jsonStr2ABI(String jsonStr) {
    if (jsonStr == null) {
      return null;
    }

    JsonParser jsonParser = new JsonParser();
    JsonElement jsonElementRoot = jsonParser.parse(jsonStr);
    JsonArray jsonRoot = jsonElementRoot.getAsJsonArray();
    SmartContract.ABI.Builder abiBuilder = SmartContract.ABI.newBuilder();
    for (int index = 0; index < jsonRoot.size(); index++) {
      JsonElement abiItem = jsonRoot.get(index);
      boolean anonymous = abiItem.getAsJsonObject().get("anonymous") != null &&
          abiItem.getAsJsonObject().get("anonymous").getAsBoolean();
      boolean constant = abiItem.getAsJsonObject().get("constant") != null &&
          abiItem.getAsJsonObject().get("constant").getAsBoolean();
      String name = abiItem.getAsJsonObject().get("name") != null ?
          abiItem.getAsJsonObject().get("name").getAsString() : null;
      JsonArray inputs = abiItem.getAsJsonObject().get("inputs") != null ?
          abiItem.getAsJsonObject().get("inputs").getAsJsonArray() : null;
      JsonArray outputs = abiItem.getAsJsonObject().get("outputs") != null ?
          abiItem.getAsJsonObject().get("outputs").getAsJsonArray() : null;
      String type = abiItem.getAsJsonObject().get("type") != null ?
          abiItem.getAsJsonObject().get("type").getAsString() : null;
      boolean payable = abiItem.getAsJsonObject().get("payable") != null &&
          abiItem.getAsJsonObject().get("payable").getAsBoolean();
      String stateMutability = abiItem.getAsJsonObject().get("stateMutability") != null ?
          abiItem.getAsJsonObject().get("stateMutability").getAsString() : null;
      if (type == null) {
        logger.error("No type!");
        return null;
      }
      if (!type.equalsIgnoreCase("fallback") && null == inputs) {
        logger.error("No inputs!");
        return null;
      }

      SmartContract.ABI.Entry.Builder entryBuilder = SmartContract.ABI.Entry.newBuilder();
      entryBuilder.setAnonymous(anonymous);
      entryBuilder.setConstant(constant);
      if (name != null) {
        entryBuilder.setName(name);
      }

      /* { inputs : optional } since fallback function not requires inputs*/
      if (null != inputs) {
        for (int j = 0; j < inputs.size(); j++) {
          JsonElement inputItem = inputs.get(j);
          if (inputItem.getAsJsonObject().get("name") == null ||
              inputItem.getAsJsonObject().get("type") == null) {
            logger.error("Input argument invalid due to no name or no type!");
            return null;
          }
          String inputName = inputItem.getAsJsonObject().get("name").getAsString();
          String inputType = inputItem.getAsJsonObject().get("type").getAsString();
          SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param
              .newBuilder();
          JsonElement indexedObj = inputItem.getAsJsonObject().get("indexed");
          paramBuilder.setIndexed((indexedObj == null) ? false : indexedObj.getAsBoolean());
          paramBuilder.setName(inputName);
          paramBuilder.setType(inputType);
          entryBuilder.addInputs(paramBuilder.build());
        }
      }

      /* { outputs : optional } */
      if (outputs != null) {
        for (int k = 0; k < outputs.size(); k++) {
          JsonElement outputItem = outputs.get(k);
          if (outputItem.getAsJsonObject().get("name") == null ||
              outputItem.getAsJsonObject().get("type") == null) {
            logger.error("Output argument invalid due to no name or no type!");
            return null;
          }
          String outputName = outputItem.getAsJsonObject().get("name").getAsString();
          String outputType = outputItem.getAsJsonObject().get("type").getAsString();
          SmartContract.ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param
              .newBuilder();
          paramBuilder.setIndexed(false);
          paramBuilder.setName(outputName);
          paramBuilder.setType(outputType);
          entryBuilder.addOutputs(paramBuilder.build());
        }
      }

      entryBuilder.setType(getEntryType(type));
      entryBuilder.setPayable(payable);
      if (stateMutability != null) {
        entryBuilder.setStateMutability(getStateMutability(stateMutability));
      }

      abiBuilder.addEntrys(entryBuilder.build());
    }

    return abiBuilder.build();
  }


  public static byte[] parseABI(String selectorStr, String params) {
    if (params == null) {
      params = "";
    }
    byte[] selector = new byte[4];
    System.arraycopy(Hash.sha3(selectorStr.getBytes()), 0, selector, 0, 4);
    byte[] triggerData = Hex.decode(Hex.toHexString(selector) + params);
    return triggerData;
  }

  public static CreateSmartContract createSmartContract(byte[] owner, String contractName,
      String abiString, String code, long value, long consumeUserResourcePercent) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);

    SmartContract.ABI abi = jsonStr2Abi(abiString);
    if (abi == null) {
      return null;
    }
    byte[] codeBytes = Hex.decode(code);
    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(owner));
    builder.setBytecode(ByteString.copyFrom(codeBytes));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    if (value != 0) {
      builder.setCallValue(value);
    }
    CreateSmartContract contractDeployContract = CreateSmartContract.newBuilder()
        .setOwnerAddress(ByteString.copyFrom(owner)).setNewContract(builder.build()).build();
    return contractDeployContract;
  }

  public static TriggerSmartContract createTriggerContract(byte[] contractAddress, String method,
      String argsStr,
      Boolean isHex, long callValue, byte[] ownerAddress) {
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);

    byte[] owner = ownerAddress;
    byte[] input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));

    TriggerSmartContract.Builder builder = TriggerSmartContract
        .newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(input));
    builder.setCallValue(callValue);
    return builder.build();
  }
}
