package me.m64diamondstar.effectmaster
import com.tcoded.folialib.FoliaLib
import hm.zelha.particlesfx.util.ParticleSFX
import me.m64diamondstar.effectmaster.commands.EffectMasterCommand
import me.m64diamondstar.effectmaster.commands.EffectMasterTabCompleter
import me.m64diamondstar.effectmaster.commands.utils.SubCommandRegistry
import me.m64diamondstar.effectmaster.editor.listeners.ParameterChatListener
import me.m64diamondstar.effectmaster.editor.listeners.LeaveListener
import me.m64diamondstar.effectmaster.editor.listeners.SettingsChatListener
import me.m64diamondstar.effectmaster.shows.ShowLooper
import me.m64diamondstar.effectmaster.shows.listeners.ChunkListener
import me.m64diamondstar.effectmaster.shows.listeners.EntityChangeBlockListener
import me.m64diamondstar.effectmaster.shows.listeners.ItemMergeListener
import me.m64diamondstar.effectmaster.shows.utils.ShowUtils
import me.m64diamondstar.effectmaster.traincarts.SignRegistry
import me.m64diamondstar.effectmaster.utils.gui.GuiListener
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

class EffectMaster : JavaPlugin() {

    companion object {
        var isTrainCartsLoaded: Boolean = false
        var isAnimatronicsLoaded: Boolean = false
        var isProtocolLibLoaded: Boolean = false
        private lateinit var foliaLib: FoliaLib

        fun plugin(): Plugin {
            return Bukkit.getPluginManager().getPlugin("EffectMaster")!! as EffectMaster
        }

        fun shortServerVersion(): Int {
            return Bukkit.getServer().bukkitVersion.split(".")[1].split("-")[0].toInt()
        }

        fun getFoliaLib(): FoliaLib = foliaLib

    }

    override fun onEnable() {
        foliaLib = FoliaLib(this)

        // Load config.yml
        saveDefaultConfig()

        // Load listeners
        this.server.pluginManager.registerEvents(EntityChangeBlockListener(), this)
        this.server.pluginManager.registerEvents(ItemMergeListener(), this)
        this.server.pluginManager.registerEvents(GuiListener(), this)
        this.server.pluginManager.registerEvents(ParameterChatListener(), this)
        this.server.pluginManager.registerEvents(SettingsChatListener(), this)
        this.server.pluginManager.registerEvents(LeaveListener(), this)
        this.server.pluginManager.registerEvents(ChunkListener(), this)

        // Load commands
        this.getCommand("effectmaster")?.setExecutor(EffectMasterCommand())
        this.getCommand("effectmaster")?.tabCompleter = EffectMasterTabCompleter()

        SubCommandRegistry.loadSubCommands()

        // Load version
        this.logger.info("Detected server version ${this.server.bukkitVersion}. Going with short version ${shortServerVersion()}")

        // Try to load dependencies
        loadDependencies()
        ParticleSFX.setPlugin(this)
        // Try to register TrainCarts signs (does nothing if plugin isn't loaded)
        if(isTrainCartsLoaded) {
            SignRegistry.registerSigns()
        }

        // Enable bStats
        if(!isFolia())
            Metrics(this, 17340)

        // Check if there is a new update
        if(config.getBoolean("notify-updates")) {
            UpdateChecker(this, 107260).getVersion { version: String? ->
                if (description.version == version) {
                    logger.info("You're running the latest version.")
                } else {
                    logger.info("There is a new update available. You are running ${this.description.version}, the new version is $version")
                    logger.info("Please download the new version here: ")
                    logger.info("https://www.spigotmc.org/resources/effectmaster-create-beautiful-shows-in-your-server.107260/")
                }
            }
        }

        // Initialize the show looper
        ShowLooper.initialize()
    }

    override fun onDisable() {
        if(isTrainCartsLoaded) {
            SignRegistry.unregisterSigns()
        }

        ShowUtils.getFallingBlocks().forEach {
            it.remove()
        }

        ShowUtils.getDroppedItems().forEach {
            it.remove()
        }

        this.logger.info("EffectMaster has successfully been disabled. Goodbye!")
    }

    private fun loadDependencies(){
        if(this.server.pluginManager.getPlugin("Train_Carts") != null) {
            isTrainCartsLoaded = true
            this.logger.info("Train Carts found.")
        }else{
            this.logger.info("Train Carts not found, continuing without it.")
        }

        if(this.server.pluginManager.getPlugin("Animatronics") != null){
            isAnimatronicsLoaded = true
            this.logger.info("Animatronics found.")
        }else{
            this.logger.info("Animatronics not found, continuing without it.")
        }

        if(this.server.pluginManager.getPlugin("ProtocolLib") != null){
            isProtocolLibLoaded = true
            this.logger.info("ProtocolLib found.")
        }else{
            this.logger.info("ProtocolLib not found, continuing without it.")
        }
    }

    private fun isFolia(): Boolean {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            return true
        } catch (_: ClassNotFoundException) {
            return false
        }
    }

}