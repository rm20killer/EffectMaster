package me.m64diamondstar.effectmaster.shows.effect

import me.m64diamondstar.effectmaster.EffectMaster
import me.m64diamondstar.effectmaster.shows.utils.Effect
import me.m64diamondstar.effectmaster.shows.EffectShow
import me.m64diamondstar.effectmaster.locations.LocationUtils
import me.m64diamondstar.effectmaster.shows.utils.DefaultDescriptions
import me.m64diamondstar.effectmaster.shows.utils.Parameter
import me.m64diamondstar.effectmaster.shows.utils.ShowSetting
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player

class FillBlock() : Effect() {

    override fun execute(players: List<Player>?, effectShow: EffectShow, id: Int, settings: Set<ShowSetting>) {

        try {
            val fromLocation =
                if(settings.any { it.identifier == ShowSetting.Identifier.PLAY_AT }){
                    LocationUtils.getRelativeLocationFromString(getSection(effectShow, id).getString("FromLocation")!!,
                        effectShow.centerLocation ?: return)
                        ?.add(settings.find { it.identifier == ShowSetting.Identifier.PLAY_AT }!!.value as Location) ?: return
                }else
                    LocationUtils.getLocationFromString(getSection(effectShow, id).getString("FromLocation")!!) ?: return
            val toLocation =
                if(settings.any { it.identifier == ShowSetting.Identifier.PLAY_AT }){
                    LocationUtils.getRelativeLocationFromString(getSection(effectShow, id).getString("ToLocation")!!,
                        effectShow.centerLocation ?: return)
                        ?.add(settings.find { it.identifier == ShowSetting.Identifier.PLAY_AT }!!.value as Location) ?: return
                }else
                    LocationUtils.getLocationFromString(getSection(effectShow, id).getString("ToLocation")!!) ?: return
            val material =
                if (getSection(effectShow, id).get("Block") != null) Material.valueOf(getSection(effectShow, id).getString("Block")!!.uppercase()) else Material.STONE

            if(!material.isBlock) {
                EffectMaster.plugin().logger.warning("Couldn't play effect with ID $id from ${effectShow.getName()} in category ${effectShow.getCategory()}.")
                EffectMaster.plugin().logger.warning("The material entered is not a block.")
                return
            }

            val blockData = if(getSection(effectShow, id).get("BlockData") != null)
                Bukkit.createBlockData(material, getSection(effectShow, id).getString("BlockData")!!) else material.createBlockData()
            val duration = if (getSection(effectShow, id).get("Duration") != null) getSection(effectShow, id).getLong("Duration") else 0

            val normalMap = HashMap<Location, BlockData>()

            for (x in fromLocation.blockX.coerceAtLeast(toLocation.blockX) downTo toLocation.blockX.coerceAtMost(
                fromLocation.blockX
            )) {
                for (y in fromLocation.blockY.coerceAtLeast(toLocation.blockY) downTo toLocation.blockY.coerceAtMost(
                    fromLocation.blockY
                )) {
                    for (z in fromLocation.blockZ.coerceAtLeast(toLocation.blockZ) downTo toLocation.blockZ.coerceAtMost(
                        fromLocation.blockZ
                    )) {
                        val location = Location(fromLocation.world, x.toDouble(), y.toDouble(), z.toDouble())
                        if(players != null) {
                            players.forEach {
                                it.sendBlockChange(location, blockData)
                            }
                        }else{
                            for (player in Bukkit.getOnlinePlayers())
                                player.sendBlockChange(location, blockData)
                        }
                        normalMap[location] = location.block.blockData
                    }
                }
            }

            Bukkit.getScheduler().scheduleSyncDelayedTask(EffectMaster.plugin(), {
                if (players != null &&  EffectMaster.isProtocolLibLoaded){
                    players.forEach {
                        for (loc in normalMap.keys)
                            it.sendBlockChange(loc, normalMap[loc]!!)
                    }
                }else{
                    for (player in Bukkit.getOnlinePlayers())
                        for (loc in normalMap.keys)
                            player.sendBlockChange(loc, normalMap[loc]!!)
                }
            }, duration)
        }catch (_: IllegalArgumentException){
            EffectMaster.plugin().logger.warning("Couldn't play Fill Block with ID $id from ${effectShow.getName()} in category ${effectShow.getCategory()}.")
            EffectMaster.plugin().logger.warning("The Block entered doesn't exist or the BlockData doesn't exist.")
        }
    }

    override fun getIdentifier(): String {
        return "FILL_BLOCK"
    }

    override fun getDisplayMaterial(): Material {
        return Material.POLISHED_ANDESITE
    }

    override fun getDescription(): String {
        return "Fill a cubic area with blocks."
    }

    override fun isSync(): Boolean {
        return true
    }

    override fun getDefaults(): List<Parameter> {
        val list = ArrayList<Parameter>()
        list.add(Parameter("FromLocation", "world, 0, 0, 0", "The first corner of the cuboid selection.", {it}) { LocationUtils.getLocationFromString(it) != null })
        list.add(Parameter("ToLocation", "world, 1, 1, 1", "The second corner of the cuboid selection.", {it}) { LocationUtils.getLocationFromString(it) != null })
        list.add(Parameter("Block", "STONE", DefaultDescriptions.BLOCK, {it.uppercase()}) { Material.entries.any { mat -> it.equals(mat.name, ignoreCase = true) } })
        list.add(Parameter("BlockData", "[]", DefaultDescriptions.BLOCK_DATA, {it}) { true })
        list.add(Parameter("Duration", 100, DefaultDescriptions.DURATION, {it.toInt()}) { it.toIntOrNull() != null && it.toInt() >= 0 })
        list.add(Parameter("Delay", 0, DefaultDescriptions.DELAY, {it.toInt()}) { it.toLongOrNull() != null && it.toLong() >= 0 })
        return list
    }
}