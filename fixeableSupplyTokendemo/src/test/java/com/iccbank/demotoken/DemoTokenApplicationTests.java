package com.iccbank.demotoken;

import com.iccbank.demotoken.constants.Constant;
import com.iccbank.demotoken.service.FiexbleSupplyTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigInteger;

@SpringBootTest
class DemoTokenApplicationTests {

    @Autowired
    private FiexbleSupplyTokenService supplyTokenService;

    String adminAddress = "0xCB56a6B574d3D4F6aA8deA0fF5028ee7e5ea2300";
    String toAddress = "0x55478526b9e609Fa7f8557914cC95476228C0e19";


    /**
     * 铸造
     */
    @Test
    void testMint() {
        supplyTokenService.mint(adminAddress,toAddress,new BigInteger("10").multiply(BigInteger.TEN.pow(18)));
    }

    /**
     * 燃烧
     */
    @Test
    void testBurn() {
        supplyTokenService.burn(adminAddress,toAddress,new BigInteger("1").multiply(BigInteger.TEN.pow(18)));
    }

    /**
     * 当前总量
     */
    @Test
    void testTotalSupply(){
        BigInteger totalSupply = supplyTokenService.totalSupply(Constant.TOKEN_ADDRESS);
        System.out.println("totalSupply:  "+totalSupply);
    }

    /**
     * 当前管理员
     */
    @Test
    void getCurrentAdmin(){
        String currentAdmin = supplyTokenService.getCurrentAdmin();
        System.out.println("currentAdmin: "+currentAdmin);
    }

    /**
     * 修改管理员
     */
    @Test
    void changeOwner(){
        String newAdmin = "0xCB56a6B574d3D4F6aA8deA0fF5028ee7e5ea2300";
        supplyTokenService.changeAdmin(adminAddress,newAdmin);
    }

    /**
     * 合约当前运行状态  【暂停，运行中】
     */
    @Test
    void getPauseState(){
        boolean isPaused = supplyTokenService.isPaused();
        System.out.println("isPaused: "+isPaused);
    }

    /**
     * 修改合约运行状态
     */
    @Test
    void changePauseState(){
        supplyTokenService.changePauseState(adminAddress);
    }

}
