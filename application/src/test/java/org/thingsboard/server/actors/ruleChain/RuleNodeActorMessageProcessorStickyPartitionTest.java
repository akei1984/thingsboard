/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.actors.ruleChain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.rule.RuleChainService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class RuleNodeActorMessageProcessorStickyPartitionTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("c7bf4c85-923c-4688-a4b5-0f8a0feb7cd5"));
    private final RuleNodeId RULE_NODE_ID = new RuleNodeId(UUID.fromString("1ca5e2ef-1309-41d9-bafa-709e9df0e2a6"));
    private final RuleChainId RULE_CHAIN_ID = new RuleChainId(UUID.fromString("b87c4123-f9f2-41a6-9a09-e3a5b6580b11"));
    private final String QUEUE_NAME = "Main";

    @Mock
    private ActorSystemContext systemContext;
    @Mock
    private TbActorCtx selfActor;
    @Mock
    private TbActorRef parentActorRef;
    @Mock
    private TbClusterService clusterService;
    @Mock
    private RuleChainService ruleChainService;

    private RuleNode ruleNode;

    @BeforeEach
    void setUp() {
        ruleNode = new RuleNode(RULE_NODE_ID);
        ruleNode.setRuleChainId(RULE_CHAIN_ID);
        ruleNode.setSingletonMode(true);
        ruleNode.setQueueName(QUEUE_NAME);

        given(systemContext.getApiUsageClient()).willReturn(mock(org.thingsboard.server.common.stats.TbApiUsageReportClient.class));
        given(systemContext.getRuleChainService()).willReturn(ruleChainService);
        given(ruleChainService.findRuleNodeById(TENANT_ID, RULE_NODE_ID)).willReturn(ruleNode);
        given(selfActor.getParentRef()).willReturn(parentActorRef);
        given(systemContext.getClusterService()).willReturn(clusterService);
        given(systemContext.getDiscoveryService()).willReturn(mock(org.thingsboard.server.queue.discovery.DiscoveryService.class));
    }

    @Test
    void givenStickyEnabled_whenIsMyNodePartition_thenChecksRuleChainIdPartition() throws Exception {
        // GIVEN - sticky enabled; partition for ruleChainId is NOT my partition
        given(systemContext.isStickyPartitionByRuleChain()).willReturn(true);
        given(systemContext.resolve(eq(ServiceType.TB_RULE_ENGINE), eq(QUEUE_NAME), eq(TENANT_ID), eq(RULE_CHAIN_ID)))
                .willReturn(tpi(false));

        var processor = createActiveProcessor();
        var msg = buildMsg();
        var ruleChainMsg = new RuleChainToRuleNodeMsg(
                mock(org.thingsboard.rule.engine.api.TbContext.class), msg, TbNodeConnectionType.SUCCESS);

        // WHEN - processor handles the message; since not my partition it will re-queue
        processor.onRuleChainToRuleNodeMsg(ruleChainMsg);

        // THEN - partition resolution uses RULE_CHAIN_ID (sticky key), not RULE_NODE_ID
        ArgumentCaptor<org.thingsboard.server.common.data.id.EntityId> entityIdCaptor =
                ArgumentCaptor.forClass(org.thingsboard.server.common.data.id.EntityId.class);
        then(systemContext).should(org.mockito.Mockito.atLeastOnce())
                .resolve(eq(ServiceType.TB_RULE_ENGINE), any(), eq(TENANT_ID), entityIdCaptor.capture());
        assertThat(entityIdCaptor.getAllValues()).contains(RULE_CHAIN_ID);
        assertThat(entityIdCaptor.getAllValues()).doesNotContain(RULE_NODE_ID);
    }

    @Test
    void givenStickyDisabled_whenIsMyNodePartition_thenChecksRuleNodeIdPartition() throws Exception {
        // GIVEN - sticky disabled; partition for ruleNodeId is NOT my partition
        given(systemContext.isStickyPartitionByRuleChain()).willReturn(false);
        given(systemContext.resolve(eq(ServiceType.TB_RULE_ENGINE), eq(QUEUE_NAME), eq(TENANT_ID), eq(RULE_NODE_ID)))
                .willReturn(tpi(false), tpi(true)); // first=membership check, second=putToNodePartition

        var processor = createActiveProcessor();
        var msg = buildMsg();
        var ruleChainMsg = new RuleChainToRuleNodeMsg(
                mock(org.thingsboard.rule.engine.api.TbContext.class), msg, TbNodeConnectionType.SUCCESS);

        // WHEN
        processor.onRuleChainToRuleNodeMsg(ruleChainMsg);

        // THEN - partition key is always RULE_NODE_ID when sticky disabled
        ArgumentCaptor<org.thingsboard.server.common.data.id.EntityId> entityIdCaptor =
                ArgumentCaptor.forClass(org.thingsboard.server.common.data.id.EntityId.class);
        then(systemContext).should(org.mockito.Mockito.atLeastOnce())
                .resolve(eq(ServiceType.TB_RULE_ENGINE), any(), eq(TENANT_ID), entityIdCaptor.capture());
        assertThat(entityIdCaptor.getAllValues()).allMatch(id -> id.equals(RULE_NODE_ID));
    }

    @Test
    void givenStickyEnabledAndNotMyPartition_whenOnRuleChainMsg_thenPushesToRuleChainIdPartition() throws Exception {
        // GIVEN - not my partition (ruleChainId-based), then route there
        var targetTpi = tpi(true);
        given(systemContext.isStickyPartitionByRuleChain()).willReturn(true);
        given(systemContext.resolve(eq(ServiceType.TB_RULE_ENGINE), eq(QUEUE_NAME), eq(TENANT_ID), eq(RULE_CHAIN_ID)))
                .willReturn(tpi(false), targetTpi); // 1st=membership check, 2nd=putToNodePartition

        var processor = createActiveProcessor();
        var msg = buildMsg();
        var ruleChainMsg = new RuleChainToRuleNodeMsg(
                mock(org.thingsboard.rule.engine.api.TbContext.class), msg, TbNodeConnectionType.SUCCESS);

        // WHEN
        processor.onRuleChainToRuleNodeMsg(ruleChainMsg);

        // THEN - message pushed to rule engine using the ruleChainId partition
        then(clusterService).should().pushMsgToRuleEngine(eq(targetTpi), any(), any(), any());
    }

    @Test
    void givenStickyDisabledAndNotMyPartition_whenOnRuleChainMsg_thenPushesToRuleNodeIdPartition() throws Exception {
        // GIVEN - not my partition (ruleNodeId-based), then route there
        var targetTpi = tpi(true);
        given(systemContext.isStickyPartitionByRuleChain()).willReturn(false);
        given(systemContext.resolve(eq(ServiceType.TB_RULE_ENGINE), eq(QUEUE_NAME), eq(TENANT_ID), eq(RULE_NODE_ID)))
                .willReturn(tpi(false), targetTpi);

        var processor = createActiveProcessor();
        var msg = buildMsg();
        var ruleChainMsg = new RuleChainToRuleNodeMsg(
                mock(org.thingsboard.rule.engine.api.TbContext.class), msg, TbNodeConnectionType.SUCCESS);

        // WHEN
        processor.onRuleChainToRuleNodeMsg(ruleChainMsg);

        // THEN - message pushed to rule engine using the ruleNodeId partition
        then(clusterService).should().pushMsgToRuleEngine(eq(targetTpi), any(), any(), any());
    }

    @Test
    void givenStickyEnabledButNullRuleChainId_whenIsMyNodePartition_thenFallsBackToRuleNodeId() throws Exception {
        // GIVEN - ruleNode has no ruleChainId; sticky enabled but no ruleChainId → fallback
        ruleNode.setRuleChainId(null);
        given(systemContext.isStickyPartitionByRuleChain()).willReturn(true);
        given(systemContext.resolve(eq(ServiceType.TB_RULE_ENGINE), eq(QUEUE_NAME), eq(TENANT_ID), eq(RULE_NODE_ID)))
                .willReturn(tpi(false), tpi(true));

        var processor = createActiveProcessor();
        var msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(TENANT_ID)
                .queueName(QUEUE_NAME)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .callback(mock(TbMsgCallback.class))
                .build();
        var ruleChainMsg = new RuleChainToRuleNodeMsg(
                mock(org.thingsboard.rule.engine.api.TbContext.class), msg, TbNodeConnectionType.SUCCESS);

        // WHEN
        processor.onRuleChainToRuleNodeMsg(ruleChainMsg);

        // THEN - falls back to RULE_NODE_ID when ruleChainId is null
        ArgumentCaptor<org.thingsboard.server.common.data.id.EntityId> entityIdCaptor =
                ArgumentCaptor.forClass(org.thingsboard.server.common.data.id.EntityId.class);
        then(systemContext).should(org.mockito.Mockito.atLeastOnce())
                .resolve(eq(ServiceType.TB_RULE_ENGINE), any(), eq(TENANT_ID), entityIdCaptor.capture());
        assertThat(entityIdCaptor.getAllValues()).allMatch(id -> id.equals(RULE_NODE_ID));
    }

    private RuleNodeActorMessageProcessor createActiveProcessor() {
        var processor = new RuleNodeActorMessageProcessor(TENANT_ID, "Test Chain", RULE_NODE_ID, systemContext, selfActor);
        // Set component state to ACTIVE and tbNode to non-null to bypass the "not active" guard
        ReflectionTestUtils.setField(processor, "state", ComponentLifecycleState.ACTIVE);
        ReflectionTestUtils.setField(processor, "tbNode", mock(TbNode.class));
        return processor;
    }

    private TbMsg buildMsg() {
        return TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(TENANT_ID)
                .queueName(QUEUE_NAME)
                .ruleChainId(RULE_CHAIN_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_STRING)
                .callback(mock(TbMsgCallback.class))
                .build();
    }

    private static TopicPartitionInfo tpi(boolean myPartition) {
        return TopicPartitionInfo.builder()
                .topic("tb_rule_engine")
                .partition(0)
                .myPartition(myPartition)
                .build();
    }
}
