package com.example.demo.invoke;

import com.example.demo.dto.EstimateGasLimitDto;
import com.example.demo.dto.TxGasFee;
import com.example.demo.utils.RetryUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.*;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.example.demo.constants.Constant.DEFAULT_WALLET_ADDRESS;
import static com.example.demo.constants.Constant.ETHDefaultGasLimit;

@Slf4j
public class MultiSignInvoke {

    EthInvoke ethInvoke = null;
    Web3j web3jClint = null;

    public MultiSignInvoke(String url){
        ethInvoke = new EthInvoke(url);
        web3jClint = Web3j.build(new HttpService(url));
    }

    /**
     * 构建ETH多签转账方法
     * @param multiSignContractAddress 多签合约地址
     * @param fromAddress
     * @param to
     * @param amount
     * @param array_v
     * @param array_r
     * @param array_s
     * @return
     */
    public RawTransaction createSpendTransaction(String multiSignContractAddress,
                                                 String fromAddress,
                                                 String to,
                                                 BigInteger amount,
                                                 List<BigInteger> array_v,
                                                 List<byte[]> array_r,
                                                 List<byte[]> array_s) {
        BigInteger nonce = ethInvoke.getNonce(fromAddress);
        String funcABI = buildSpendFuncABI(to,amount,array_v,array_r,array_s);

        BigInteger defaultGasLimit = new BigInteger(ETHDefaultGasLimit);

        EstimateGasLimitDto estimateGasLimitDto = EstimateGasLimitDto.builder()
                .fromAddr(fromAddress)
                .contractAddress(multiSignContractAddress)
                .nonce(nonce)
                .minerFee(new BigDecimal("0.001"))
                .ethValue(amount)
                .funcABI(funcABI)
                .defaultGasLimit(defaultGasLimit)
                .retry(true)
                .build();
        TxGasFee txGasFee = estimateTxFee(estimateGasLimitDto);
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, txGasFee.getGasPrice(), txGasFee.getGasLimit(), multiSignContractAddress, funcABI);
        return rawTransaction;
    }

    /**
     * 构建ETH多签转账方法
     * @param multiSignContractAddress 多签合约地址
     * @param fromAddress
     * @param destination
     * @param amount
     * @param array_v
     * @param array_r
     * @param array_s
     * @return
     */
    public RawTransaction createSpendErc20Transaction(String multiSignContractAddress,
                                                 String fromAddress,
                                                 String erc20contract,
                                                 String destination,
                                                 BigInteger amount,
                                                 List<BigInteger> array_v,
                                                 List<byte[]> array_r,
                                                 List<byte[]> array_s) {
        BigInteger nonce = ethInvoke.getNonce(fromAddress);
        //funABI
        String funcABI = buildSpendErc20FuncABI(destination,erc20contract,amount,array_v,array_r,array_s);

        BigInteger defaultGasLimit = new BigInteger(ETHDefaultGasLimit);
        EstimateGasLimitDto estimateGasLimitDto = EstimateGasLimitDto.builder()
                .fromAddr(fromAddress)
                .contractAddress(multiSignContractAddress)
                .nonce(nonce)
                .minerFee(new BigDecimal("0.001"))
                .ethValue(null)
                .funcABI(funcABI)
                .defaultGasLimit(defaultGasLimit)
                .retry(true)
                .build();
        TxGasFee txGasFee = estimateTxFee(estimateGasLimitDto);

        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, txGasFee.getGasPrice(), txGasFee.getGasLimit(),multiSignContractAddress,funcABI);
        return rawTransaction;
    }

    /**
     * 构建ETH多签转账方法
     * 合约方法签名
     * function spend(address destination, uint256 value, uint8[] vs, bytes32[] rs, bytes32[] ss);
     * @param destination 接收者地址
     * @param value 转账金额
     * @param array_v eip712签名的v集合
     * @param array_r eip712签名的r集合
     * @param array_s eip712签名的s集合
     * @return
     */
    private String buildSpendFuncABI(String destination, BigInteger value, List<BigInteger> array_v,List<byte[]> array_r,List<byte[]> array_s){
        List<Uint8> v_array = new ArrayList<>();
        for (BigInteger v:array_v) {
            v_array.add(new Uint8(v));
        }
        DynamicArray<Uint8> vs = new DynamicArray<Uint8>(Uint8.class, v_array);

        //EIP712  R
        List<Bytes32> r_array = new ArrayList<>();
        for (byte[] r:array_r) {
            r_array.add(new Bytes32(r));
        }
        DynamicArray<Bytes32> rs = new DynamicArray<Bytes32>(Bytes32.class, r_array);

        //EIP712  S
        List<Bytes32> s_array = new ArrayList<>();
        for (byte[] s:array_s) {
            s_array.add(new Bytes32(s));
        }
        DynamicArray<Bytes32> ss = new DynamicArray<Bytes32>(Bytes32.class, s_array);

        // 构建方法调用信息
        String method = "spend";
        // 构建输入参数
        List<Type> inputArgs = new ArrayList<>();
        inputArgs.add(new Address(destination));
        inputArgs.add(new Uint256(value));
        inputArgs.add(vs);
        inputArgs.add(rs);
        inputArgs.add(ss);
        // 合约返回值容器
        List<TypeReference<?>> outputArgs = new ArrayList<>();
        String funcABI = FunctionEncoder.encode(new Function(method, inputArgs, outputArgs));

        return funcABI;
    }

    /**
     * 构建ETH多签转账方法
     * 合约方法签名
     * function spendERC20(address destination, address erc20contract, uint256 value, uint8[] vs, bytes32[] rs, bytes32[] ss)
     * @param destination 接收者地址
     * @param value 转账金额
     * @param array_v 签名的v集合
     * @param array_r 签名的r集合
     * @param array_s 签名的s集合
     * @return
     */
    private String buildSpendErc20FuncABI(String destination,String erc20contract, BigInteger value, List<BigInteger> array_v,List<byte[]> array_r,List<byte[]> array_s){
        //EIP712  V
        List<Uint8> v_array = new ArrayList<>();
        for (BigInteger v:array_v) {
            v_array.add(new Uint8(v));
        }
        DynamicArray<Uint8> vs = new DynamicArray<Uint8>(Uint8.class, v_array);

        //EIP712  R
        List<Bytes32> r_array = new ArrayList<>();
        for (byte[] r:array_r) {
            r_array.add(new Bytes32(r));
        }
        DynamicArray<Bytes32> rs = new DynamicArray<Bytes32>(Bytes32.class, r_array);

        //EIP712  S
        List<Bytes32> s_array = new ArrayList<>();
        for (byte[] s:array_s) {
            s_array.add(new Bytes32(s));
        }
        DynamicArray<Bytes32> ss = new DynamicArray<Bytes32>(Bytes32.class, s_array);

        // 构建方法调用信息
        String method = "spendERC20";
        // 构建输入参数
        List<Type> inputArgs = new ArrayList<>();
        inputArgs.add(new Address(destination));
        inputArgs.add(new Address(erc20contract));
        inputArgs.add(new Uint256(value));
        inputArgs.add(vs);
        inputArgs.add(rs);
        inputArgs.add(ss);
        // 合约返回值容器
        List<TypeReference<?>> outputArgs = new ArrayList<>();
        String funcABI = FunctionEncoder.encode(new Function(method, inputArgs, outputArgs));

        return funcABI;
    }

    public RawTransaction createSpendErc20Transaction2(String multiSignContractAddress,
                                                      String fromAddress,
                                                      String erc20contract,
                                                      String destination,
                                                      BigInteger amount,
                                                      BigInteger array_v,
                                                      byte[] array_r,
                                                      byte[] array_s) {
        BigInteger nonce = ethInvoke.getNonce(fromAddress);
        //funABI
        String funcABI = buildSpendErc20TestFuncABI(destination,erc20contract,amount,array_v,array_r,array_s);

        BigInteger defaultGasLimit = new BigInteger(ETHDefaultGasLimit);
        EstimateGasLimitDto estimateGasLimitDto = EstimateGasLimitDto.builder()
                .fromAddr(fromAddress)
                .contractAddress(multiSignContractAddress)
                .nonce(nonce)
                .minerFee(new BigDecimal("0.001"))
                .ethValue(null)
                .funcABI(funcABI)
                .defaultGasLimit(defaultGasLimit)
                .retry(true)
                .build();
        TxGasFee txGasFee = estimateTxFee(estimateGasLimitDto);

        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, txGasFee.getGasPrice(), txGasFee.getGasLimit(),multiSignContractAddress,funcABI);
        return rawTransaction;
    }

    /**
     * 构建ETH多签转账方法
     * 合约方法签名
     * function spendERC20(address destination, address erc20contract, uint256 value, uint8[] vs, bytes32[] rs, bytes32[] ss)
     * @param destination 接收者地址
     * @param value 转账金额
     * @param v 签名的v集合
     * @param r 签名的r集合
     * @param s 签名的s集合
     * @return
     */
    private String buildSpendErc20TestFuncABI(String destination,String erc20contract, BigInteger value, BigInteger v,byte[] r,byte[] s){
        // 构建方法调用信息
        String method = "spendERC20Test";
        // 构建输入参数
        List<Type> inputArgs = new ArrayList<>();
        inputArgs.add(new Address(destination));
        inputArgs.add(new Address(erc20contract));
        inputArgs.add(new Uint256(value));
        inputArgs.add(new Uint8(v));
        inputArgs.add(new Bytes32(r));
        inputArgs.add(new Bytes32(s));
        // 合约返回值容器
        List<TypeReference<?>> outputArgs = new ArrayList<>();
        String funcABI = FunctionEncoder.encode(new Function(method, inputArgs, outputArgs));

        return funcABI;
    }


    public String signAndBroadcast( RawTransaction rawTransaction,String privateKey){
        if (privateKey.startsWith("0x")) {
            privateKey = privateKey.substring(2);
        }

        ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
        Credentials credentials = Credentials.create(ecKeyPair);
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, credentials);

        String txHash = ethInvoke.sendRawTransaction(signMessage);
        return txHash;
    }

    public Sign.SignatureData signMessage(String privateKey,byte[] message){
        if (privateKey.startsWith("0x")) {
            privateKey = privateKey.substring(2);
        }
        ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
        Credentials credentials = Credentials.create(ecKeyPair);
        Sign.SignatureData signatureData = Sign.signMessage(message, credentials.getEcKeyPair(),false);

        return signatureData;
    }

    /**
     *
     * 合约方法签名
     * bytes32 message = keccak256(abi.encodePacked(address(this), erc20Contract, destination, value, spendNonce));
     *
     * @param  multiSignContractAddr 多签合约地址
     */
    public byte[] generateMessageToSign(String multiSignContractAddr,String erc20Contract,String destination,BigInteger value){

        BigInteger spendNonce = getSpendNonce( multiSignContractAddr);
        String contractAddr = StringUtils.isEmpty(erc20Contract)? "0000000000000000000000000000000000000000":erc20Contract.substring(2).toLowerCase();
       String hashMessage = Numeric.toHexStringNoPrefix(Hash.sha3(Numeric.hexStringToByteArray(
                "000000000000000000000000" + multiSignContractAddr.substring(2).toLowerCase() +
                        "000000000000000000000000" + contractAddr +
                        "000000000000000000000000" + destination.substring(2).toLowerCase() +
                        Numeric.toHexStringNoPrefix(Numeric.toBytesPadded(value, 32)) +
                        Numeric.toHexStringNoPrefix(Numeric.toBytesPadded(spendNonce, 32)))));

         return  Numeric.hexStringToByteArray(hashMessage);
    }

    /**
     *
     * 合约方法签名
     * bytes32 message = keccak256(abi.encodePacked(address(this), erc20Contract, destination, value, spendNonce));
     * "\\x19Ethereum Signed Message:\n32"+
     * @param  multiSignContractAddr 多签合约地址
     */
    public byte[] generateMessageToSign2(String multiSignContractAddr,String erc20Contract,String destination,BigInteger value){

        BigInteger spendNonce = getSpendNonce( multiSignContractAddr);
        String contractAddr = "0x0".equals(erc20Contract)? "0000000000000000000000000000000000000000":erc20Contract.substring(2).toLowerCase();

        String data =
                "000000000000000000000000" + multiSignContractAddr.substring(2).toLowerCase() +
                "000000000000000000000000" + contractAddr +
                "000000000000000000000000" + destination.substring(2).toLowerCase() +
                Numeric.toHexStringNoPrefix(Numeric.toBytesPadded(value, 32)) +
                Numeric.toHexStringNoPrefix(Numeric.toBytesPadded(spendNonce, 32));
        String hashMessage = Hash.sha3(data);
        return Numeric.hexStringToByteArray(hashMessage) ;
    }

    /**
     *
     * 合约方法签名
     * bytes32 message = keccak256(abi.encodePacked(address(this), erc20Contract, destination, value, spendNonce));
     * "\\x19Ethereum Signed Message:\n32"+
     * @param  multiSignAddr 多签合约地址
     */
    public byte[] generateMessageToSign3(String multiSignAddr,String erc20Contract,String destination,BigInteger value){
        BigInteger spendNonce = getSpendNonce( multiSignAddr);
        String contractAddr = "0x0".equals(erc20Contract)? "0000000000000000000000000000000000000000":erc20Contract.substring(2).toLowerCase();
        String data =  "\\x19Ethereum Signed Message:\n32" +
                Numeric.toHexStringNoPrefix(Hash.sha3(Numeric.hexStringToByteArray(
                        "000000000000000000000000" + multiSignAddr.substring(2).toLowerCase() +
                                "000000000000000000000000" + contractAddr +
                                "000000000000000000000000" + destination.substring(2).toLowerCase() +
                                Numeric.toHexStringNoPrefix(Numeric.toBytesPadded(value, 32)) +
                                Numeric.toHexStringNoPrefix(Numeric.toBytesPadded(spendNonce, 32))))
                );
        String proofStr = Numeric.toHexStringNoPrefix(Hash.sha3(data.getBytes()));
        byte[] proofBytes = Numeric.hexStringToByteArray(proofStr);
        return proofBytes ;
    }


    public byte[] getEthereumMessageHash(byte[] message) {
        byte[] prefix = getEthereumMessagePrefix(message.length);

        byte[] result = new byte[prefix.length + message.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(message, 0, result, prefix.length, message.length);

        return Hash.sha3(result);
    }

    public byte[] getEthereumMessagePrefix(int messageLength) {
        return "\u0019Ethereum Signed Message:\n".concat(String.valueOf(messageLength)).getBytes();
    }

    /**
     *
     * 合约方法签名
     * bytes32 message = keccak256(abi.encodePacked(address(this), erc20Contract, destination, value, spendNonce));
     * "\\x19Ethereum Signed Message:\n32"+
     * @param  multiSignAddr 多签合约地址
     */
    public byte[] generateMessageToSign4(String multiSignAddr,String erc20Contract,String destination,BigInteger value){
        BigInteger spendNonce = getSpendNonce( multiSignAddr);
        String contractAddr = StringUtils.isEmpty(erc20Contract)? "0000000000000000000000000000000000000000":erc20Contract.substring(2).toLowerCase();

        String paramStr = "000000000000000000000000" + multiSignAddr.substring(2).toLowerCase() +
                "000000000000000000000000" + contractAddr +
                "000000000000000000000000" + destination.substring(2).toLowerCase() +
                Numeric.toHexStringNoPrefix(Numeric.toBytesPadded(value, 32)) +
                Numeric.toHexStringNoPrefix(Numeric.toBytesPadded(spendNonce, 32));

        String data =
                Numeric.toHexStringNoPrefix(Hash.sha3(Numeric.hexStringToByteArray(
                        paramStr))
                );
        log.info("unsiginData:{}",paramStr);
        byte[] proofBytes = Numeric.hexStringToByteArray(data);
        return proofBytes ;
    }

    public String ecrecoverAddress(byte[] proof, byte[] r,byte[] s) {
        ECDSASignature esig = new ECDSASignature(Numeric.toBigInt(r), Numeric.toBigInt(s));
        BigInteger res;
        for (int i=0; i<4; i++) {
            res = Sign.recoverFromSignature(i, esig, proof);
//            if ((res != null) && Keys.getAddress(res).toLowerCase().equals(expectedAddress.substring(2).toLowerCase())) {
//                log.info("public Ethereum address: 0x" + Keys.getAddress(res));
//                return Keys.getAddress(res);
//            }
            return Keys.getAddress(res);
        }
        return null;
    }

    /**
     * 获取spendNonce
     * @param contractAddr
     * @return
     */
    public BigInteger getSpendNonce(String contractAddr){
        String methodName = "getSpendNonce";
        //入参
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        //出参
        TypeReference<Uint> typeReference = new TypeReference<Uint>() {
        };
        outputParameters.add(typeReference);

        Function function = new Function(methodName, inputParameters, outputParameters);
        String data = FunctionEncoder.encode(function);
        Transaction transaction = Transaction.createEthCallTransaction(DEFAULT_WALLET_ADDRESS, contractAddr, data);
        BigInteger nonce = BigInteger.ZERO;
        try {
            EthCall ethCall = web3jClint.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
            List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
            if (results != null && results.size() > 0) {
                nonce = (BigInteger) results.get(0).getValue();
            }
        } catch (Exception e) {
            log.error("【ETH multiSign getSpendNonce】：：walletAddr:{},multiSignContract:{} 异常：{}", DEFAULT_WALLET_ADDRESS, contractAddr, e);
        }
        log.info("multiSign getSpendNonce:{}", nonce);
        return nonce;
    }


    /**
     * 获取spendNonce
     * @param contractAddr
     * @return
     */
    public BigInteger getRequired(String contractAddr){
        String methodName = "getRequired";
        //入参
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        //出参
        TypeReference<Uint> typeReference = new TypeReference<Uint>() {
        };
        outputParameters.add(typeReference);

        Function function = new Function(methodName, inputParameters, outputParameters);
        String data = FunctionEncoder.encode(function);
        Transaction transaction = Transaction.createEthCallTransaction(DEFAULT_WALLET_ADDRESS, contractAddr, data);
        BigInteger nonce = BigInteger.ZERO;
        try {
            EthCall ethCall = web3jClint.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
            List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
            if (results != null && results.size() > 0) {
                nonce = (BigInteger) results.get(0).getValue();
            }
        } catch (Exception e) {
            log.error("【ETH multiSign getSpendNonce】：：walletAddr:{},multiSignContract:{} 异常：{}", DEFAULT_WALLET_ADDRESS, contractAddr, e);
        }
        log.info("multiSign getSpendNonce:{}", nonce);
        return nonce;
    }

    //
    /**
     * 获取spendNonce
     * @param contractAddr
     * @return
     */
    public void activeOwner(String contractAddr){
        String methodName = "active";
        //入参
        List<Type> inputParameters = new ArrayList<>();
        //出参
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        Function function = new Function(methodName, inputParameters, outputParameters);
        String data = FunctionEncoder.encode(function);
        Transaction transaction = Transaction.createEthCallTransaction(DEFAULT_WALLET_ADDRESS, contractAddr, data);
        try {
            EthCall ethCall = web3jClint.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
            List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
        } catch (Exception e) {
            log.error("【ETH multiSign activeOwner】：：walletAddr:{},multiSignContract:{} 异常：{}", DEFAULT_WALLET_ADDRESS, contractAddr, e);
        }
    }


    /**
     * 估算交易费用
     *
     * @param estimateGasLimitDto
     * @return
     */
    public TxGasFee estimateTxFee(EstimateGasLimitDto estimateGasLimitDto) {
        BigInteger gasPrice = null;
        if (estimateGasLimitDto.getGasPrice() == null || estimateGasLimitDto.getMinerFee() == null) {
            gasPrice = ethInvoke.getCurrentGasPrice();
        }
        Transaction transaction =null;
        if (estimateGasLimitDto.getEthValue() == null){
            transaction = Transaction.createFunctionCallTransaction(estimateGasLimitDto.getFromAddr(), estimateGasLimitDto.getNonce(), gasPrice, null, estimateGasLimitDto.getContractAddress(), estimateGasLimitDto.getFuncABI());
        }else{
            transaction = Transaction.createFunctionCallTransaction(estimateGasLimitDto.getFromAddr(),  estimateGasLimitDto.getNonce(), gasPrice, null, estimateGasLimitDto.getContractAddress(), estimateGasLimitDto.getEthValue(), estimateGasLimitDto.getFuncABI());
        }
        //是否重试
        BigInteger gasLimit = null;
        if (estimateGasLimitDto.isRetry()){
            //失败重试
            try {
                Transaction finalTransaction = transaction;
                gasLimit = RetryUtil.<BigInteger>getADefaultRetryer()
                        .call(new Callable<BigInteger>() {
                            @Override
                            public BigInteger call() throws Exception {
                                BigInteger transactionGasLimit = ethInvoke.getTransactionGasLimit(finalTransaction,true);
                                return transactionGasLimit;
                            }
                        });
            } catch (Exception e) {
                log.error("估算手续费异常e:",e);
            }
        }else{
            try{
                gasLimit = ethInvoke.getTransactionGasLimit(transaction,true);
            }catch (Exception e){
            }
        }

        if (gasLimit == null){
            gasLimit = estimateGasLimitDto.getDefaultGasLimit();
        }

        if (estimateGasLimitDto.getMinerFee() != null) {
            BigDecimal bigDecimal = Convert.toWei(estimateGasLimitDto.getMinerFee(), Convert.Unit.ETHER);
            BigDecimal divide = bigDecimal.divide(new BigDecimal(gasLimit), 18, RoundingMode.HALF_UP);
            gasPrice = divide.toBigInteger();
        }

        BigInteger gasFee = gasLimit.multiply(gasPrice);
        TxGasFee txGasFee = TxGasFee.builder()
                .gasLimit(gasLimit)
                .gasPrice(gasPrice)
                .gasFee(gasFee)
                .build();
        return txGasFee;
    }

}
