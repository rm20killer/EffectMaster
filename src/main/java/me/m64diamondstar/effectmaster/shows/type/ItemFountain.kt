package me.m64diamondstar.effectmaster.shows.type

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import me.m64diamondstar.effectmaster.EffectMaster
import me.m64diamondstar.effectmaster.shows.utils.Effect
import me.m64diamondstar.effectmaster.shows.EffectShow
import me.m64diamondstar.effectmaster.shows.utils.ShowUtils
import me.m64diamondstar.effectmaster.locations.LocationUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector

class ItemFountain() : Effect() {

    override fun execute(players: List<Player>?, effectShow: EffectShow, id: Int) {
        try {
            val location = LocationUtils.getLocationFromString(getSection(effectShow, id).getString("Location")!!) ?: return

            // Doesn't need to play the show if it can't be viewed
            if(!location.chunk.isLoaded || Bukkit.getOnlinePlayers().isEmpty())
                return

            val material = if (getSection(effectShow, id).get("Material") != null) Material.valueOf(
                getSection(effectShow, id).getString("Material")!!.uppercase()
            ) else Material.STONE
            val customModelData =
                if (getSection(effectShow, id).get("CustomModelData") != null) getSection(effectShow, id).getInt("CustomModelData") else 0
            val velocity =
                if (getSection(effectShow, id).get("Velocity") != null)
                    if (LocationUtils.getVectorFromString(getSection(effectShow, id).getString("Velocity")!!) != null)
                        LocationUtils.getVectorFromString(getSection(effectShow, id).getString("Velocity")!!)!!
                    else Vector(0.0, 0.0, 0.0)
                else Vector(0.0, 0.0, 0.0)
            val length = if (getSection(effectShow, id).get("Length") != null) getSection(effectShow, id).getInt("Length") else 1
            val randomizer =
                if (getSection(effectShow, id).get("Randomizer") != null) getSection(effectShow, id).getDouble("Randomizer") / 10 else 0.0
            val lifetime = if (getSection(effectShow, id).get("Lifetime") != null) getSection(effectShow, id).getInt("Lifetime") else 40

            object : BukkitRunnable() {
                var c = 0
                override fun run() {
                    if (c == length) {
                        this.cancel()
                        return
                    }

                    // Create item
                    val item = location.world!!.spawnEntity(location, EntityType.DROPPED_ITEM) as Item
                    item.pickupDelay = Integer.MAX_VALUE
                    item.isPersistent = false
                    item.itemStack = ItemStack(material)
                    if (item.itemStack.itemMeta != null) {
                        val meta = item.itemStack.itemMeta!!
                        meta.setCustomModelData(customModelData)
                        item.itemStack.itemMeta = meta
                    }

                    // Fix velocity
                    if (randomizer != 0.0)
                        item.velocity = Vector(
                            velocity.x + Math.random() * (randomizer * 2) - randomizer,
                            velocity.y + Math.random() * (randomizer * 2) - randomizer / 3,
                            velocity.z + Math.random() * (randomizer * 2) - randomizer
                        )
                    else
                        item.velocity = velocity

                    // Register dropped item (this prevents it from merging with others)
                    ShowUtils.addDroppedItem(item)

                    // Make private effect if needed
                    if (players != null && EffectMaster.isProtocolLibLoaded)
                        for (player in Bukkit.getOnlinePlayers()) {
                            if (!players.contains(player)) {
                                val protocolManager = ProtocolLibrary.getProtocolManager()
                                val removePacket = PacketContainer(PacketType.Play.Server.ENTITY_DESTROY)
                                removePacket.intLists.write(0, listOf(item.entityId))
                                protocolManager.sendServerPacket(player, removePacket)
                            }
                        }

                    // Remove item after given time
                    Bukkit.getScheduler().scheduleSyncDelayedTask(EffectMaster.plugin(), {
                        if(item.isValid)
                            item.remove()
                    }, lifetime.toLong())

                    c++
                }
            }.runTaskTimer(EffectMaster.plugin(), 0L, 1L)
        }catch (_: Exception){
            EffectMaster.plugin().logger.warning("Couldn't play Item Fountain with ID $id from ${effectShow.getName()} in category ${effectShow.getCategory()}.")
            EffectMaster.plugin().logger.warning("Possible errors: ")
            EffectMaster.plugin().logger.warning("- The item you entered doesn't exist.")
            EffectMaster.plugin().logger.warning("- The location/world doesn't exist or is unloaded")
        }
    }

    override fun getIdentifier(): String {
        return "ITEM_FOUNTAIN"
    }

    override fun getDisplayMaterial(): Material {
        return Material.SPLASH_POTION
    }

    override fun getDescription(): String {
        return "Spawns a fountain of dropped items with customizable velocity."
    }

    override fun isSync(): Boolean {
        return true
    }

    override fun getDefaults(): List<me.m64diamondstar.effectmaster.utils.Pair<String, Any>> {
        val list = ArrayList<me.m64diamondstar.effectmaster.utils.Pair<String, Any>>()
        list.add(me.m64diamondstar.effectmaster.utils.Pair("Type", "ITEM_FOUNTAIN"))
        list.add(me.m64diamondstar.effectmaster.utils.Pair("Location", "world, 0, 0, 0"))
        list.add(me.m64diamondstar.effectmaster.utils.Pair("Velocity", "0, 0, 0"))
        list.add(me.m64diamondstar.effectmaster.utils.Pair("Material", "BLUE_STAINED_GLASS"))
        list.add(me.m64diamondstar.effectmaster.utils.Pair("CustomModelData", 0))
        list.add(me.m64diamondstar.effectmaster.utils.Pair("Length", 20))
        list.add(me.m64diamondstar.effectmaster.utils.Pair("Lifetime", 40))
        list.add(me.m64diamondstar.effectmaster.utils.Pair("Randomizer", 0))
        list.add(me.m64diamondstar.effectmaster.utils.Pair("Delay", 0))
        return list
    }
}