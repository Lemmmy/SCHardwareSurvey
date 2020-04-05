package pw.lemmmy.schws;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class GuiCollectedData extends GuiScreen {
    private static final int LIST_PADDING = 10, LIST_COL_X = 230;
    private static final int PADDING = 12, FOOTER_HEIGHT = 40;
    
    private static final String HEADER_TEXT = GuiSurvey.HEADER_TEXT;
    public static final int BUTTON_ID_DONE = 0;
    
    private final GuiSurvey lastScreen;
    
    private final Map<String, String> stats;
    private final List<String> keys = new ArrayList<>();
    private final List<String> values = new ArrayList<>();
    
    private StatList statList;
    
    public GuiCollectedData(GuiSurvey lastScreen, StatsCollector collector) {
        this.lastScreen = lastScreen;
        stats = collector.getStats();
    }
    
    @Override
    public void initGui() {
        keys.clear();
        values.clear();
        stats.forEach((k, v) -> {
            keys.add(k);
            values.add(v);
        });
        
        buttonList.add(new GuiButton(
            BUTTON_ID_DONE,
            (width / 2) - 100, height - (FOOTER_HEIGHT + 20) / 2,
            I18n.format("gui.done")
        ));
        
        statList = new StatList();
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // drawDefaultBackground();
        
        // stat list
        statList.drawScreen(mouseX, mouseY, partialTicks);
        
        // header text
        drawCenteredString(fontRenderer, HEADER_TEXT, width / 2, PADDING, 0xFFFFFF);
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        statList.handleMouseInput();
    }
    
    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled && button.id == BUTTON_ID_DONE) {
            mc.displayGuiScreen(lastScreen);
        }
    }
    
    @SideOnly(Side.CLIENT)
    private class StatList extends GuiSlot {
        public StatList() {
            super(
                GuiCollectedData.this.mc,
                GuiCollectedData.this.width, GuiCollectedData.this.height,
                (PADDING * 2) + fontRenderer.FONT_HEIGHT,
                GuiCollectedData.this.height - FOOTER_HEIGHT,
                fontRenderer.FONT_HEIGHT + 1
            );
        }
    
        @Override
        protected int getSize() {
            return stats.size();
        }
    
        @Override
        protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {}
    
        @Override
        protected boolean isSelected(int slotIndex) {
            return false;
        }
    
        @Override
        protected void drawBackground() {}
    
        @Override
        protected void drawSlot(int slotIndex, int xPos, int yPos, int heightIn, int mouseXIn, int mouseYIn, float partialTicks) {
            fontRenderer.drawString(keys.get(slotIndex), LIST_PADDING, yPos, 0xFFFFFF);
            fontRenderer.drawString(values.get(slotIndex), LIST_COL_X, yPos, 0xFFFFFF);
        }
    
        @Override
        protected int getScrollBarX() {
            return width - LIST_PADDING;
        }
    }
}
