package com.yomahub.liteflow.solon.integration;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.solon.LiteflowProperty;
import com.yomahub.liteflow.solon.NodeComponentOfMethod;
import org.noear.solon.Utils;
import org.noear.solon.core.AopContext;
import org.noear.solon.core.Plugin;

import java.util.Properties;

/**
 * @author noear
 * @since 2.9
 */
public class XPluginImpl implements Plugin {
    @Override
    public void start(AopContext context) {
        //加载默认配置
        Properties defProps = Utils.loadProperties("META-INF/liteflow-default.properties");
        if (defProps != null && defProps.size() > 0) {
            defProps.forEach((k, v) -> {
                context.getProps().putIfAbsent(k, v);
            });
        }

        //是否启用
        boolean enable = context.getProps().getBool("liteflow.enable", false);

        if (!enable) {
            return;
        }

        //订阅 NodeComponent 组件
        context.subWrapsOfType(NodeComponent.class, bw -> {
            NodeComponent node1 = bw.raw();
            node1.setNodeId(bw.name());

            FlowBus.addSpringScanNode(bw.name(), bw.raw());
        });

        context.beanExtractorAdd(LiteflowMethod.class, (bw, method, anno) -> {
            NodeComponent node1 = new NodeComponentOfMethod(bw, method, anno.value());
            String nodeId = Utils.annoAlias(anno.nodeId(), bw.name());
            node1.setNodeId(nodeId);
            node1.setType(anno.nodeType());

            FlowBus.addSpringScanNode(bw.name(), node1);
        });

        context.beanBuilderAdd(LiteflowComponent.class, (clz, bw, anno) -> {
            if(NodeComponent.class.isAssignableFrom(clz)) {
                NodeComponent node1 = bw.raw();
                String id1 = Utils.annoAlias(anno.id(), anno.value());
                String name1 =Utils.annoAlias(anno.name(), id1);

                node1.setNodeId(id1);
                node1.setName(name1);

                FlowBus.addSpringScanNode(node1.getNodeId(), node1);
            }else{
                context.beanExtract(bw); //尝试提取 LiteflowMethod 函数
            }
        });

        //扫描相关组件
        context.beanOnloaded((ctx)->{
            context.beanScan(LiteflowProperty.class);
        });
    }
}
