/*
 * SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.operator.server.wot

import ai.ancf.lmos.wot.reflection.annotations.*
import com.fasterxml.jackson.databind.JsonNode
import io.fabric8.kubernetes.client.KubernetesClient
import org.eclipse.lmos.operator.reconciler.generator.LABEL_WOT_THING_DESCRIPTION_ID
import org.eclipse.lmos.operator.resources.AgentResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/apis/v1/things", produces = [MediaType.APPLICATION_JSON_VALUE])
@Thing(
    id = "things", title = "Thing Description Directory", type = "ThingDirectory",
    description = "This a Thing Discovery Service implemented by the LMOS Operator."
)
@Context(prefix = "NOT-REQUIRED", url = "https://www.w3.org/2022/wot/discovery")
@VersionInfo(instance = "1.0.0")
class ThingDiscoveryController(private val client: KubernetesClient) {

    @Property(readOnly = true)
    val things: List<JsonNode>
        get() = retrieveThingDescriptions()

    @GetMapping
    fun getThingDescriptions(): ResponseEntity<List<JsonNode>> {
        val thingDescriptions = retrieveThingDescriptions()
        return ResponseEntity.ok().body(thingDescriptions)
    }

    @GetMapping("/{thingId}")
    fun getThingDescription(@PathVariable("thingId") thingId: String): ResponseEntity<JsonNode> {
        val labelSelectors = mapOf(LABEL_WOT_THING_DESCRIPTION_ID to thingId)
        val agentResource = client.resources(AgentResource::class.java)
            .withLabels(labelSelectors)
            .list()
            .items
            .firstOrNull() ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(agentResource.spec.wotThingDescription)
    }

    private fun retrieveThingDescriptions(): List<JsonNode> {
        return client.resources(AgentResource::class.java)
            .list()
            .items
            .mapNotNull { it.spec.wotThingDescription }
            .toList()
    }

}
