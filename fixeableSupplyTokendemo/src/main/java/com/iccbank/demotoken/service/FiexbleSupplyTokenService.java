package com.iccbank.demotoken.service;

import com.iccbank.demotoken.invoke.EthInvoke;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.iccbank.demotoken.constants.Constant.TOKEN_ADDRESS;

@Service
@Slf4j
public class FiexbleSupplyTokenService implements ITokenService {

    String privateKey = "私钥";

    @Autowired
    private EthInvoke ethInvoke;

    @Override
    public void mint(String fromAddress,String address,BigInteger amount){
        BigInteger nonce = ethInvoke.getNonce(fromAddress);
        BigInteger gasPrice = ethInvoke.getCurrentGasPrice();
        String funcABI = mintFuncABI(address, amount);

        Transaction transaction = Transaction.createFunctionCallTransaction(fromAddress, nonce, gasPrice, null, TOKEN_ADDRESS, funcABI);
        BigInteger transactionGasLimit = ethInvoke.getTransactionGasLimit(transaction, true);
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, transactionGasLimit, TOKEN_ADDRESS, funcABI);

        byte[] signedTransaction = signTransaction(rawTransaction, privateKey);
        String txHash = broadcastTransaction(signedTransaction);
        System.out.println("mint txHash:  "+txHash);
    }

    /**
     * 铸造代币方法ABI
     *
     * @param address
     * @param amount
     * @return
     */
    private String mintFuncABI(String address, BigInteger amount) {
        // 构建方法调用信息
        String method = "mint";
        // 构建输入参数
        List<Type> inputArgs = new ArrayList<>();
        inputArgs.add(new Address(address));
        inputArgs.add(new Uint256(amount));
        // 合约返回值容器
        List<TypeReference<?>> outputArgs = new ArrayList<>();
        TypeReference<Bool> typeReference = new TypeReference<Bool>() {
        };
        outputArgs.add(typeReference);
        String funcABI = FunctionEncoder.encode(new Function(method, inputArgs, outputArgs));
        return funcABI;
    }

    /**
     * 燃烧代币
     * @param adminAddress
     * @param burnFromAddress
     * @param burnAmount
     */
    @Override
    public void burn(String adminAddress,String burnFromAddress,BigInteger burnAmount){
        BigInteger nonce = ethInvoke.getNonce(adminAddress);
        BigInteger gasPrice = ethInvoke.getCurrentGasPrice();
        String funcABI = burnFuncABI(burnFromAddress, burnAmount);

        Transaction transaction = Transaction.createFunctionCallTransaction(adminAddress, nonce, gasPrice, null, TOKEN_ADDRESS, funcABI);
        BigInteger transactionGasLimit = ethInvoke.getTransactionGasLimit(transaction, true);
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, transactionGasLimit, TOKEN_ADDRESS, funcABI);

        byte[] signedTransaction = signTransaction(rawTransaction, privateKey);
        String txHash = broadcastTransaction(signedTransaction);
        System.out.println("burn txHash:  "+txHash);
    }

    /**
     * 铸造代币方法ABI
     *
     * @param address
     * @param amount
     * @return
     */
    private String burnFuncABI(String address, BigInteger amount) {
        // 构建方法调用信息
        String method = "burn";
        // 构建输入参数
        List<Type> inputArgs = new ArrayList<>();
        inputArgs.add(new Address(address));
        inputArgs.add(new Uint256(amount));
        // 合约返回值容器
        List<TypeReference<?>> outputArgs = new ArrayList<>();
        TypeReference<Bool> typeReference = new TypeReference<Bool>() {
        };
        outputArgs.add(typeReference);
        String funcABI = FunctionEncoder.encode(new Function(method, inputArgs, outputArgs));
        return funcABI;
    }

    /**
     * 查询代币总供应量
     * @param tokenAddress
     */
    @Override
    public BigInteger totalSupply(String tokenAddress){
        CompletableFuture<EthCall> totalSupply = ethInvoke.callSimpleContractInfo(tokenAddress, "totalSupply");
        try {
            EthCall ethCall = totalSupply.get(5000, TimeUnit.SECONDS);
            String value = ethCall.getValue();
            if (StringUtils.isNotEmpty(value)){
              return  Numeric.toBigInt(value);
            }
        } catch (Exception e) {
           log.error("",e);
        }
        return null;
    }

    /**
     * 当前管理员
     * @return
     */
    @Override
    public String getCurrentAdmin(){
        CompletableFuture<EthCall> totalSupply = ethInvoke.callSimpleContractInfo(TOKEN_ADDRESS, "getOwner");
        try {
            EthCall ethCall = totalSupply.get(5000, TimeUnit.SECONDS);
            String value = ethCall.getValue();
            if (StringUtils.isNotEmpty(value)){
                return "0x"+ value.substring(value.length()-40);
            }
        } catch (Exception e) {
            log.error("",e);
        }
        return null;
    }


    /**
     * 修改合约管理员
     * 修改合约管理员需先暂停合约（pause方法）
     * @param oldAdmin
     * @param newAdmin
     * @return
     */
    @Override
    public boolean changeAdmin(String oldAdmin,String newAdmin){
        BigInteger nonce = ethInvoke.getNonce(oldAdmin);
        BigInteger gasPrice = ethInvoke.getCurrentGasPrice();
        String funcABI = chargeAdminFuncABI(newAdmin);

        Transaction transaction = Transaction.createFunctionCallTransaction(oldAdmin, nonce, gasPrice, null, TOKEN_ADDRESS, funcABI);
        BigInteger transactionGasLimit = ethInvoke.getTransactionGasLimit(transaction, true);
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, transactionGasLimit, TOKEN_ADDRESS, funcABI);

        byte[] signedTransaction = signTransaction(rawTransaction, privateKey);
        String txHash = broadcastTransaction(signedTransaction);
        System.out.println("changeAdmin txHash:  "+txHash);
        return true;
    }

    /**
     * 修改合约管理员
     *
     * @param newAdmin
     * @return
     */
    private String chargeAdminFuncABI(String newAdmin) {
        // 构建方法调用信息
        String method = "changeOwner";
        // 构建输入参数
        List<Type> inputArgs = new ArrayList<>();
        inputArgs.add(new Address(newAdmin));
        // 合约返回值容器
        List<TypeReference<?>> outputArgs = new ArrayList<>();
        TypeReference<Bool> typeReference = new TypeReference<Bool>() {
        };
        outputArgs.add(typeReference);
        String funcABI = FunctionEncoder.encode(new Function(method, inputArgs, outputArgs));
        return funcABI;
    }


    /**
     * 当前管理员
     * @return
     */
    @Override
    public boolean isPaused(){
        CompletableFuture<EthCall> totalSupply = ethInvoke.callSimpleContractInfo(TOKEN_ADDRESS, "isPaused");
        try {
            EthCall ethCall = totalSupply.get(5000, TimeUnit.SECONDS);
            String value = ethCall.getValue();
            if (StringUtils.isNotEmpty(value)){
                String substring = value.substring(value.length() - 1);
                return "0".equalsIgnoreCase(substring)?false:true;
            }
        } catch (Exception e) {
            log.error("",e);
            throw new RuntimeException(e.getMessage());
        }
        return false;
    }

    /**
     * 修改合约管理员
     * @param adminAddress
     * @return
     */
    @Override
    public boolean changePauseState(String adminAddress){
        BigInteger nonce = ethInvoke.getNonce(adminAddress);
        BigInteger gasPrice = ethInvoke.getCurrentGasPrice();
        String funcABI = chargePauseFuncABI();

        Transaction transaction = Transaction.createFunctionCallTransaction(adminAddress, nonce, gasPrice, null, TOKEN_ADDRESS, funcABI);
        BigInteger transactionGasLimit = ethInvoke.getTransactionGasLimit(transaction, true);
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, transactionGasLimit, TOKEN_ADDRESS, funcABI);

        byte[] signedTransaction = signTransaction(rawTransaction, privateKey);
        String txHash = broadcastTransaction(signedTransaction);
        System.out.println("changePauseState txHash:  "+txHash);
        return true;
    }

    /**
     * 修改合约运行状态
     *
     * @return
     */
    private String chargePauseFuncABI() {
        // 构建方法调用信息
        String method = "pause";
        // 构建输入参数
        List<Type> inputArgs = new ArrayList<>();
        // 合约返回值容器
        List<TypeReference<?>> outputArgs = new ArrayList<>();
        TypeReference<Bool> typeReference = new TypeReference<Bool>() {
        };
        outputArgs.add(typeReference);
        String funcABI = FunctionEncoder.encode(new Function(method, inputArgs, outputArgs));
        return funcABI;
    }

    /**
     * 签名交易
     * @param rawTransaction
     * @param privateKey
     * @return
     */
    private byte[] signTransaction(RawTransaction rawTransaction,String privateKey){
        ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
        Credentials credentials = Credentials.create(ecKeyPair);

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        return signedMessage;
    }

    /**
     * 广播交易
     * @param signedMessage
     * @return
     */
    private String broadcastTransaction(byte[] signedMessage){
       return ethInvoke.sendRawTransaction(signedMessage);
    }

}
