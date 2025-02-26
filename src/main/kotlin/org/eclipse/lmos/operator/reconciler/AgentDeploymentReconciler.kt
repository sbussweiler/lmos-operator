/*
 * SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.operator.reconciler

import ai.ancf.lmos.wot.thing.ThingDescription
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.javaoperatorsdk.operator.api.reconciler.*
import org.eclipse.lmos.operator.reconciler.generator.AgentGenerator
import org.eclipse.lmos.operator.resources.ProvidedCapability
import org.eclipse.lmos.operator.service.AgentClient
import org.eclipse.lmos.operator.service.KubernetesResourceManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private const val ANNOTATION_KEY_TD_PATH = "wot.w3.org/td-endpoint"
private const val DEFAULT_VALUE_TD_PATH = ".well-known/wot"

@Component
@ControllerConfiguration(labelSelector = "wot-agent=true")
class AgentDeploymentReconciler(
    private val agentClient: AgentClient,
    private val kubernetesResourceManager: KubernetesResourceManager,
) : Reconciler<Deployment>, Cleaner<Deployment> {

    private val log = LoggerFactory.getLogger(AgentDeploymentReconciler::class.java)

    override fun reconcile(
        deployment: Deployment,
        context: Context<Deployment>
    ): UpdateControl<Deployment> {
        // FIXME:
        // 1. Can the Operator SDK only call the Reconciler when the deployment is ready?
        // 2. Use a DependentResource instead of the Reconiler?
        // 3. Shouldn't we listen directly to the service?

        val isDeploymentReady = kubernetesResourceManager.isDeploymentReady(deployment)

        log.info("is Deployment {} ready: {}", deployment.metadata.name, isDeploymentReady)

        if (isDeploymentReady) {
            try {
                val wotThingDescriptionPath = deployment.metadata.annotations.getOrDefault(ANNOTATION_KEY_TD_PATH, DEFAULT_VALUE_TD_PATH)
                val wotThingDescriptionUrl = kubernetesResourceManager.getServiceUrl(deployment, wotThingDescriptionPath)
                val wotThingDescription = agentClient.get(wotThingDescriptionUrl, ThingDescription::class.java)
                val wotCapabilitiesLink = wotThingDescription.links?.first { it.rel == "service-meta" }
                if (wotCapabilitiesLink != null) {
                    val capabilitiesUrl = wotCapabilitiesLink.href
                    val capabilities = agentClient.get(capabilitiesUrl, ThingCapabilities::class.java)
                    val agentResource = AgentGenerator.createAgentResource(deployment, wotThingDescription, capabilities)
                    kubernetesResourceManager.createAgentResource(agentResource )
                    return UpdateControl.noUpdate()
                } else {
                    log.error("Thing Description does not provide link to capabilities.")
                    return UpdateControl.noUpdate<Deployment>().rescheduleAfter(10, TimeUnit.SECONDS)
                }
            } catch (e: Exception) {
                log.error("Error processing td for deployment: {}", deployment.metadata.name, e)
                return UpdateControl.noUpdate<Deployment>().rescheduleAfter(10, TimeUnit.SECONDS)
            }
        }

        return UpdateControl.noUpdate<Deployment>().rescheduleAfter(10, TimeUnit.SECONDS)
    }

    override fun cleanup(deployment: Deployment, context: Context<Deployment?>?): DeleteControl {
        log.info("Trigger AgentResource Deletion for deployment: {}", deployment.metadata.name)
        kubernetesResourceManager.deleteAgentResource(deployment)
        return DeleteControl.defaultDelete()
    }

}

data class ThingCapabilities(
    var supportedTenants: Set<String> = emptySet(),
    var supportedChannels: Set<String> = emptySet(),
    var providedCapabilities: Set<ProvidedCapability> = emptySet(),
)