/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2024 Skytils
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package gg.skytils.skytilsmod.features.impl.crimson

import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.Companion.client
import gg.skytils.skytilsmod.Skytils.Companion.domain
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.events.impl.CheckRenderEntityEvent
import gg.skytils.skytilsmod.events.impl.PacketEvent
import gg.skytils.skytilsmod.utils.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S02PacketChat
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import java.util.UUID

object KuudraFeatures {
    var kuudraOver = false
    var myFaction: CrimsonFaction? = null
    private val factionRegex = Regex("§r§b§l(?<faction>\\w+) Reputation:§r")

    init {
        tickTimer(20, repeats = true) {
            if (Utils.inSkyblock && SBInfo.mode == SkyblockIsland.CrimsonIsle.mode) {
                TabListUtils.tabEntries.filter { it.second.endsWith(" Reputation:§r") }.forEach {
                    val faction = factionRegex.find(it.second)?.groupValues?.get(1) ?: return@forEach
                    myFaction = CrimsonFaction.entries.find { it.displayName == faction }
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Unload) {
        kuudraOver = false
    }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (event.entity !is EntityArmorStand || SBInfo.mode != SkyblockIsland.KuudraHollow.mode) return
        if (Skytils.config.kuudraHideNonNametags && !kuudraOver && !Keyboard.isKeyDown(Keyboard.KEY_LMENU)) {
            if (event.entity.isInvisible && !event.entity.alwaysRenderNameTag) {
                event.isCanceled = true
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onPacket(event: PacketEvent.ReceiveEvent) {
        if (SBInfo.mode != SkyblockIsland.KuudraHollow.mode) return
        if (event.packet is S02PacketChat) {
            if (event.packet.chatComponent.unformattedText.stripControlCodes().trim() == "KUUDRA DOWN!") {
                kuudraOver = true
            }
        }
    }

    suspend fun getAttributePricedItem(item: ItemStack): AttributePricedItem? {
        val extraAttr = ItemUtil.getExtraAttributes(item) ?: return null
        val attributes = extraAttr.getCompoundTag("attributes")
        if (attributes.hasNoTags()) return null
        return client.get("https://${domain}/api/auctions/kuudra/item_price") {
            attributes.keySet.forEachIndexed { i, attr ->
                parameter("attr${i+1}", "${attr}_${attributes.getInteger(attr)}")
            }
            parameter("item", ItemUtil.getSkyBlockItemID(extraAttr))
        }.body()
    }


    /**
     * @param id the ID of the lowest priced auction
     * @param price the price of the lowest priced auction
     * @param timestamp if [id] is `null`, the timestamp of when [price] was last updated
     */
    @Serializable
    data class AttributePricedItem(
        val id: String,
        val price: Double,
        val timestamp: Long?
    )
 }

enum class CrimsonFaction(val keyMaterial: String) {
    BARBARIAN("ENCHANTED_RED_SAND"),
    MAGE("ENCHANTED_MYCELIUM");

    val displayName = name.toTitleCase()
}