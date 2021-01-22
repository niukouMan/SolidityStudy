package com.iccbank.demotoken.invoke;


import com.iccbank.demotoken.constants.Constant;
import com.iccbank.demotoken.dto.EthRawTransaction;
import com.iccbank.demotoken.utils.RetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static com.iccbank.demotoken.constants.Constant.INFURA_URL_TEST;

@Slf4j
@Service
public class EthInvoke {
    Web3j web3jClint = null;

    public EthInvoke() {
        String url = INFURA_URL_TEST;
        web3jClint = Web3j.build(new HttpService(url));
    }

    /**
     * 获取最新高度
     *
     * @return
     */
    public Long getBestBlockNumber() {
        try {
            BigInteger blockNumer = RetryUtil.<BigInteger>getADefaultRetryer()
                    .call(new Callable<BigInteger>() {
                        @Override
                        public BigInteger call() throws Exception {
                            EthBlockNumber send = web3jClint.ethBlockNumber().send();
                            BigInteger number = send.getBlockNumber();
                            return number;
                        }
                    });
            return blockNumer.longValue();
        } catch (Exception e) {
            log.error("【ETH getBlockNumber】：： 异常:", e);
        }
        return null;
    }

    /**
     * 通过高度获取区块
     *
     * @param blockNum
     * @return
     */
    public EthBlock getBlockByNumber(Long blockNum) {
        EthBlock ethBlock = null;
        try {
             ethBlock = RetryUtil.<EthBlock>getADefaultRetryer()
                    .call(new Callable<EthBlock>() {
                        @Override
                        public EthBlock call() throws Exception {
                            EthBlock ethBlock = web3jClint.ethGetBlockByNumber(DefaultBlockParameter.valueOf(new BigInteger(blockNum + "")), true).send();
                            return ethBlock;
                        }
                    });
        } catch (Exception e) {
            log.error("【ETH getBlockByNumber】：： blockNum:{},异常：{}", blockNum, e);
        }
        return ethBlock;
    }

    /**
     * 通过hash获取交易
     *
     * @param txid
     * @return
     */
    public EthTransaction getTransactionByHash(String txid) {
        EthTransaction ethTransaction = null;
        try {
            ethTransaction = RetryUtil.<EthTransaction>getADefaultRetryer()
                    .call(new Callable<EthTransaction>() {
                        @Override
                        public EthTransaction call() throws Exception {
                            EthTransaction send = web3jClint.ethGetTransactionByHash(txid).send();
                            return send;
                        }
                    });
        } catch (Exception e) {
            log.error("【ETH getTransactionByHash】 ：：txid:{},异常:", txid, e);
        }
        return ethTransaction;
    }

    /**
     * 获取交易状态
     *
     * @param txid
     */
    public String getTransactionStatus(String txid) {

        EthGetTransactionReceipt txReceipt = getTransactionRepeatByHash(txid);
        if (txReceipt == null) {
            return null;
        }
        Optional<TransactionReceipt> transactionReceipt = txReceipt.getTransactionReceipt();
        TransactionReceipt receipt = transactionReceipt.get();
        if (receipt == null) {
            return null;
        }
        String status = receipt.getStatus() != null ? Integer.valueOf(receipt.getStatus().substring(2), 10).toString() : null;
        return status;
    }

    /**
     * 通过hash获取交易
     *
     * @param txid
     * @return
     */
    public EthGetTransactionReceipt getTransactionRepeatByHash(String txid) {
        EthGetTransactionReceipt  transactionReceipt = null;
        try {
            transactionReceipt = RetryUtil.<EthGetTransactionReceipt>getADefaultRetryer()
                    .call(new Callable<EthGetTransactionReceipt>() {
                        @Override
                        public EthGetTransactionReceipt call() throws Exception {
                            EthGetTransactionReceipt ethTransactionReceipt = web3jClint.ethGetTransactionReceipt(txid).send();
                            return ethTransactionReceipt;
                        }
                    });
        } catch (Exception e) {
            log.error("【ETH getTransactionRepeatByHash】：：txid:{} 异常:{}", txid, e);
        }
        return transactionReceipt;
    }

    /**
     * 获取ETH balance
     *
     * @param address
     * @return
     */
    public BigInteger getEthBalance(String address) {

        BigInteger balance = BigInteger.ZERO;
        try {
            EthGetBalance ethGetBalance = web3jClint.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
            balance = ethGetBalance.getBalance();
        } catch (Exception e) {
            log.error("【ETH getEthBalance】 ：：address:{} 异常:{}", address, e);
        }

        return balance;
    }

    /**
     * 获取ERC20 Token balance
     *
     * @param walletAddress   钱包地址
     * @param contractAddress token合约地址
     * @return
     */
    public BigInteger getTokenBalance(String walletAddress, String contractAddress) {
        String methodName = "balanceOf";
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        Address address = new Address(walletAddress);
        inputParameters.add(address);

        TypeReference<Uint256> typeReference = new TypeReference<Uint256>() {
        };
        outputParameters.add(typeReference);
        Function function = new Function(methodName, inputParameters, outputParameters);
        String data = FunctionEncoder.encode(function);
        Transaction transaction = Transaction.createEthCallTransaction(walletAddress, contractAddress, data);

        EthCall ethCall;
        BigInteger balanceValue = BigInteger.ZERO;
        try {
            ethCall = web3jClint.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
            List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
            if (results != null && results.size() > 0) {
                balanceValue = (BigInteger) results.get(0).getValue();
            }
        } catch (Exception e) {
            log.error("【ETH getTokenBalance】：：walletAddress:{},contractAddress:{} 异常：{}", walletAddress, contractAddress, e);
        }
        return balanceValue;
    }

    /**
     * 构建eth交易
     *
     * @param fromAddress
     * @param toAddress
     * @param amount
     * @param gasPrice
     * @return
     */
    public RawTransaction createRawTransaction(String fromAddress, String toAddress, BigDecimal amount, BigInteger gasPrice, BigInteger nonce) {
        if (nonce == null) {
            nonce = getNonce(fromAddress);
        }

        Transaction transaction = Transaction.createEtherTransaction(fromAddress, nonce, gasPrice, null, toAddress, amount.toBigInteger());
        BigInteger gasLimit = getTransactionGasLimit(transaction, false);

        //检查余额是否充足
        BigInteger ethBalance = getEthBalance(fromAddress);
        BigInteger minerFee = gasLimit.multiply(gasPrice);
        if (ethBalance ==null ||ethBalance.compareTo(amount.toBigInteger().add(minerFee)) < 0) {
            log.error("【ETH 地址余额不足】：： from：{},to:{},amount:{},gasPrice:{},fee:{}", fromAddress, toAddress, amount, gasPrice, minerFee);
        }
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, toAddress, amount.toBigInteger(), "");
        log.info("【ETH 构建交易】 ：：createRawTransaction  fromAddress:{},  toAddress:{},  amount:{},  gasPrice:{}, nonce:{}", fromAddress, toAddress, amount, gasPrice, nonce);
        return rawTransaction;
    }

    /**
     * 构建erc20交易
     *
     * @param fromAddr
     * @param toAddr
     * @param contractAddr
     * @param amount
     * @param gasPrice
     * @return
     */
    public RawTransaction createErc20RawTransaction(String fromAddr, String toAddr, String contractAddr, BigDecimal amount, BigInteger gasPrice, BigInteger nonce) {
        if (nonce == null) {
            nonce = getNonce(fromAddr);
        }
        // 构建方法调用信息
        String method = "transfer";

        // 构建输入参数
        List<Type> inputArgs = new ArrayList<>();
        inputArgs.add(new Address(toAddr));
        inputArgs.add(new Uint256(amount.toBigInteger()));

        // 合约返回值容器
        List<TypeReference<?>> outputArgs = new ArrayList<>();
        String funcABI = FunctionEncoder.encode(new Function(method, inputArgs, outputArgs));
        Transaction transaction = Transaction.createFunctionCallTransaction(fromAddr, nonce, gasPrice, null, contractAddr, funcABI);
        BigInteger gasLimit = getTransactionGasLimit(transaction, true);

        //token余额
        BigInteger tokenBalance = getTokenBalance(fromAddr, contractAddr);
        if (tokenBalance ==null || tokenBalance.compareTo(amount.toBigInteger()) < 0) {
            log.error("【ERC20 token余额不足】：： from：{},to:{},amount:{},contract:{},token余额：{}", fromAddr, toAddr, amount, contractAddr,tokenBalance);
        }

        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, contractAddr, funcABI);
        log.info("【ETH 构建ETC20交易】 ：：createErc20RawTransaction  fromAddress:{},  toAddress:{}, contractAddr:{}, amount:{},  gasPrice:{}, nonce:{}", fromAddr, toAddr, contractAddr, amount, gasPrice, nonce);
        return rawTransaction;
    }

    /**
     * 获取账户nonce
     *
     * @param addr
     * @return
     */
    public BigInteger getNonce(String addr) {
        try {
            BigInteger transactionCount = RetryUtil.<BigInteger>getADefaultRetryer()
                    .call(new Callable<BigInteger>() {
                        @Override
                        public BigInteger call() throws Exception {
                            EthGetTransactionCount getNonce = web3jClint.ethGetTransactionCount(addr, DefaultBlockParameterName.PENDING).send();
                            return getNonce.getTransactionCount();
                        }
                    });
            return transactionCount;
        } catch (Exception e) {
            log.error("【ETH getNonce】：：addr:{}, 异常：{}", addr, e);
        }
        return null;
    }

    /**
     * 获取当前网络gasPrice
     *
     * @return
     */
    public BigInteger getCurrentGasPrice() {
        try {
            BigInteger gasPrice = RetryUtil.<BigInteger>getADefaultRetryer()
                    .call(new Callable<BigInteger>() {
                        @Override
                        public BigInteger call() throws Exception {
                            EthGasPrice send = web3jClint.ethGasPrice().send();
                            return  send.getGasPrice();
                        }
                    });
            return gasPrice;
        } catch (Exception e) {
            log.error("【ETH getGasPrice】：：异常：", e);
            return null;
        }
    }

    /**
     * 估算gaslimit
     *
     * @param transaction
     * @return
     */
    public BigInteger getTransactionGasLimit(Transaction transaction, boolean isErc20) {

        BigInteger gasLimit = null;
        try {
            EthEstimateGas ethEstimateGas = web3jClint.ethEstimateGas(transaction).send();
            if (StringUtils.isNotEmpty(ethEstimateGas.getResult())) {
                gasLimit = ethEstimateGas.getAmountUsed();

                if(gasLimit != null){
                    gasLimit = new BigDecimal(gasLimit).multiply(new BigDecimal("1.1")).toBigInteger();
                }
            }
        } catch (Exception e) {
            log.error("【ETH 估算gasLimit】：： 异常:", e);
            return null;
        }
        return gasLimit ;
    }

    /**
     * 签名交易
     *
     * @param ethRawTransaction
     * @param privateKey
     * @return
     */
    public byte[] signRawTransaction(EthRawTransaction ethRawTransaction, String privateKey) {
        if (privateKey.startsWith("0x")) {
            privateKey = privateKey.substring(2);
        }

        ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
        Credentials credentials = Credentials.create(ecKeyPair);

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                ethRawTransaction.getNonce(),
                ethRawTransaction.getGasPrice(),
                ethRawTransaction.getGasLimit(),
                ethRawTransaction.getTo(),
                ethRawTransaction.getValue(),
                ethRawTransaction.getData());
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        return signMessage;
    }

    /**
     * 广播交易
     *
     * @param signMessage
     * @return
     */
    public String sendRawTransaction(byte[] signMessage) {
        String signData = Numeric.toHexString(signMessage);
        if (!"".equals(signData)) {
            try {
                EthSendTransaction send = web3jClint.ethSendRawTransaction(signData).send();
                Response.Error error = send.getError();
                if (error != null) {
                    log.error("【ETH 广播交易】:: sendRawTransaction 异常 : code:{}, data:{}, message:{}", error.getCode(), error.getData(), error.getMessage());
                    return null;
                }
                String txHash = send.getTransactionHash();
                return txHash;
            } catch (Exception e) {
                log.error("【ETH 广播交易】：： sendRawTransaction 异常: ", e);
            }
        }
        return null;
    }

    /**
     * 获取合约信息
     * @param contractAddress
     * @param methodName
     * @return
     */
    public CompletableFuture<EthCall> callSimpleContractInfo(String contractAddress, String methodName) {
        Function function = new Function(methodName,
                new ArrayList(),
                Arrays.asList(new TypeReference<Utf8String>() {
                }));
        String encode = FunctionEncoder.encode(function);
        Transaction ethCallTransaction = Transaction.createEthCallTransaction(Constant.DEFAULT_WALLET_ADDRESS, contractAddress, encode);
        try {
            CompletableFuture<EthCall> ethCallCompletableFuture = web3jClint.ethCall(ethCallTransaction, DefaultBlockParameterName.LATEST).sendAsync();
            return ethCallCompletableFuture;
        } catch (Exception e) {
            log.error("查询合约信息异常： contractAddress:{} method:{}, error:",contractAddress,methodName,e);
             return null;
        }
    }


}
