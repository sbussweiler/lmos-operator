/*
 * SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.operator.service

import io.fabric8.kubernetes.api.model.ServicePort
import io.fabric8.kubernetes.api.model.StatusDetails
import io.fabric8.kubernetes.api.model.apps.Deployment
import io.fabric8.kubernetes.client.KubernetesClient
import org.eclipse.lmos.operator.resources.AgentResource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class KubernetesResourceManager(private val kubernetesClient: KubernetesClient) {

    private val log = LoggerFactory.getLogger(KubernetesResourceManager::class.java)

    fun createAgentResource(agentResource: AgentResource) {
        kubernetesClient.resources(AgentResource::class.java).createOrReplace(agentResource)
        log.info("AgentResource {} created.", agentResource)
    }

    fun deleteAgentResource(deployment: Deployment) {
        val deleteStatus: List<StatusDetails> = kubernetesClient.resources(AgentResource::class.java)
            .inNamespace(deployment.metadata.namespace)
            .withName(deployment.metadata.name)
            .delete()
        log.info("AgentResource {} deleted for deployment: {}", deleteStatus, deployment.metadata.name)
    }

    fun isDeploymentReady(deployment: Deployment): Boolean {
        val deploymentName = deployment.metadata.name
        val deploymentNamespace = deployment.metadata.namespace
        val replicas = deployment.status.replicas
        val availableReplicas = deployment.status.availableReplicas
        val desiredReplicas = deployment.spec.replicas

        log.info(
            "Reconciling Deployment: {} in namespace: {}, Replicas, availableReplicas: {}, desiredReplicas: {}",
            deploymentName, deploymentNamespace, availableReplicas, desiredReplicas
        )

        return (replicas != null && availableReplicas != null && replicas == desiredReplicas && availableReplicas == desiredReplicas)
    }

    fun getServiceUrl(deployment: Deployment, path: String): String {
        val baseServiceUrl = getBaseServiceUrl(findService(deployment))
        return if (path.startsWith("/")) {
            "$baseServiceUrl$path"
        } else {
            "$baseServiceUrl/$path"
        }
    }

    private fun findService(deployment: Deployment): io.fabric8.kubernetes.api.model.Service {
        val selectorLabels = deployment.spec.selector.matchLabels
        val deploymentNamespace = deployment.metadata.namespace
        val serviceList = kubernetesClient.services()
            .inNamespace(deploymentNamespace)
            .withLabels(selectorLabels)
            .list()
        if (serviceList.items.size != 1) {
            log.error("Expected exactly one service, but got {}, {}", serviceList.items.size, serviceList.items)
            throw IllegalStateException("Expected exactly one service, but got " + serviceList.items.size)
        }
        return serviceList
            .items
            .first()
    }

    private fun getBaseServiceUrl(service: io.fabric8.kubernetes.api.model.Service): String {
        val servicePort: ServicePort = service.spec.ports.first()
        val port = servicePort.port.toString()
        val isHttps = servicePort.port == 443 || "https".equals(servicePort.name, ignoreCase = true)
        val protocol = if (isHttps) "https" else "http"
        val url = "$protocol://${service.metadata.name}.${service.metadata.namespace}.svc.cluster.local:$port"
        log.info("Service URL is: {}", url)
        return url
    }

}