package com.blackrikka.ironworks.storage.terminal;

import com.blackrikka.ironworks.net.InsertRequestPayload;
import com.blackrikka.ironworks.net.QueryRequestPayload;
import com.blackrikka.ironworks.net.TakeRequestPayload;
import com.blackrikka.ironworks.storage.query.QueryPage;
import com.blackrikka.ironworks.storage.query.SortMode;
import com.blackrikka.ironworks.storage.terminal.TerminalActions.InsertMode;
import com.blackrikka.ironworks.storage.terminal.TerminalActions.TakeMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * DB terminal GUI, drawn with {@code GuiGraphics.fill} like the other Ironworks screens.
 * <ul>
 *   <li>Search box: the query is sent 0.3 s after the last edit, and at once on Enter.</li>
 *   <li>Sort buttons: a new sort starts ascending; pressing the active sort again toggles the direction.</li>
 *   <li>9 x 5 result grid. Empty cursor: left = one stack, right = half, shift + left = to the inventory,
 *       ctrl + left or middle = one item. Cursor holding an item: left = all of it, right = one item.</li>
 * </ul>
 * The server is the source of truth: after every take or insert the screen asks for the page again.
 */
public class DbTerminalScreen extends AbstractContainerScreen<DbTerminalMenu> {
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 288;

    private static final int SEARCH_X = 8;
    private static final int SEARCH_Y = 18;
    private static final int SEARCH_W = 160;
    private static final int SEARCH_H = 14;

    private static final int SORT_Y = 36;
    private static final int SORT_W = 52;
    private static final int SORT_H = 16;
    private static final int[] SORT_X = {7, 63, 119};

    private static final int STATUS_Y = 56;
    private static final int ERROR_Y = 66;

    private static final int GRID_X = 7;
    private static final int GRID_Y = 78;
    private static final int COLUMNS = 9;
    private static final int ROWS = 5;
    private static final int CELL = 18;

    private static final int PAGE_Y = 172;
    private static final int PAGE_BUTTON_W = 20;
    private static final int PAGE_BUTTON_H = 14;

    private static final long DEBOUNCE_MILLIS = 300;

    private static final int COLOR_BORDER = 0xFF373737;
    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_SLOT_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_BAR_BACK = 0xFF2B2B2B;
    private static final int COLOR_ONLINE = 0xFF1E7A1E;
    private static final int COLOR_OFFLINE = 0xFFA01E1E;
    private static final int COLOR_ERROR = 0xFFB00000;
    private static final int COLOR_DIM = 0xFF606060;
    private static final int COLOR_TEXT = 0xFF404040;

    private EditBox search;
    private Button nameSort;
    private Button countSort;
    private Button idSort;
    private Button previousPage;
    private Button nextPage;

    private SortMode sort = SortMode.NAME;
    private boolean descending;
    private int page;
    private QueryPage result;
    private boolean queryPending;
    private long lastEditMillis;

    public DbTerminalScreen(DbTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = IMAGE_WIDTH;
        this.imageHeight = IMAGE_HEIGHT;
        // The player inventory label sits just above the inventory rows.
        this.inventoryLabelY = IMAGE_HEIGHT - 94;
    }

    @Override
    protected void init() {
        super.init();
        String previousText = search == null ? "" : search.getValue();
        search = addRenderableWidget(new EditBox(this.font, leftPos + SEARCH_X, topPos + SEARCH_Y, SEARCH_W, SEARCH_H,
                Component.translatable("gui.ironworks.terminal.search")));
        search.setMaxLength(TerminalActions.MAX_QUERY_LENGTH);
        search.setHint(Component.translatable("gui.ironworks.terminal.search_hint"));
        search.setValue(previousText);
        search.setResponder(value -> onSearchEdited());
        setInitialFocus(search);

        nameSort = addRenderableWidget(Button.builder(Component.empty(), b -> onSort(SortMode.NAME))
                .bounds(leftPos + SORT_X[0], topPos + SORT_Y, SORT_W, SORT_H).build());
        countSort = addRenderableWidget(Button.builder(Component.empty(), b -> onSort(SortMode.COUNT))
                .bounds(leftPos + SORT_X[1], topPos + SORT_Y, SORT_W, SORT_H).build());
        idSort = addRenderableWidget(Button.builder(Component.empty(), b -> onSort(SortMode.ID))
                .bounds(leftPos + SORT_X[2], topPos + SORT_Y, SORT_W, SORT_H).build());

        previousPage = addRenderableWidget(Button.builder(Component.literal("<"), b -> changePage(page - 1))
                .bounds(leftPos + GRID_X, topPos + PAGE_Y, PAGE_BUTTON_W, PAGE_BUTTON_H).build());
        nextPage = addRenderableWidget(Button.builder(Component.literal(">"), b -> changePage(page + 1))
                .bounds(leftPos + IMAGE_WIDTH - GRID_X - PAGE_BUTTON_W, topPos + PAGE_Y, PAGE_BUTTON_W, PAGE_BUTTON_H)
                .build());

        refreshControls();
        sendQuery();
    }

    /** Called by the client packet handler with the server's page. */
    public void acceptResult(QueryPage page) {
        this.result = page;
        this.page = page.page();
        refreshControls();
    }

    /** Runs once per client tick (AbstractContainerScreen.tick is final and calls this). */
    @Override
    protected void containerTick() {
        super.containerTick();
        if (queryPending && System.currentTimeMillis() - lastEditMillis >= DEBOUNCE_MILLIS) {
            sendQuery();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // While the search box has focus, every key goes to it, so the inventory key does not close the screen.
        if (search != null && search.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                search.setFocused(false);
                setFocused(null);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                sendQuery();
                return true;
            }
            search.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isInsideGrid(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        setFocused(null);
        if (search != null) {
            search.setFocused(false);
        }
        ItemStack cursor = menu.getCarried();
        if (!cursor.isEmpty()) {
            if (button == 0) {
                insert(InsertMode.ALL);
            } else if (button == 1) {
                insert(InsertMode.ONE);
            }
            return true;
        }
        int index = entryIndexAt(mouseX, mouseY);
        TakeMode mode = takeModeFor(button);
        if (index >= 0 && mode != null) {
            take(result.entries().get(index).template(), mode);
        }
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawEntries(graphics);
        int index = entryIndexAt(mouseX, mouseY);
        if (index >= 0) {
            QueryPage.Entry entry = result.entries().get(index);
            List<Component> lines = List.of(entry.template().getHoverName(),
                    Component.translatable("gui.ironworks.terminal.stored", formatExact(entry.count())));
            graphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Panel with a 1px border.
        graphics.fill(x, y, x + imageWidth, y + imageHeight, COLOR_BORDER);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, COLOR_PANEL);

        // Dark well behind the result grid.
        graphics.fill(x + GRID_X - 1, y + GRID_Y - 1, x + GRID_X + COLUMNS * CELL + 1, y + GRID_Y + ROWS * CELL + 1,
                COLOR_BAR_BACK);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                drawSlot(graphics, x + GRID_X + col * CELL, y + GRID_Y + row * CELL);
            }
        }

        // Player inventory and hotbar (the menu slot origin is one pixel inside the background).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, x + 7 + col * 18, y + DbTerminalMenu.PLAYER_INVENTORY_Y - 1 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(graphics, x + 7 + col * 18, y + DbTerminalMenu.HOTBAR_Y - 1);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        drawStatus(graphics);
        drawPageLabel(graphics);
    }

    private void drawStatus(GuiGraphics graphics) {
        if (result == null) {
            graphics.drawString(this.font, Component.translatable("gui.ironworks.terminal.loading").getString(),
                    8, STATUS_Y, COLOR_DIM, false);
            return;
        }
        if (result.rackCount() == 0) {
            graphics.drawString(this.font, Component.translatable("gui.ironworks.terminal.no_network").getString(),
                    8, STATUS_Y, COLOR_OFFLINE, false);
        } else {
            String state = Component.translatable(result.online()
                    ? "gui.ironworks.terminal.online" : "gui.ironworks.terminal.offline").getString();
            String status = Component.translatable("gui.ironworks.terminal.status", state,
                    formatExact(result.usedItems()), result.rackCount()).getString();
            graphics.drawString(this.font, status, 8, STATUS_Y,
                    result.online() ? COLOR_ONLINE : COLOR_OFFLINE, false);
        }

        if (!result.errors().isEmpty()) {
            String error = Component.translatable("gui.ironworks.terminal.error", result.errors().get(0)).getString();
            graphics.drawString(this.font, error, 8, ERROR_Y, COLOR_ERROR, false);
        } else if (result.online() && result.entries().isEmpty()) {
            String empty = Component.translatable(result.usedItems() == 0
                    ? "gui.ironworks.terminal.empty" : "gui.ironworks.terminal.no_match").getString();
            graphics.drawString(this.font, empty, GRID_X + 4, GRID_Y + 6, COLOR_DIM, false);
        }
    }

    private void drawPageLabel(GuiGraphics graphics) {
        String text = result == null ? "-" : (result.page() + 1) + " / " + result.pageCount();
        int x = IMAGE_WIDTH / 2 - this.font.width(text) / 2;
        graphics.drawString(this.font, text, x, PAGE_Y + 3, COLOR_TEXT, false);
    }

    private void drawEntries(GuiGraphics graphics) {
        if (result == null) {
            return;
        }
        List<QueryPage.Entry> entries = result.entries();
        for (int i = 0; i < entries.size(); i++) {
            QueryPage.Entry entry = entries.get(i);
            int x = leftPos + GRID_X + (i % COLUMNS) * CELL + 1;
            int y = topPos + GRID_Y + (i / COLUMNS) * CELL + 1;
            graphics.renderItem(entry.template(), x, y);
            graphics.renderItemDecorations(this.font, entry.template(), x, y, abbreviate(entry.count()));
        }
    }

    private void onSearchEdited() {
        page = 0;
        queryPending = true;
        lastEditMillis = System.currentTimeMillis();
    }

    private void onSort(SortMode mode) {
        if (sort == mode) {
            descending = !descending;
        } else {
            sort = mode;
            descending = false;
        }
        page = 0;
        sendQuery();
    }

    private void changePage(int newPage) {
        page = Math.max(0, newPage);
        sendQuery();
    }

    private void sendQuery() {
        queryPending = false;
        refreshControls();
        PacketDistributor.sendToServer(new QueryRequestPayload(search.getValue(), sort, descending, page));
    }

    private void take(ItemStack template, TakeMode mode) {
        PacketDistributor.sendToServer(new TakeRequestPayload(template, mode));
        sendQuery();
    }

    private void insert(InsertMode mode) {
        PacketDistributor.sendToServer(new InsertRequestPayload(mode));
        sendQuery();
    }

    private void refreshControls() {
        if (nameSort == null) {
            return;
        }
        nameSort.setMessage(sortLabel(SortMode.NAME, "gui.ironworks.terminal.sort.name"));
        countSort.setMessage(sortLabel(SortMode.COUNT, "gui.ironworks.terminal.sort.count"));
        idSort.setMessage(sortLabel(SortMode.ID, "gui.ironworks.terminal.sort.id"));
        previousPage.active = page > 0;
        nextPage.active = result != null && page + 1 < result.pageCount();
    }

    private Component sortLabel(SortMode mode, String key) {
        String marker = "";
        if (sort == mode) {
            marker = descending ? " v" : " ^";
        }
        return Component.translatable(key).append(Component.literal(marker));
    }

    private boolean isInsideGrid(double mouseX, double mouseY) {
        int left = leftPos + GRID_X;
        int top = topPos + GRID_Y;
        return mouseX >= left && mouseX < left + COLUMNS * CELL && mouseY >= top && mouseY < top + ROWS * CELL;
    }

    /** Index of the result entry under the mouse, or -1 if there is none. */
    private int entryIndexAt(double mouseX, double mouseY) {
        if (result == null || !isInsideGrid(mouseX, mouseY)) {
            return -1;
        }
        int col = (int) ((mouseX - (leftPos + GRID_X)) / CELL);
        int row = (int) ((mouseY - (topPos + GRID_Y)) / CELL);
        int index = row * COLUMNS + col;
        return index < result.entries().size() ? index : -1;
    }

    private static TakeMode takeModeFor(int button) {
        return switch (button) {
            case 0 -> hasShiftDown() ? TakeMode.SHIFT_TO_INVENTORY : hasControlDown() ? TakeMode.ONE : TakeMode.STACK;
            case 1 -> TakeMode.HALF;
            case 2 -> TakeMode.ONE;
            default -> null;
        };
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + CELL, y + CELL, COLOR_SLOT_LIGHT);
        graphics.fill(x + 1, y + 1, x + CELL, y + CELL, COLOR_SLOT);
    }

    /** Stored amount for the grid: exact below 1000, otherwise one decimal with K, M, G or T (1.2K, 3.4M). */
    static String abbreviate(long count) {
        if (count < 1000) {
            return Long.toString(count);
        }
        String[] units = {"K", "M", "G", "T"};
        int unit = 0;
        long divisor = 1;
        while (count / divisor >= 1000 && unit < units.length - 1) {
            divisor *= 1000;
            unit++;
        }
        long tenths = Math.round(count * 10.0 / divisor);
        if (tenths >= 10_000 && unit < units.length - 1) {
            // Rounding reached 1000 of this unit, so use the next unit (999.96K -> 1M, not 1000K).
            divisor *= 1000;
            unit++;
            tenths = Math.round(count * 10.0 / divisor);
        }
        String whole = Long.toString(tenths / 10);
        return tenths % 10 == 0 ? whole + units[unit] : whole + "." + (tenths % 10) + units[unit];
    }

    private static String formatExact(long count) {
        return String.format(Locale.ROOT, "%,d", count);
    }
}
