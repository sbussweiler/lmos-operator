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
import org.eclipse.lmos.operator.resources.AgentResource
import org.eclipse.lmos.routing.core.llm.Agent
import org.eclipse.lmos.routing.core.llm.Capability
import org.eclipse.lmos.routing.core.semantic.EmbeddingHandler
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
        agentResource.convert().forEach { (tenant, agent) ->
            log.info(
                "Create embeddings for agent '${agentResource.metadata.namespace}/${agentResource.metadata.name}' " +
                    "and tenant '$tenant'.",
            )
            embeddingHandler.ingest(tenant, agent)
        }
        return UpdateControl.noUpdate()
    }

    override fun cleanup(
        resource: AgentResource?,
        context: Context<AgentResource>?,
    ): DeleteControl {
        resource?.convert()?.forEach { (tenant, agent) ->
            log.info("Remove embeddings for agent '${resource.metadata.namespace}/${resource.metadata.name}' and tenant '$tenant'.")
            embeddingHandler.remove(tenant, agent)
        }
        return DeleteControl.defaultDelete()
    }

    private fun AgentResource.convert(): Map<String, Agent> =
        this.spec.supportedTenants.associateWith { tenant ->
            Agent(this.spec.id, this.spec.providedCapabilities.map { Capability(it.name, tenant, it.examples) })
        }
}
