package com.example.demo;

import com.example.demo.invoke.MultiSignInvoke;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@Slf4j
class EthMultisignContractDemoApplicationTests {

    boolean isTest = true;
    //    String ropsten = "http://10.10.23.40:8541/";
    String ropsten = "https://ropsten.infura.io/v3/5e4fc2780f254d629a6d919e002c4ce8";
    //    String mainnet = "https://mainnet.infura.io/v3/c1c81ee2287f46549f4551952fe908c1";
    String mainnet = "http://52.77.229.82:6666";
    String url = isTest ? ropsten : mainnet;

    //mock data
    String[] activiteAddress = new String[]{"0xcb56a6b574d3d4f6aa8dea0ff5028ee7e5ea2300","0x55478526b9e609Fa7f8557914cC95476228C0e19","0x7971b576f602488e1c57f7b03b2e7a33fcb0ebca"};
    //对应的私钥
    String[] activitePrivate = new String[]{"","",""};
    //多签合约
    String multiSignContractAddr = "0x6010ec43affe0559b2159a0816dc8f669d657955";


    @Test
    public void testMultiSignEth(){
        MultiSignInvoke multiSignInvoke = new MultiSignInvoke(url);
        String from = activiteAddress[0];
        String fromPrivateKey = activitePrivate[0];
        String destination = activiteAddress[1];
        String erc20Contract = null;
        BigInteger value = Convert.toWei("0.01", Convert.Unit.ETHER).toBigInteger();
        byte[] bytes = multiSignInvoke.generateMessageToSign4(multiSignContractAddr, erc20Contract, destination, value);
        byte[] unSignMessage = multiSignInvoke.getEthereumMessageHash(bytes);

        List<BigInteger> vs = new ArrayList<>();
        List<byte[]> rs = new ArrayList<>();
        List<byte[]> ss = new ArrayList<>();
        for (int i=0;i<activiteAddress.length;i++){
            Sign.SignatureData signMessage = multiSignInvoke.signMessage(activitePrivate[i], unSignMessage);
            BigInteger bigInteger = Numeric.toBigInt(signMessage.getV());
            vs.add(bigInteger);
            rs.add(signMessage.getR());
            ss.add(signMessage.getS());
        }

        RawTransaction rawTransaction = multiSignInvoke.createSpendTransaction(multiSignContractAddr,from, destination, value, vs, rs, ss);
        String txHash = multiSignInvoke.signAndBroadcast(rawTransaction, fromPrivateKey);
        log.info("txHash:{}",txHash);
    }


    @Test
    public void testMultiSignErc20(){
        MultiSignInvoke multiSignInvoke = new MultiSignInvoke(url);
        String from = activiteAddress[0];
        String fromPrivateKey = activitePrivate[0];
        String destination = activiteAddress[1];
        String erc20Contract = "0x1c3b1ecaacb0ffad56060fc1b6405fb508d93518";
//        String erc20Contract = "0x0";
        BigInteger value = Convert.toWei("0.01", Convert.Unit.ETHER).toBigInteger();
        byte[] bytes = multiSignInvoke.generateMessageToSign4(multiSignContractAddr, erc20Contract, destination, value);
        byte[] unSignMessage = multiSignInvoke.getEthereumMessageHash(bytes);

        List<BigInteger> vs = new ArrayList<>();
        List<byte[]> rs = new ArrayList<>();
        List<byte[]> ss = new ArrayList<>();
        for (int i=0;i<activiteAddress.length;i++){
            Sign.SignatureData signMessage = multiSignInvoke.signMessage(activitePrivate[i], unSignMessage);
            BigInteger bigInteger = Numeric.toBigInt(signMessage.getV());
//            BigInteger bigInteger = new BigInteger("0");
            vs.add(bigInteger);
            rs.add(signMessage.getR());
            ss.add(signMessage.getS());
            //解析签名钱包地址
//            String s = multiSignInvoke.ecrecoverAddress(unSignMessage, signMessage.getR(), signMessage.getS());
//            log.info("address:{}",s);
        }

        RawTransaction rawTransaction = multiSignInvoke.createSpendErc20Transaction(multiSignContractAddr,from,erc20Contract, destination, value, vs, rs, ss);
        String txHash = multiSignInvoke.signAndBroadcast(rawTransaction, fromPrivateKey);
        log.info("txHash:{}",txHash);
    }


    @Test
    public void testMultiSign2(){
        MultiSignInvoke multiSignInvoke = new MultiSignInvoke(url);
        String from = activiteAddress[0];
        String fromPrivateKey = activitePrivate[0];
        String destination = activiteAddress[1];
        String erc20Contract = "0x1c3b1ecaacb0ffad56060fc1b6405fb508d93518";
//        String erc20Contract = "0x0";
        BigInteger value = Convert.toWei("0.01", Convert.Unit.ETHER).toBigInteger();
        byte[] bytes = multiSignInvoke.generateMessageToSign4(multiSignContractAddr, erc20Contract, destination, value);
        byte[] ethereumMessageHash = multiSignInvoke.getEthereumMessageHash(bytes);

        // byte[] unSignMessage = multiSignInvoke.generateMessageToSign3(multiSignContractAddr, erc20Contract, destination, value);

        Sign.SignatureData signMessage = multiSignInvoke.signMessage(activitePrivate[0], ethereumMessageHash);
        BigInteger bigInteger = Numeric.toBigInt(signMessage.getV());
        BigInteger vs = bigInteger;
        RawTransaction rawTransaction = multiSignInvoke.createSpendErc20Transaction2(multiSignContractAddr,from,erc20Contract, destination, value, vs, signMessage.getR(), signMessage.getS());
        String txHash = multiSignInvoke.signAndBroadcast(rawTransaction, fromPrivateKey);
        log.info("txHash:{}",txHash);
    }

    @Test
    public void getRequired(){
        MultiSignInvoke multiSignInvoke = new MultiSignInvoke(url);
        BigInteger required = multiSignInvoke.getRequired(multiSignContractAddr);
        log.info("required:{}",required);
    }

    @Test
    public void getSpendNonce(){
        MultiSignInvoke multiSignInvoke = new MultiSignInvoke(url);
        BigInteger required = multiSignInvoke.getSpendNonce(multiSignContractAddr);
        log.info("required:{}",required);
    }

    @Test
    public void activeOwner(){
        MultiSignInvoke multiSignInvoke = new MultiSignInvoke(url);
        multiSignInvoke.activeOwner(multiSignContractAddr);
    }


    @Test
    public void  sign() {
        String message = "hello";
        ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(activitePrivate[0], 16));
        Credentials credentials = Credentials.create(ecKeyPair);

        byte[] hash = message.getBytes(StandardCharsets.UTF_8);
        Sign.SignatureData signature = Sign.signPrefixedMessage(hash, credentials.getEcKeyPair());
        String r = Numeric.toHexString(signature.getR());
        String s = Numeric.toHexString(signature.getS()).substring(2);
        BigInteger v = Numeric.toBigInt(signature.getV());
        System.out.println(r + "    " + s + "    " + v);
        String s1 = new StringBuilder(r)
                .append(s)
                .append(v)
                .toString();

        System.out.println(s1);
    }

}
