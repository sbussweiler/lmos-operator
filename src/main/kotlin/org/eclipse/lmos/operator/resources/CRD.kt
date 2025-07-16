/*
 * SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.operator.resources

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.fabric8.crd.generator.annotation.PrinterColumn
import io.fabric8.generator.annotation.Required
import io.fabric8.kubernetes.api.model.Namespaced
import io.fabric8.kubernetes.client.CustomResource
import io.fabric8.kubernetes.model.annotation.Group
import io.fabric8.kubernetes.model.annotation.Kind
import io.fabric8.kubernetes.model.annotation.Plural
import io.fabric8.kubernetes.model.annotation.ShortNames
import io.fabric8.kubernetes.model.annotation.Singular
import io.fabric8.kubernetes.model.annotation.Version
import org.eclipse.lmos.operator.resolver.ResolveStrategy
import org.eclipse.lmos.operator.resolver.Wire

class EmptyStatus

@Group("lmos.eclipse")
@Version("v2")
@Plural("agents")
@Singular("agent")
@Kind("Agent")
@ShortNames("ag")
class AgentResource :
    CustomResource<AgentSpec, EmptyStatus>(),
    Namespaced

@Group("lmos.eclipse")
@Version("v1", storage = false)
@Plural("agents")
@Singular("agent")
@Kind("Agent")
@ShortNames("ag")
class AgentResourceV1 :
    CustomResource<AgentSpecV1, EmptyStatus>(),
    Namespaced

data class AgentSpec(
    var id: String = "",
    var description: String = "",
    var supportedTenants: Set<String> = emptySet(),
    var supportedChannels: Set<String> = emptySet(),
    var providedCapabilities: Set<ProvidedCapability> = emptySet(),
)

data class AgentSpecV1(
    var description: String = "",
    var supportedTenants: Set<String> = emptySet(),
    var supportedChannels: Set<String> = emptySet(),
    var providedCapabilities: Set<ProvidedCapabilityV1> = emptySet(),
)

sealed class Capability {
    abstract var id: String
    abstract var name: String
    abstract var version: String
}

sealed class CapabilityV1 {
    abstract var name: String
    abstract var version: String
}

data class ProvidedCapability(
    @JsonPropertyDescription("The id of the capability")
    @Required
    override var id: String,
    @JsonPropertyDescription("The name of the capability")
    @Required
    override var name: String,
    @Required
    override var version: String,
    @Required
    var description: String = "",
    @Required
    var examples: List<String> = emptyList(),
) : Capability()

data class ProvidedCapabilityV1(
    @JsonPropertyDescription("The name of the capability")
    @Required
    override var name: String,
    @Required
    override var version: String,
    var description: String = "",
) : CapabilityV1()

@Group("lmos.eclipse")
@Version("v2")
@Plural("channels")
@Singular("channel")
@Kind("Channel")
@ShortNames("ch")
class ChannelResource :
    CustomResource<ChannelSpec, ChannelStatus>(),
    Namespaced

@Group("lmos.eclipse")
@Version("v1", storage = false)
@Plural("channels")
@Singular("channel")
@Kind("Channel")
@ShortNames("ch")
class ChannelResourceV1 :
    CustomResource<ChannelSpec, ChannelStatusV1>(),
    Namespaced

enum class ResolveStatus {
    RESOLVED,
    UNRESOLVED,
}

data class RequiredCapability(
    @JsonPropertyDescription("The id of the capability")
    @Required
    override var id: String,
    @JsonPropertyDescription("The name of the capability")
    @Required
    override var name: String,
    @Required
    override var version: String,
    var strategy: ResolveStrategy = ResolveStrategy.HIGHEST,
) : Capability()

data class RequiredCapabilityV1(
    @JsonPropertyDescription("The name of the capability")
    @Required
    override var name: String,
    @Required
    override var version: String,
    var strategy: ResolveStrategy = ResolveStrategy.HIGHEST,
) : CapabilityV1()

data class ChannelStatus(
    @PrinterColumn(name = "RESOLVE_STATUS")
    var resolveStatus: ResolveStatus = ResolveStatus.UNRESOLVED,
    var unresolvedRequiredCapabilities: Set<RequiredCapability> = emptySet(),
)

data class ChannelStatusV1(
    @PrinterColumn(name = "RESOLVE_STATUS")
    var resolveStatus: ResolveStatus = ResolveStatus.UNRESOLVED,
    var unresolvedRequiredCapabilities: Set<RequiredCapabilityV1> = emptySet(),
)

@JsonDeserialize
data class ChannelSpec(
    @Required
    var requiredCapabilities: Set<RequiredCapability>,
)

@JsonDeserialize
data class ChannelSpecV1(
    @Required
    var requiredCapabilities: Set<RequiredCapabilityV1>,
)

@Group("lmos.eclipse")
@Version("v2")
@Plural("channelrollouts")
@Singular("channelrollout")
@Kind("ChannelRollout")
@ShortNames("crl")
class ChannelRolloutResource :
    CustomResource<ChannelRolloutSpec, EmptyStatus>(),
    Namespaced

@Group("lmos.eclipse")
@Version("v1", storage = false)
@Plural("channelrollouts")
@Singular("channelrollout")
@Kind("ChannelRollout")
@ShortNames("crl")
class ChannelRolloutResourceV1 :
    CustomResource<ChannelRolloutSpec, EmptyStatus>(),
    Namespaced

data class StableChannel(
    var name: String,
    var weight: Int = 0,
)

data class CanaryChannel(
    var name: String,
    var weight: Int = 0,
)

data class Canary(
    var canaryChannel: CanaryChannel,
    var stableChannel: StableChannel,
)

data class Strategy(
    var canary: Canary,
)

data class ChannelRolloutSpec(
    var strategy: Strategy,
)

@Group("lmos.eclipse")
@Version("v2")
@Plural("channelroutings")
@Singular("channelrouting")
@Kind("ChannelRouting")
@ShortNames("cr")
class ChannelRoutingResource :
    CustomResource<ChannelRoutingSpec, EmptyStatus>(),
    Namespaced

@Group("lmos.eclipse")
@Version("v1", storage = false)
@Plural("channelroutings")
@Singular("channelrouting")
@Kind("ChannelRouting")
@ShortNames("cr")
class ChannelRoutingResourceV1 :
    CustomResource<ChannelRoutingSpecV1, EmptyStatus>(),
    Namespaced

data class ChannelRoutingSpec(
    var capabilityGroups: Set<CapabilityGroup> = setOf(),
)

data class ChannelRoutingSpecV1(
    var capabilityGroups: Set<CapabilityGroupV1> = setOf(),
)

data class ChannelRoutingStatus(
    @PrinterColumn(name = "RESOLVE_STATUS")
    var resolveStatus: ResolveStatus = ResolveStatus.UNRESOLVED,
    var unresolvedRequiredCapabilities: List<RequiredCapability> = emptyList(),
)

data class CapabilityGroup(
    @Required
    var id: String,
    @Required
    var name: String,
    @Required
    var capabilities: Set<ChannelRoutingCapability>,
    var description: String = "",
)

data class CapabilityGroupV1(
    @Required
    var name: String,
    @Required
    var capabilities: Set<ChannelRoutingCapabilityV1>,
    var description: String = "",
)

data class ChannelRoutingCapability(
    @Required
    var id: String,
    @Required
    var name: String,
    @Required
    var requiredVersion: String,
    @Required
    var providedVersion: String,
    @Required
    var host: String,
    var description: String = "",
) {
    constructor(wire: Wire<AgentResource>) : this(
        id = wire.providedCapability.id,
        name = wire.providedCapability.name,
        requiredVersion = wire.requiredCapability.version,
        providedVersion = wire.providedCapability.version,
        description = wire.providedCapability.description,
        host = "${wire.provider.metadata.name}.${wire.provider.metadata.namespace}.svc.cluster.local",
    )
}

data class ChannelRoutingCapabilityV1(
    @Required
    var name: String,
    @Required
    var requiredVersion: String,
    @Required
    var providedVersion: String,
    @Required
    var host: String,
    var description: String = "",
) {
    constructor(wire: Wire<AgentResource>) : this(
        name = wire.providedCapability.name,
        requiredVersion = wire.requiredCapability.version,
        providedVersion = wire.providedCapability.version,
        description = wire.providedCapability.description,
        host = "${wire.provider.metadata.name}.${wire.provider.metadata.namespace}.svc.cluster.local",
    )
}

object Labels {
    const val CHANNEL: String = "channel"
    const val TENANT: String = "tenant"
    const val VERSION: String = "version"
    const val SUBSET: String = "subset"
}
