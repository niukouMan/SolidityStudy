package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;

@Setter
@Getter
@AllArgsConstructor
@Builder
public class EstimateGasLimitDto {
    private String fromAddr;
    private String contractAddress;
    private BigInteger nonce;
    private BigInteger gasPrice;
    private BigDecimal minerFee;
    private BigInteger ethValue;
    private String funcABI;
    private BigInteger defaultGasLimit; //失败默认值
    private boolean retry;  //是否是否重试
}
