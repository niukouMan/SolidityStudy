package com.iccbank.demotoken.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Setter
@Getter
@AllArgsConstructor
@Builder
public class TxGasFee {
    private BigInteger gasLimit;
    private BigInteger gasPrice;   //单位：wei
    private BigInteger gasFee;     //单位：wei
}
