package me.m64diamondstar.effectmaster.shows.type

import me.m64diamondstar.effectmaster.EffectMaster
import me.m64diamondstar.effectmaster.shows.utils.Effect
import me.m64diamondstar.effectmaster.shows.utils.EffectShow
import me.thundertnt33.animatronics.api.Group
import org.bukkit.entity.Player

class AnimatronicGroup(effectShow: EffectShow, private val id: Int) : Effect(effectShow, id) {

    override fun execute(players: List<Player>?) {
        if(!EffectMaster.isAnimatronicsLoaded){
            EffectMaster.plugin.logger.warning("The show \"${getShow().getName()}\" in category \"${getShow().getCategory()}\" " +
                    "tried to play an Animatronic Group at ID $id while that plugin is not enabled. Please add the plugin to the server or remove the effect.")
            return
        }

        val name = getSection().getString("Name")!!
        val group = Group(name)
        group.play()
    }

    override fun getType(): Type {
        return Type.ANIMATRONIC_GROUP
    }

    override fun isSync(): Boolean {
        return true
    }

    override fun getDefaults(): List<Pair<String, Any>> {
        val list = ArrayList<Pair<String, Any>>()
        list.add(Pair("Type", "ANIMATRONIC_GROUP"))
        list.add(Pair("Name", "anima"))
        list.add(Pair("Delay", 0))
        return list
    }
}