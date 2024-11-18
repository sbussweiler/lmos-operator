/*
 * SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package ai.ancf.lmos.operator.server.routing.impl

import ai.ancf.lmos.operator.resources.ChannelResource
import ai.ancf.lmos.operator.resources.ChannelRoutingResource
import ai.ancf.lmos.operator.server.routing.CustomResourcesService
import io.fabric8.kubernetes.client.KubernetesClient
import org.springframework.stereotype.Component
import java.util.function.Consumer

@Component
class DefaultCustomResourcesService(private val client: KubernetesClient) : CustomResourcesService {
    override fun getRouting(
        tenant: String,
        channel: String,
        subset: String,
    ): ChannelRoutingResource? {
        val labelSelectors =
            mapOf(
                "tenant" to tenant,
                "channel" to channel,
                "subset" to subset,
            )

        val channelRoutingResources =
            client.resources(
                ChannelRoutingResource::class.java,
            ).withLabels(labelSelectors).list()

        if (channelRoutingResources.items.isEmpty()) {
            return null
        } else {
            val channelRoutingResource = channelRoutingResources.items[0]
            channelRoutingResource.metadata.ownerReferences = null
            channelRoutingResource.metadata.managedFields = null
            return channelRoutingResource
        }
    }

    override fun getChannels(
        tenant: String,
        subset: String,
    ): List<ChannelResource> {
        val labelSelectors =
            mapOf(
                "tenant" to tenant,
                "subset" to subset,
            )

        val channelResources =
            client.resources(
                ChannelResource::class.java,
            ).withLabels(labelSelectors).list()
        channelResources.items.forEach(
            Consumer { channelResource: ChannelResource ->
                channelResource.metadata.managedFields = null
            },
        )
        return channelResources.items
    }

    override fun getChannel(
        tenant: String,
        channel: String,
        subset: String,
    ): ChannelResource? {
        val labelSelectors =
            mapOf(
                "tenant" to tenant,
                "channel" to channel,
                "subset" to subset,
            )

        val channelResources =
            client.resources(
                ChannelResource::class.java,
            ).withLabels(labelSelectors).list()

        if (channelResources.items.isEmpty()) {
            return null
        } else {
            val channelResource = channelResources.items[0]
            channelResource.metadata.managedFields = null
            return channelResource
        }
    }
}