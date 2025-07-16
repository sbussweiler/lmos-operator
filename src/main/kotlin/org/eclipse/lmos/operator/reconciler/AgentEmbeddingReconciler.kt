/*
 * SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.operator.reconciler

import io.javaoperatorsdk.operator.api.reconciler.Cleaner
import io.javaoperatorsdk.operator.api.reconciler.Context
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl
import io.javaoperatorsdk.operator.api.reconciler.Reconciler
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl
import org.eclipse.lmos.classifier.core.Agent
import org.eclipse.lmos.classifier.core.Capability
import org.eclipse.lmos.classifier.core.SystemContext
import org.eclipse.lmos.classifier.core.semantic.EmbeddingHandler
import org.eclipse.lmos.operator.resources.AgentResource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ControllerConfiguration
@ConditionalOnProperty(
    prefix = "lmos.router.embedding",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false,
)
class AgentEmbeddingReconciler(
    private val embeddingHandler: EmbeddingHandler,
) : Reconciler<AgentResource>,
    Cleaner<AgentResource> {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun reconcile(
        agentResource: AgentResource,
        context: Context<AgentResource>,
    ): UpdateControl<AgentResource> {
        agentResource.convert().forEach { (context, agent) ->
            try {
                embeddingHandler.ingest(context, agent)
            } catch (e: Exception) {
                log.error(
                    "Failed to ingest embeddings for agent '${agent.id}' " +
                        "(tenant: '${context.tenantId}', channel: '${context.channelId}').",
                    e,
                )
            }
        }
        return UpdateControl.noUpdate()
    }

    override fun cleanup(
        resource: AgentResource?,
        context: Context<AgentResource>?,
    ): DeleteControl {
        resource?.convert()?.forEach { (context, agent) ->
            try {
                embeddingHandler.remove(context, agent)
            } catch (e: Exception) {
                log.error(
                    "Failed to remove embeddings for agent '${agent.id}' " +
                        "(tenant: '${context.tenantId}', channel: '${context.channelId}').",
                    e,
                )
            }
        }
        return DeleteControl.defaultDelete()
    }

    private fun AgentResource.convert(): Map<SystemContext, Agent> {
        val systemContexts = generateSystemContexts(this.spec.supportedTenants, spec.supportedChannels)
        return systemContexts.associateWith {
            Agent(this.spec.id, this.spec.providedCapabilities.map { Capability(it.name, it.description, it.examples) })
        }
    }

    fun generateSystemContexts(
        tenants: Set<String>,
        channels: Set<String>,
    ): List<SystemContext> =
        tenants.flatMap { tenant ->
            channels.map { channel ->
                SystemContext(tenant, channel)
            }
        }
}
