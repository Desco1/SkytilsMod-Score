/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2023 Skytils
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
package gg.skytils.skytilsmod.features.impl.dungeons.solvers.terminals

import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.screen.GuiContainerBackgroundDrawnEvent
import gg.skytils.event.impl.screen.GuiContainerPreDrawSlotEvent
import gg.skytils.event.impl.screen.GuiContainerSlotClickEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.utils.SuperSecretSettings
import gg.skytils.skytilsmod.utils.Utils
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.EnumDyeColor
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.random.Random

object SelectAllColorSolver : EventSubscriber {

    @JvmField
    val shouldClick = hashSetOf<Int>()
    private var colorNeeded: String? = null
    private val colors by lazy {
        EnumDyeColor.entries.associateWith { it.getName().replace("_", " ").uppercase() }
    }

    override fun setup() {
        register(::onBackgroundDrawn)
        register(::onDrawSlot)
        register(::onSlotClick, EventPriority.High)
        register(::onTooltip)
    }

    fun onBackgroundDrawn(event: GuiContainerBackgroundDrawnEvent) {
        if (Skytils.config.selectAllColorTerminalSolver && Utils.inDungeons && event.container is ContainerChest && event.chestName.startsWith(
                "Select all the"
            )
        ) {
            val promptColor = colors.entries.find { (_, name) ->
                event.chestName.contains(name)
            }?.key?.unlocalizedName
            if (promptColor != colorNeeded) {
                colorNeeded = promptColor
                shouldClick.clear()
            }
            if (shouldClick.size == 0) {
                for (slot in event.container.inventorySlots) {
                    if (slot.inventory === mc.thePlayer?.inventory || !slot.hasStack) continue
                    val item = slot.stack ?: continue
                    if (item.isItemEnchanted) continue
                    if (slot.slotNumber < 9 || slot.slotNumber > 44 || slot.slotNumber % 9 == 0 || slot.slotNumber % 9 == 8) continue
                    if (SuperSecretSettings.bennettArthur) {
                        if (Random.nextInt(3) == 0) shouldClick.add(slot.slotNumber)
                    } else if (item.unlocalizedName.contains(colorNeeded!!)) {
                        shouldClick.add(slot.slotNumber)
                    }
                }
            } else {
                shouldClick.removeIf {
                    val slot = event.container.getSlot(it)
                    return@removeIf slot.hasStack && slot.stack.isItemEnchanted
                }
            }
        } else {
            shouldClick.clear()
            colorNeeded = null
        }
    }

    fun onDrawSlot(event: GuiContainerPreDrawSlotEvent) {
        if (!Utils.inDungeons) return
        if (!Skytils.config.selectAllColorTerminalSolver) return
        if (event.container is ContainerChest) {
            if (event.chestName.startsWith("Select all the")) {
                val slot = event.slot
                if (shouldClick.isNotEmpty() && slot.slotNumber !in shouldClick && slot.inventory !== mc.thePlayer.inventory) {
                    event.cancelled = true
                }
            }
        }
    }

    fun onSlotClick(event: GuiContainerSlotClickEvent) {
        if (!Utils.inDungeons || !Skytils.config.selectAllColorTerminalSolver || !Skytils.config.blockIncorrectTerminalClicks) return
        if (event.container is ContainerChest && event.chestName.startsWith("Select all the")) {
            if (shouldClick.isNotEmpty() && !shouldClick.contains(event.slotId)) event.cancelled = true
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.LOWEST)
    fun onTooltip(event: ItemTooltipEvent) {
        if (event.toolTip == null || !Utils.inDungeons || !Skytils.config.selectAllColorTerminalSolver) return
        val chest = mc.thePlayer.openContainer
        if (chest is ContainerChest) {
            val chestName = chest.lowerChestInventory.displayName.unformattedText
            if (chestName.startsWith("Select all the")) {
                event.toolTip.clear()
            }
        }
    }
}