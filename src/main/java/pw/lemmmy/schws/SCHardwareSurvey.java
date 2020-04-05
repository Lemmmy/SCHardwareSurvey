package pw.lemmmy.schws;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;

// If you'd like to TeamView me to see what we're doing with this data, feel free to stop by: 531 488 166
// Tribute to MrSnoopDoge
// Based on MCSnooper
@Mod(modid = SCHardwareSurvey.MODID, name = SCHardwareSurvey.NAME, version = SCHardwareSurvey.VERSION)
public class SCHardwareSurvey {
    public static final String MODID = "schardwaresurvey";
    public static final String NAME = "SwitchCraft Hardware Survey";
    public static final String VERSION = "1.0";
    
    @Mod.Instance(MODID)
    public static SCHardwareSurvey INSTANCE;
    
    private static final int POPUP_DELAY = 200;
    
    static Logger LOG;
    File configDir;
    
    private StatsPersistence persistence;
    private StatsCollector collector;
    private boolean ticking = false, collected = false, dontCollect = false;
    private long timerTicks = 0;
    
    public SCHardwareSurvey() {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOG = event.getModLog();
        configDir = event.getModConfigurationDirectory();
    }
    
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        persistence = new StatsPersistence();
        persistence.loadProperties();
        
        if (persistence.isDone()) {
            dontCollect = true;
            return;
        }
        
        collector = new StatsCollector();
        collector.collectStats();
        
        collector.getStats().forEach((stat, value) -> LOG.info("Collected stat {} = {}", stat, value));
    }
    
    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (persistence.isDone()) {
            dontCollect = true;
            return;
        }
    
        collected = false;
        if (!ticking) ticking = true;
    }
    
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (dontCollect) return;
        if (!event.player.getEntityWorld().isRemote || event.phase != TickEvent.Phase.START) return;
        
        if (ticking) timerTicks++;
        
        if (!collected && timerTicks >= POPUP_DELAY) {
            ticking = false;
            collected = true;
            
            Minecraft.getMinecraft().displayGuiScreen(new GuiSurvey(persistence, collector));
        }
    }
}
