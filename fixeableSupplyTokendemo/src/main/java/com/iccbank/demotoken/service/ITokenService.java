package com.iccbank.demotoken.service;

import java.math.BigInteger;

public interface ITokenService {

     void mint(String adminAddress, String mintToAddress, BigInteger mintAmount);


    /**
     * 燃烧代币
     * @param adminAddress
     * @param burnFromaddress
     * @param burnAmount
     */
     void burn(String adminAddress,String burnFromaddress,BigInteger burnAmount);

    /**
     * 查询代币总供应量
     * @param tokenAddress
     */
     BigInteger totalSupply(String tokenAddress);

    /**
     * 当前管理员
     * @return
     */
    public String getCurrentAdmin();


    /**
     * 修改合约管理员
     * 修改合约管理员需先暂停合约（pause方法）
     * @param oldAdmin
     * @param newAdmin
     * @return
     */
     boolean changeAdmin(String oldAdmin,String newAdmin);


    /**
     * 当前管理员
     * @return
     */
    public boolean isPaused();

    /**
     * 修改合约管理员
     * @param adminAddress
     * @return
     */
    public boolean changePauseState(String adminAddress);

}
