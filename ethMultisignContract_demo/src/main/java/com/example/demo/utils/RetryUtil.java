package com.example.demo.utils;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;

import java.util.concurrent.TimeUnit;

/**
 * 重试
 */
public class RetryUtil {

    public static <T> Retryer<T> getADefaultRetryer() {
        Retryer<T> retryer = RetryerBuilder.<T>newBuilder()
                .retryIfException()
                //设置1秒后重试
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                //设置重试次数 超过将出异常
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .build();

        return retryer;
    }

}
