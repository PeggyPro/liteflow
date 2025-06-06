package com.yomahub.liteflow.test.chainThreadPool;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.thread.ExecutorBuilder;

import java.util.concurrent.ExecutorService;

public class CustomChainThreadExecutor implements ExecutorBuilder {

    @Override
    public ExecutorService buildExecutor() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
        // 只有在非spring的场景下liteflowConfig才会为null
        if (ObjectUtil.isNull(liteflowConfig)) {
            liteflowConfig = new LiteflowConfig();
        }
        return buildDefaultExecutor(16, 16,
                                    512, "customer-chain-thead");
    }

}
