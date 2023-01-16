package me.m64diamondstar.effectmaster.editor.listeners

import me.m64diamondstar.effectmaster.editor.effect.EditEffectGui
import me.m64diamondstar.effectmaster.editor.utils.EditingPlayers
import me.m64diamondstar.effectmaster.shows.utils.ParameterType
import me.m64diamondstar.effectmaster.shows.utils.Show
import me.m64diamondstar.effectmaster.utils.Colors
import me.m64diamondstar.effectmaster.utils.Prefix
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class ChatListener: Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onChat(event: AsyncPlayerChatEvent){
        val player = event.player
        if(!EditingPlayers.contains(player)) return

        event.isCancelled = true

        val showCategory = EditingPlayers.get(player)!!.first.getCategory()
        val showName = EditingPlayers.get(player)!!.first.getName()
        val show = Show(showCategory, showName)

        val id = EditingPlayers.get(player)!!.second
        val parameter = EditingPlayers.get(player)!!.third

        if(event.message.equals("cancel", ignoreCase = true)){
            player.sendMessage(Colors.format(Prefix.PrefixType.SUCCESS.toString() + "Cancelled edit."))
            val editEffectGui = EditEffectGui(player, id, show)
            editEffectGui.open()
            EditingPlayers.remove(player)
        }else{

            var value = event.message as Any

            if(event.message.toDoubleOrNull() != null)
                value = event.message.toDouble()
            if(event.message.toBooleanStrictOrNull() != null)
                value = event.message.toBooleanStrict()

            if(ParameterType.valueOf(parameter.uppercase()).getFormat().isPossible(value)){
                show.getEffect(id)!!.getSection().set(parameter, value)
                show.reloadConfig()

                player.sendMessage(Colors.format(Prefix.PrefixType.SUCCESS.toString() + "Edited parameter."))
                val editEffectGui = EditEffectGui(player, id, show)
                editEffectGui.open()
                EditingPlayers.remove(player)
            }else{
                player.sendMessage(Colors.format(Prefix.PrefixType.ERROR.toString() + "The value entered is not possible."))
                player.sendMessage(Colors.format(Prefix.PrefixType.ERROR.toString() + "You need to enter a $parameter, please read " +
                        "the info above."))
            }
        }

    }

}