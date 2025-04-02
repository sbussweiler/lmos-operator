/*
 * SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.operator.server.wot

import com.fasterxml.jackson.databind.JsonNode
import io.fabric8.kubernetes.client.KubernetesClient
import org.eclipse.lmos.operator.reconciler.generator.LABEL_WOT_THING_DESCRIPTION_ID
import org.eclipse.lmos.operator.resources.AgentResource
import org.eclipse.thingweb.reflection.annotations.Context
import org.eclipse.thingweb.reflection.annotations.Property
import org.eclipse.thingweb.reflection.annotations.Thing
import org.eclipse.thingweb.reflection.annotations.VersionInfo
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
@Context(url = "https://www.w3.org/2022/wot/discovery")
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
        val thingDescription = retrieveThingDescriptions(labelSelectors)
            .firstOrNull() ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(thingDescription)
    }

    private fun retrieveThingDescriptions(labels: Map<String, String> = mapOf()): List<JsonNode> {
        return client.resources(AgentResource::class.java)
            .withLabels(labels)
            .list()
            .items
            .mapNotNull { it.spec.wotThingDescription }
            .toList()
    }

}
