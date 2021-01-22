package com.iccbank.demotoken.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigInteger;

@Setter
@Getter
@AllArgsConstructor
@Builder
public class EthRawTransaction implements Serializable {

    /** 地址nonce */
    private BigInteger nonce;
    /** 交易gasPrice */
    private BigInteger gasPrice;
    /** 交易gasLimit */
    private BigInteger gasLimit;
    /** 接收地址 */
    private String to;
    /** 转账金额 */
    private BigInteger value;
    /** 其他数据 */
    private String data;
}
