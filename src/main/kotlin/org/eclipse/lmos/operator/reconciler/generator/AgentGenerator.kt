/*
 * SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.operator.reconciler.generator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder
import io.fabric8.kubernetes.api.model.Service
import org.eclipse.lmos.operator.reconciler.ThingCapabilities
import org.eclipse.lmos.operator.resources.AgentResource
import org.eclipse.lmos.operator.resources.AgentSpec
import org.eclipse.thingweb.thing.ThingDescription

const val LABEL_WOT_THING_DESCRIPTION_ID = "wot-td-id"

object AgentGenerator {

    fun createAgentResource(
        service: Service,
        thingDescription: ThingDescription,
        capabilitiesDescription: ThingCapabilities
    ): AgentResource {
        val agentSpec = AgentSpec()
        agentSpec.supportedTenants = capabilitiesDescription.supportedTenants
        agentSpec.supportedChannels = capabilitiesDescription.supportedChannels
        agentSpec.providedCapabilities = capabilitiesDescription.providedCapabilities
        agentSpec.description = thingDescription.description.orEmpty()
        agentSpec.wotThingDescriptionId = thingDescription.id
        agentSpec.wotThingDescription = jacksonObjectMapper().convertValue(thingDescription, JsonNode::class.java)

        val agentMetadata= ObjectMetaBuilder()
            .withName(service.metadata.name)
            .withNamespace(service.metadata.namespace)
            .addToLabels(LABEL_WOT_THING_DESCRIPTION_ID, thingDescription.id)
            .build()

        val agentResource = AgentResource()
        agentResource.metadata = agentMetadata
        agentResource.spec = agentSpec

        return agentResource
    }
}
