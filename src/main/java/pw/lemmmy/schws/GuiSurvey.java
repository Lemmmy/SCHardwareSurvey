package pw.lemmmy.schws;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

@SideOnly(Side.CLIENT)
public class GuiSurvey extends GuiScreen {
    private static final int LOGO_WIDTH = 445, LOGO_HEIGHT = 96;
    private static final int PADDING = 12, MARGIN = 16, FULL_MARGIN = MARGIN * 2;
    
    private static final int BUTTON_ID_VIEW_DATA = 0,
                             BUTTON_ID_DONT_SEND = 1,
                             BUTTON_ID_CONFIRM = 2;
    
    public static final String HEADER_TEXT
        = "\u00a72\u00a7l\u00a7nSwitch\u00a7a\u00a7l\u00a7nCraft \u00a7f\u00a7l\u00a7nHardware Survey";
    private static final String MAIN_PARAGRAPH
        = "We need your help improving CC:Tweaked! To do so, we need to know what kind of hardware players have. "
        + "We've collected some basic information about your computer's OS, CPU, GPU and RAM.";
    private static final String LAST_TEXT
        = "Please confirm to \u00a7lanonymously\u00a7r submit this data to our server:";
    private static final String SC_URL = "https://hardware.switchcraft.pw";
    
    private final StatsPersistence persistence;
    private final StatsCollector collector;
    
    private ResourceLocation logo;
    
    public GuiSurvey(StatsPersistence persistence, StatsCollector collector) {
        this.persistence = persistence;
        this.collector = collector;
    }
    
    @Override
    public void initGui() {
        super.initGui();
        
        logo = new ResourceLocation(SCHardwareSurvey.MODID, "textures/gui/sc-logo.png");
        
        int y;
        addButton(getCenteredButton(
            BUTTON_ID_VIEW_DATA,
            width / 2, y = getBtnViewDataY(),
            "View collected data"
        ));
        y += fontRenderer.FONT_HEIGHT + (PADDING * 2) + 20;
    
        addButton(getCenteredButton(
            BUTTON_ID_DONT_SEND,
            (width / 2) - 50, y,
            "\u00a7cDon't send"
        ));
        addButton(getCenteredButton(
            BUTTON_ID_CONFIRM,
            (width / 2) + 50, y,
            "\u00a7a\u00a7lConfirm"
        ));
    }
    
    @Override
    public void updateScreen() {
        super.updateScreen();
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        // draw the logo
        mc.getTextureManager().bindTexture(logo);
        drawScaledCustomSizeModalRect(
            (width - (LOGO_WIDTH) / 2) / 2, PADDING,
            0, 0, LOGO_WIDTH, LOGO_HEIGHT,
            LOGO_WIDTH / 2, LOGO_HEIGHT / 2,
            512, 128
        );
        
        // header text
        drawCenteredString(fontRenderer, HEADER_TEXT, width / 2, (PADDING * 2) + (LOGO_HEIGHT / 2), 0xFFFFFF);
    
        // main paragraph
        int y = (PADDING * 4) + (LOGO_HEIGHT / 2);
        y = drawCenteredSplitString(
            MAIN_PARAGRAPH,
            width / 2, y,
            width - FULL_MARGIN
        );
        y += 18 + PADDING * 2;
        
        // please confirm text
        drawCenteredString(fontRenderer, LAST_TEXT, width / 2, y, 0xFFFFFF);
    
        // URL at bottom
        drawCenteredString(
            fontRenderer,
            "§7Data and source code: §9" + SC_URL,
            width / 2, height - PADDING - fontRenderer.FONT_HEIGHT,
            0xFFFFFF
        );
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        // Don't call super to disallow esc presses
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // handle clicks on the URL at the bottom
        if (mouseButton == 0 && mouseY > height - (PADDING * 2) - fontRenderer.FONT_HEIGHT) {
            openWebLink(SC_URL);
        } else {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }
    
    @Override
    protected void actionPerformed(GuiButton button) {
        if (!button.enabled) return;
        switch (button.id) {
            case BUTTON_ID_VIEW_DATA:
                mc.displayGuiScreen(new GuiCollectedData(this, collector));
                break;
                
            case BUTTON_ID_DONT_SEND:
                persistence.dontShow();
                mc.displayGuiScreen(null);
                break;
            
            case BUTTON_ID_CONFIRM:
                StatsSubmitter.submitStats(persistence, collector.getStats());
                mc.displayGuiScreen(null);
                break;
        }
    }
    
    private void openWebLink(String link) {
        try {
            URI url = new URI(link);
            Class<?> desktopClass = Class.forName("java.awt.Desktop");
            Object desktop = desktopClass.getMethod("getDesktop").invoke(null);
            desktopClass.getMethod("browse", URI.class).invoke(desktop, url);
        } catch (URISyntaxException | ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            SCHardwareSurvey.LOG.error("Error opening link:", e);
        }
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
    
    private GuiButton getCenteredButton(int id, int x, int y, String text) {
        int btnWidth = getButtonWidth(text);
        return new GuiButton(id, getCenteredButtonX(x, btnWidth), y, btnWidth, 20, text);
    }
    
    private int getButtonWidth(String text) {
        return fontRenderer.getStringWidth(text) + 16;
    }
    
    private int getCenteredButtonX(int x, int btnWidth) {
        return x - (btnWidth / 2);
    }
    
    private int drawCenteredSplitString(String str, int x, int y, int wrapWidth) {
        for (String s : fontRenderer.listFormattedStringToWidth(str, wrapWidth)) {
            drawCenteredString(fontRenderer, s, x, y, 0xFFFFFF);
            y += fontRenderer.FONT_HEIGHT;
        }
        return y;
    }
    
    private int getSplitStringHeight(String str, int wrapWidth) {
        return fontRenderer.listFormattedStringToWidth(str, wrapWidth).size() * fontRenderer.FONT_HEIGHT;
    }
    
    private int getBtnViewDataY() {
        return (PADDING * 4) + (LOGO_HEIGHT / 2) + getSplitStringHeight(HEADER_TEXT, width - FULL_MARGIN) + 28;
    }
}
