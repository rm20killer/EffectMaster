package me.m64diamondstar.effectmaster.shows.effect

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import me.m64diamondstar.effectmaster.EffectMaster
import me.m64diamondstar.effectmaster.locations.LocationUtils
import me.m64diamondstar.effectmaster.shows.EffectShow
import me.m64diamondstar.effectmaster.shows.utils.*
import me.m64diamondstar.effectmaster.shows.utils.Effect
import org.bukkit.*
import org.bukkit.Particle
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.*
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import java.util.*
import kotlin.random.Random


class ItemCircleTrail() : Effect() {

    override fun execute(players: List<Player>?, effectShow: EffectShow, id: Int, settings: Set<ShowSetting>) {
        try {
            val location =
                if (settings.any { it.identifier == ShowSetting.Identifier.PLAY_AT }) {
                    LocationUtils.getRelativeLocationFromString(
                        getSection(effectShow, id).getString("Location")!!,
                        effectShow.centerLocation ?: return
                    )
                        ?.add(settings.find { it.identifier == ShowSetting.Identifier.PLAY_AT }!!.value as Location)
                        ?: return
                } else
                    LocationUtils.getLocationFromString(getSection(effectShow, id).getString("Location")!!) ?: return

            // Doesn't need to play the show if it can't be viewed
            if (!location.chunk.isLoaded || Bukkit.getOnlinePlayers().isEmpty())
                return

            val material = if (getSection(effectShow, id).get("Material") != null) Material.valueOf(
                getSection(effectShow, id).getString("Material")!!.uppercase()
            ) else Material.STONE
            val customModelData =
                if (getSection(effectShow, id).get("CustomModelData") != null) getSection(
                    effectShow,
                    id
                ).getInt("CustomModelData") else 0
            val velocity =
                if (getSection(effectShow, id).get("Velocity") != null)
                    if (LocationUtils.getVectorFromString(getSection(effectShow, id).getString("Velocity")!!) != null)
                        LocationUtils.getVectorFromString(getSection(effectShow, id).getString("Velocity")!!)!!
                    else Vector(0.0, 0.0, 0.0)
                else Vector(0.0, 0.0, 0.0)
            val duration = if (getSection(effectShow, id).get("Duration") != null) getSection(
                effectShow,
                id
            ).getInt("Duration") else {
                if (getSection(effectShow, id).get("Length") != null) getSection(
                    effectShow,
                    id
                ).getInt("Length") else 20
            }
            val randomizer =
                if (getSection(effectShow, id).get("Randomizer") != null) getSection(
                    effectShow,
                    id
                ).getDouble("Randomizer") / 10 else 0.0
            val amount =
                if (getSection(effectShow, id).get("Amount") != null) getSection(effectShow, id).getInt("Amount") else 1
            val lifetime = if (getSection(effectShow, id).get("Lifetime") != null) getSection(
                effectShow,
                id
            ).getInt("Lifetime") else 40
            val particle = getSection(effectShow, id).getString("Particle")?.let { Particle.valueOf(it.uppercase()) } ?: return
            val radius = getSection(effectShow, id).getDouble("Radius") ?: 2.0 // Radius of the circle
            val gravity = getSection(effectShow, id).getDouble("Gravity") ?: 2.0
            object : BukkitRunnable() {
                var c = 0
                val items = mutableListOf<Item>()

                override fun run() {
                    if (c < duration) {

                        repeat(amount) {
                            //circle
                            val angle = (2 * Math.PI * it) / amount
                            val x = radius * Math.cos(angle)
                            val z = radius * Math.sin(angle)
                            val spawnLocation = location.clone().add(x, 0.0, z)

                            // Create item
                            val item = location.world!!.spawnEntity(spawnLocation, EntityType.ITEM) as Item
                            item.pickupDelay = Integer.MAX_VALUE
                            item.isPersistent = false
                            item.persistentDataContainer.set(
                                NamespacedKey(EffectMaster.plugin(), "effectmaster-entity"),
                                PersistentDataType.BOOLEAN, true
                            )
                            item.itemStack = ItemStack(material)
                            if (item.itemStack.itemMeta != null) {
                                val meta = item.itemStack.itemMeta!!
                                meta.setCustomModelData(customModelData)
                                item.itemStack.itemMeta = meta
                            }

                            // Fix velocity
                            //calc outwards velocity
                            val outwardVector = Vector(x, 0.0, z).normalize() // Direction away from center
                            val finalVelocity = outwardVector.multiply(velocity.length()) // Scale to original velocity magnitude

                            if (randomizer != 0.0) {
                                val randomX = Random.nextInt(0, 1000).toDouble() / 1000 * randomizer * 2 - randomizer
                                val randomY = Random.nextInt(0, 1000).toDouble() / 1000 * randomizer * 2 - randomizer / 3
                                val randomZ = Random.nextInt(0, 1000).toDouble() / 1000 * randomizer * 2 - randomizer
                                item.velocity = finalVelocity.add(Vector(randomX, randomY, randomZ))
                            } else {
                                item.velocity = finalVelocity
                            }



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
                            items.add(item)
                            // Remove item after given time
                            Bukkit.getScheduler().scheduleSyncDelayedTask(EffectMaster.plugin(), {
                                if (item.isValid) {
                                    item.remove()
                                    ShowUtils.removeDroppedItem(item)
                                    items.remove(item)
                                }
                            }, lifetime.toLong())
                        }
                    }
                    for (item in items.toList()) {
                        if (item.isValid) {
                            val itemLocation = item.location
                            itemLocation.world!!.spawnParticle(particle, itemLocation,1, 0.0, 0.0, 0.0,0.0, null, true)
                        }
                        else
                        {
                            items.remove(item)
                        }
                    }
                    if (items.isEmpty() && c >= duration) {
                        this.cancel()
                    }
                    c++
                }
            }.runTaskTimer(EffectMaster.plugin(), 0L, 1L)
        } catch (_: Exception) {
            EffectMaster.plugin().logger.warning("Couldn't play Item Fountain with ID $id from ${effectShow.getName()} in category ${effectShow.getCategory()}.")
            EffectMaster.plugin().logger.warning("Possible errors: ")
            EffectMaster.plugin().logger.warning("- The item you entered doesn't exist.")
            EffectMaster.plugin().logger.warning("- The location/world doesn't exist or is unloaded")
        }
    }

    override fun getIdentifier(): String {
        return "ITEM_CIRCLE_TRAIL"
    }

    override fun getDisplayMaterial(): Material {
        return Material.LIME_WOOL
    }

    override fun getDescription(): String {
        return "Spawns a circle of dropped items and add a trail."
    }

    override fun isSync(): Boolean {
        return true
    }

    override fun getDefaults(): List<Parameter> {
        val list = ArrayList<Parameter>()
        list.add(
            Parameter(
                "Location",
                "world, 0, 0, 0",
                DefaultDescriptions.LOCATION,
                { it }) { LocationUtils.getLocationFromString(it) != null })
        list.add(
            Parameter(
                "Radius",
                2.0,
                "The radius of the circle where items will spawn.",
                { it.toDouble() }) { it.toDoubleOrNull() != null && it.toDouble() >= 0.0 })
        list.add(
            Parameter(
                "Velocity",
                "0, 0, 0",
                DefaultDescriptions.VELOCITY,
                { it }) { LocationUtils.getVectorFromString(it) != null })
        list.add(
            Parameter(
                "Material",
                "BLUE_STAINED_GLASS",
                DefaultDescriptions.BLOCK,
                { it.uppercase() }) { Material.entries.any { mat -> it.equals(mat.name, ignoreCase = true) } })
        list.add(
            Parameter(
                "CustomModelData",
                0,
                DefaultDescriptions.BLOCK_DATA,
                { it.toInt() }) { it.toIntOrNull() != null && it.toInt() >= 0 })
        list.add(
            Parameter(
                "Duration",
                20,
                DefaultDescriptions.DURATION,
                { it.toInt() }) { it.toIntOrNull() != null && it.toInt() >= 0 })
        list.add(
            Parameter(
                "Lifetime",
                40,
                "How long the item should stay before they get removed. Items don't automatically get removed when they hit the ground.",
                { it.toInt() }) { it.toIntOrNull() != null && it.toInt() >= 0 })
        list.add(
            Parameter(
                "Randomizer",
                0.0,
                "This randomizes the value of the velocity a bit. The higher the value, the more the velocity changes. It's best keeping this between 0 and 1.",
                { it.toDouble() }) { it.toDoubleOrNull() != null && it.toDouble() >= 0.0 })
        list.add(
            Parameter(
                "Amount",
                1,
                "The amount of blocks to spawn each tick.",
                { it.toInt() }) { it.toIntOrNull() != null && it.toInt() >= 0 })
        list.add(
            Parameter(
                "Delay",
                0,
                DefaultDescriptions.DELAY,
                { it.toInt() }) { it.toLongOrNull() != null && it.toLong() >= 0 })
        list.add(Parameter("Particle", "END_ROD", DefaultDescriptions.PARTICLE, {it.uppercase()}) { it in Particle.entries.map { it.name } })
        list.add(
            Parameter(
                "Gravity",
                0.08,
                "The gravity of the item.",
                { it.toDouble() }) { it.toDoubleOrNull() != null && it.toDouble() >= 0.0 })
        return list
    }
}