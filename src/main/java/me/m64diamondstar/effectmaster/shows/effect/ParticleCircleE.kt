package me.m64diamondstar.effectmaster.shows.effect

import hm.zelha.particlesfx.particles.ParticleDustColored
import hm.zelha.particlesfx.shapers.ParticleCircle
import hm.zelha.particlesfx.util.Color
import hm.zelha.particlesfx.util.LocationSafe
import me.m64diamondstar.effectmaster.EffectMaster
import me.m64diamondstar.effectmaster.locations.LocationUtils
import me.m64diamondstar.effectmaster.shows.EffectShow
import me.m64diamondstar.effectmaster.shows.utils.DefaultDescriptions
import me.m64diamondstar.effectmaster.shows.utils.Effect
import me.m64diamondstar.effectmaster.shows.utils.Parameter
import me.m64diamondstar.effectmaster.shows.utils.ShowSetting
import me.m64diamondstar.effectmaster.utils.Colors
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import kotlin.math.roundToInt

class ParticleCircleE() : Effect() {

    override fun execute(players: List<Player>?, effectShow: EffectShow, id: Int, settings: Set<ShowSetting>) {
        try {
            val location =
                if(settings.any { it.identifier == ShowSetting.Identifier.PLAY_AT }){
                    LocationUtils.getRelativeLocationFromString(getSection(effectShow, id).getString("Location")!!,
                        effectShow.centerLocation ?: return)
                        ?.add(settings.find { it.identifier == ShowSetting.Identifier.PLAY_AT }!!.value as Location) ?: return
                }else
                    LocationUtils.getLocationFromString(getSection(effectShow, id).getString("Location")!!) ?: return
            val particle = getSection(effectShow, id).getString("Particle")?.let { Particle.valueOf(it.uppercase()) } ?: return
            val amount = if (getSection(effectShow, id).get("Amount") != null) getSection(effectShow, id).getInt("Amount") else 0
            val color = Colors.getJavaColorFromString(getSection(effectShow, id).getString("Color")!!) ?: java.awt.Color(0, 0, 0)
            val radius = getSection(effectShow, id).getDouble("Radius") ?: 2.0  // Default radius
            val pitch = getSection(effectShow, id).getDouble("Pitch") ?: 0.0
            val yaw = getSection(effectShow, id).getDouble("Yaw") ?: 0.0
            val roll = getSection(effectShow, id).getDouble("Roll") ?: 0.0
            val particleFrequency = getSection(effectShow, id).getInt("ParticleFrequency") ?: 50

            val duration = if (getSection(effectShow, id).get("Duration") != null) getSection(effectShow, id).getInt("Duration") else {
                if (getSection(effectShow, id).get("Length") != null) getSection(effectShow, id).getInt("Length") else 20
            }

            val force = if (getSection(effectShow, id).get("Force") != null) getSection(effectShow, id).getBoolean("Force") else false
            val extra = if (amount == 0) 1.0 else 0.0
            val particleSFXP = ParticleDustColored(Color(color.red, color.green, color.blue))
            val circle = ParticleCircle(
                particleSFXP, // Use the converted Particle enum value here
                LocationSafe(location),
                0.0,
                0.0, // Assuming zRadius is the same as radius
                pitch,
                yaw,
                roll,
                particleFrequency
            )
            var currentRadius = 0.0
            object: BukkitRunnable(){
                var c = 0
                override fun run() {
                    if(c == duration){
                        circle.stop()
                        this.cancel()
                        return
                    }
                    currentRadius = (radius / duration) * c
                    circle.xRadius = currentRadius
                    circle.zRadius = currentRadius
                    circle.display()
                    c++
                }
            }.runTaskTimerAsynchronously(EffectMaster.plugin(), 0L, 1L)


        }catch (e: Exception){
            EffectMaster.plugin().logger.warning(e.message)
            EffectMaster.plugin().logger.warning("Couldn't play Particle with ID $id from ${effectShow.getName()} in category ${effectShow.getCategory()}.")
            EffectMaster.plugin().logger.warning("Possible errors: ")
            EffectMaster.plugin().logger.warning("- The particle you entered doesn't exist.")
            EffectMaster.plugin().logger.warning("- The location/world doesn't exist or is unloaded")
        }
    }

    override fun getIdentifier(): String {
        return "PARTICLE_CIRCLE"
    }

    override fun getDisplayMaterial(): Material {
        return Material.SUNFLOWER
    }

    override fun getDescription(): String {
        return "Spawns a circle of particle."
    }

    override fun isSync(): Boolean {
        return false
    }

    override fun getDefaults(): List<Parameter> {
        val list = ArrayList<Parameter>()
        list.add(Parameter("Particle", "CLOUD", "NOT SETUP YET, USE COLOUR", { it.uppercase() }) { it in Particle.entries.map { it.name } })
        list.add(Parameter("Location", "world, 0, 0, 0", DefaultDescriptions.LOCATION, { it }) { LocationUtils.getLocationFromString(it) != null })
        list.add(Parameter("Amount", 50, "The amount of particles to spawn.", { it.toInt() }) { it.toIntOrNull() != null && it.toInt() >= 0 })
        list.add(Parameter("Radius", 2.0, "The radius of the particle circle.", { it.toDouble() }) { it.toDoubleOrNull() != null && it.toDouble() >= 0.0 })
        list.add(Parameter("Pitch", 0.0, "The pitch rotation of the particle circle.", { it.toDouble() }) { it.toDoubleOrNull() != null })
        list.add(Parameter("Yaw", 0.0, "The yaw rotation of the particle circle.", { it.toDouble() }) { it.toDoubleOrNull() != null })
        list.add(Parameter("Roll", 0.0, "The roll rotation of the particle circle.", { it.toDouble() }) { it.toDoubleOrNull() != null })
        list.add(Parameter("ParticleFrequency", 50, "The frequency of particles in the circle.", { it.toInt() }) { it.toIntOrNull() != null && it.toInt() > 0 })
        list.add(Parameter("Force", false, "Whether the particle should be forcibly rendered by the player or not.", { it.toBoolean() }) { it.toBooleanStrictOrNull() != null })
        list.add(Parameter("Size", 0.5f, "The size of the particle, only works for REDSTONE, SPELL_MOB and SPELL_MOB_AMBIENT.", { it.toFloat() }) { it.toFloatOrNull() != null && it.toFloat() >= 0.0 })
        list.add(Parameter("Color", "0, 0, 0", "The color of the particle, only works for REDSTONE, SPELL_MOB and SPELL_MOB_AMBIENT. Formatted in RGB.", { it }) { Colors.getJavaColorFromString(it) != null })
        list.add(Parameter("Block", "STONE", "The block id of the particle, only works for BLOCK_CRACK, BLOCK_DUST, FALLING_DUST and ITEM_CRACK.", { it.uppercase() }) { Material.entries.any { mat -> it.equals(mat.name, ignoreCase = true) } })
        list.add(Parameter("Duration", 20, DefaultDescriptions.DURATION, {it.toInt()}) { it.toLongOrNull() != null && it.toLong() >= 0 })
        list.add(Parameter("Delay", 0, DefaultDescriptions.DELAY, { it.toInt() }) { it.toLongOrNull() != null && it.toLong() >= 0 })
        return list
    }

}