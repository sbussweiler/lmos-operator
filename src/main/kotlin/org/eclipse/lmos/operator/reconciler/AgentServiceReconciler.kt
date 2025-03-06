/*
 * SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.operator.reconciler

import ai.ancf.lmos.wot.thing.ThingDescription
import io.fabric8.kubernetes.api.model.Service
import io.fabric8.kubernetes.client.KubernetesClient
import io.javaoperatorsdk.operator.api.reconciler.*
import org.eclipse.lmos.operator.reconciler.generator.AgentGenerator
import org.eclipse.lmos.operator.resources.AgentResource
import org.eclipse.lmos.operator.resources.ProvidedCapability
import org.eclipse.lmos.operator.service.AgentClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientException
import java.util.concurrent.TimeUnit

private const val ANNOTATION_KEY_TD_PATH = "wot.w3.org/td-endpoint"
private const val DEFAULT_VALUE_TD_PATH = ".well-known/wot"
private const val WOT_LINK_REL_CAPABILITIES = "service-meta"

@Component
@ControllerConfiguration(labelSelector = "wot-agent=true")
class AgentServiceReconciler(
    private val agentClient: AgentClient,
    private val kubernetesClient: KubernetesClient
) : Reconciler<Service>, Cleaner<Service> {

    private val log = LoggerFactory.getLogger(AgentServiceReconciler::class.java)

    override fun reconcile(service: Service, context: Context<Service>): UpdateControl<Service> {
        val serviceName = service.metadata.name
        val serviceBaseUrl = getBaseServiceUrl(service)
        val wotThingDescriptionPath = getWotThingDescriptionPath(service)
        val wotThingDescriptionUrl = "$serviceBaseUrl/$wotThingDescriptionPath"
        log.info("Reconcile service '{}' with thing description {}", serviceName, wotThingDescriptionUrl)
        try {
            val wotThingDescription = agentClient.get(wotThingDescriptionUrl, ThingDescription::class.java)
            val wotCapabilitiesLink = wotThingDescription.links?.find { it.rel == WOT_LINK_REL_CAPABILITIES }
            if (wotCapabilitiesLink != null) {
                val capabilitiesUrl = wotCapabilitiesLink.href
                val capabilities = agentClient.get(capabilitiesUrl, ThingCapabilities::class.java)
                val agentResource = AgentGenerator.createAgentResource(service, wotThingDescription, capabilities)
                kubernetesClient.resources(AgentResource::class.java).resource(agentResource).serverSideApply()
                log.info("Created AgentResource for service '{}' with thing description {}", serviceName, wotThingDescriptionUrl)
            } else {
                log.error("Failed to create AgentResource for service '$serviceName', due to missing capabilities link in thing description $wotThingDescriptionUrl")
            }
        } catch (e: WebClientException) {
            log.warn("Could not reach thing description endpoint $wotThingDescriptionUrl from service '$serviceName' (${e.message}). Retrying in 20 seconds.")
            return UpdateControl.noUpdate<Service>().rescheduleAfter(20, TimeUnit.SECONDS)
        } catch (e: Exception) {
            log.error(
                "Failed to create AgentResource for service '$serviceName' with thing description $wotThingDescriptionUrl", e)
        }
        return UpdateControl.noUpdate()
    }

    override fun cleanup(service: Service, context: Context<Service?>?): DeleteControl {
        kubernetesClient.resources(AgentResource::class.java)
            .inNamespace(service.metadata.namespace)
            .withName(service.metadata.name)
            .delete()
        log.info("Deleted AgentResource for service '{}'.", service.metadata.name)
        return DeleteControl.defaultDelete()
    }

    private fun getBaseServiceUrl(service: Service): String {
        val port = service.spec.ports.find { it.name == "https-wot" || it.name == "http-wot" }
        val schema = if (port?.name == "https-wot") "https" else "http"
        val portNumber = port?.port ?: 8080

        return "$schema://${service.metadata.name}.${service.metadata.namespace}.svc.cluster.local:$portNumber"
    }


    private fun getWotThingDescriptionPath(service: Service): String =
        service.metadata.annotations.getOrDefault(ANNOTATION_KEY_TD_PATH, DEFAULT_VALUE_TD_PATH)

}

data class ThingCapabilities(
    var supportedTenants: Set<String> = emptySet(),
    var supportedChannels: Set<String> = emptySet(),
    var providedCapabilities: Set<ProvidedCapability> = emptySet(),
)