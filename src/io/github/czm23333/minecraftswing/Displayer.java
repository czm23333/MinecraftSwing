package io.github.czm23333.minecraftswing;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import sun.swing.JLightweightFrame;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Displayer implements Listener {
    private static final int MAP_SIZE = 128;
    private static final EventQueue eventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
    private final Plugin plugin;
    private final int width, height;
    private final int imgWidth, imgHeight;
    private final Map<UUID, Screen> panels = new ConcurrentHashMap<>();
    private final HashSet<Location> background = new HashSet<>();
    private final HashSet<UUID> frames = new HashSet<>();
    private final JLightweightFrame root = new JLightweightFrame();
    private AxisAlign align;
    private BukkitTask updateTask;

    private Location leftTop, rightBottom;

    private Displayer(Plugin plugin, int width, int height, Location loc, BlockFace face) {
        this.plugin = plugin;

        this.width = width;
        this.height = height;
        int imgWidth1 = width;
        int imgHeight1 = height;
        if (imgWidth1 % MAP_SIZE != 0) {
            imgWidth1 -= imgWidth1 % MAP_SIZE;
            imgWidth1 += MAP_SIZE;
        }
        this.imgWidth = imgWidth1;
        if (imgHeight1 % MAP_SIZE != 0) {
            imgHeight1 -= imgHeight1 % MAP_SIZE;
            imgHeight1 += MAP_SIZE;
        }
        this.imgHeight = imgHeight1;

        root.setLocation(0, 0);
        root.setLayout(null);
        root.setVisible(true);

        prepare(loc, face);
    }

    public static Displayer create(Plugin plugin, int width, int height, Location loc, BlockFace face) {
        return new Displayer(plugin, width, height, loc, face);
    }

    private void prepare(Location loc, BlockFace face) {
        Location base = loc.clone();
        int wc = width / MAP_SIZE + (width % MAP_SIZE == 0 ? 0 : 1);
        int hc = height / MAP_SIZE + (height % MAP_SIZE == 0 ? 0 : 1);
        Vector wVec;
        Vector hVec;
        if (face.getModX() != 0) {
            if (face.getModX() > 0) {
                loc.add(1, 0, 0);
                wVec = new Vector(0, 0, -1);
                align = new AxisAlign(AxisAlign.Axis.X, loc.getX());
            } else {
                align = new AxisAlign(AxisAlign.Axis.X, loc.getX());
                loc.add(-1, 0, 0);
                wVec = new Vector(0, 0, 1);
            }
            hVec = new Vector(0, -1, 0);
        } else if (face.getModZ() != 0) {
            if (face.getModZ() > 0) {
                loc.add(0, 0, 1);
                wVec = new Vector(1, 0, 0);
                align = new AxisAlign(AxisAlign.Axis.Z, loc.getZ());
            } else {
                align = new AxisAlign(AxisAlign.Axis.Z, loc.getZ());
                loc.add(0, 0, -1);
                wVec = new Vector(-1, 0, 0);
            }
            hVec = new Vector(0, -1, 0);
        } else {
            throw new IllegalArgumentException("Invalid block face");
        }

        for (int i = 0; i < wc; ++i) {
            for (int j = 0; j < hc; ++j) {
                Location back = base.clone().add(wVec.clone().multiply(i)).add(hVec.clone().multiply(j));
                back.getBlock().setType(Material.BARRIER);
                background.add(back);
                int finalI = i;
                int finalJ = j;
                ItemFrame frame = (ItemFrame) Objects.requireNonNull(loc.getWorld()).spawnEntity(loc.clone()
                                                                                                         .add(wVec.clone()
                                                                                                                      .multiply(
                                                                                                                              finalI))
                                                                                                         .add(hVec.clone()
                                                                                                                      .multiply(
                                                                                                                              finalJ)),
                                                                                                 EntityType.ITEM_FRAME);
                MapView view = Bukkit.createMap(loc.getWorld());
                view.getRenderers().forEach(view::removeRenderer);
                view.addRenderer(new MapRenderer(true) {
                    @Override
                    public void render(@Nonnull MapView mapView, @Nonnull MapCanvas mapCanvas, @Nonnull Player player) {
                        if (panels.containsKey(player.getUniqueId())) {
                            mapCanvas.drawImage(0,
                                                0,
                                                panels.get(player.getUniqueId()).image.getSubimage(finalI * MAP_SIZE,
                                                                                                   finalJ * MAP_SIZE,
                                                                                                   MAP_SIZE,
                                                                                                   MAP_SIZE));
                        }
                    }
                });
                ItemStack mapItem = new ItemStack(Material.MAP);
                MapMeta mapMeta = (MapMeta) Objects.requireNonNull(mapItem.hasItemMeta() ?
                                                                   mapItem.getItemMeta() :
                                                                   Bukkit.getItemFactory().getItemMeta(Material.MAP));
                mapMeta.setMapView(view);
                mapItem.setItemMeta(mapMeta);
                frame.setItem(mapItem);

                frames.add(frame.getUniqueId());
            }
        }

        if (face.getModX() > 0) {
            loc.add(0, 0, 1);
        } else if (face.getModX() < 0) {
            loc.add(1, 0, 0);
        } else if (face.getModZ() < 0) {
            loc.add(1, 0, 1);
        }
        loc.add(0, 1, 0);
        leftTop = loc.clone();
        rightBottom = loc.clone().add(wVec.clone().multiply(wc)).add(hVec.clone().multiply(hc));

        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                SwingUtilities.invokeAndWait(() -> panels.values().forEach(screen -> {
                    Graphics2D graphics2D = screen.image.createGraphics();
                    screen.panel.update(graphics2D);
                    graphics2D.dispose();
                }));
            } catch (InterruptedException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }, 0, 0);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private BufferedImage createImage() {
        return new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_BYTE_INDEXED);
    }

    public void destroy() {
        updateTask.cancel();
        HandlerList.unregisterAll(this);

        background.stream().map(Location::getBlock).forEach(block -> block.setType(Material.AIR));
        frames.stream().map(Bukkit::getEntity).filter(Objects::nonNull).forEach(Entity::remove);
    }

    public void setPanel(Player player, JPanel panel) {
        if (panel.getWidth() != width || panel.getHeight() != height)
            throw new IllegalArgumentException("Illegal panel size");

        root.add(panel);
        if (panels.containsKey(player.getUniqueId())) {
            Screen screen = panels.get(player.getUniqueId());
            if (screen.panel != null)
                root.remove(screen.panel);
            screen.panel = panel;
        } else
            panels.put(player.getUniqueId(), new Screen(panel));
    }

    private Pair<Integer, Integer> calHit(Location eyeLoc) {
        Location hit = align.align(eyeLoc);
        if (hit == null)
            return null;
        long hitWidth, hitHeight;
        switch (align.axis) {
            case X:
                hitWidth = Math.round((hit.getZ() - leftTop.getZ()) / (rightBottom.getZ() - leftTop.getZ()) * imgWidth);
                hitHeight = Math.round((hit.getY() - leftTop.getY()) / (rightBottom.getY() - leftTop.getY()) *
                                       imgHeight);
                break;
            case Z:
                hitWidth = Math.round((hit.getX() - leftTop.getX()) / (rightBottom.getX() - leftTop.getX()) * imgWidth);
                hitHeight = Math.round((hit.getY() - leftTop.getY()) / (rightBottom.getY() - leftTop.getY()) *
                                       imgHeight);
                break;
            default:
                throw new IllegalStateException();
        }

        return hitWidth >= 0 && hitWidth < width && hitHeight >= 0 && hitHeight < height ? Pair.of(Math.toIntExact(
                hitWidth), Math.toIntExact(hitHeight)) : null;
    }

    private Component passMouse(Component component, int relativeX, int relativeY, MouseEvent template) {
        eventQueue.postEvent(new MouseEvent(component,
                                            template.getID(),
                                            template.getWhen(),
                                            template.getModifiers(),
                                            relativeX,
                                            relativeY,
                                            template.getXOnScreen(),
                                            template.getYOnScreen(),
                                            template.getClickCount(),
                                            template.isPopupTrigger(),
                                            template.getButton()));

        Component another = component.getComponentAt(relativeX, relativeY);
        if (another == component)
            return another;
        Point loc = another.getLocation();
        int newX = relativeX - loc.x;
        int newY = relativeY - loc.y;
        return passMouse(another, newX, newY, template);
    }

    public boolean click(Player player, boolean left) {
        if (!panels.containsKey(player.getUniqueId()))
            return false;

        Pair<Integer, Integer> hit = calHit(player.getEyeLocation());
        if (hit == null)
            return false;

        Screen screen = panels.get(player.getUniqueId());
        SwingUtilities.invokeLater(() -> {
            passMouse(screen.panel,
                      hit.first,
                      hit.second,
                      new MouseEvent(screen.panel,
                                     MouseEvent.MOUSE_PRESSED,
                                     System.currentTimeMillis(),
                                     left ? InputEvent.BUTTON1_DOWN_MASK : InputEvent.BUTTON2_DOWN_MASK,
                                     hit.first,
                                     hit.second,
                                     hit.first,
                                     hit.second,
                                     1,
                                     false,
                                     left ? MouseEvent.BUTTON1 : MouseEvent.BUTTON2));
            passMouse(screen.panel,
                      hit.first,
                      hit.second,
                      new MouseEvent(screen.panel,
                                     MouseEvent.MOUSE_RELEASED,
                                     System.currentTimeMillis(),
                                     left ? InputEvent.BUTTON1_DOWN_MASK : InputEvent.BUTTON2_DOWN_MASK,
                                     hit.first,
                                     hit.second,
                                     hit.first,
                                     hit.second,
                                     1,
                                     false,
                                     left ? MouseEvent.BUTTON1 : MouseEvent.BUTTON2));
            Component component = passMouse(screen.panel,
                                            hit.first,
                                            hit.second,
                                            new MouseEvent(screen.panel,
                                                           MouseEvent.MOUSE_CLICKED,
                                                           System.currentTimeMillis(),
                                                           left ?
                                                           InputEvent.BUTTON1_DOWN_MASK :
                                                           InputEvent.BUTTON2_DOWN_MASK,
                                                           hit.first,
                                                           hit.second,
                                                           hit.first,
                                                           hit.second,
                                                           1,
                                                           false,
                                                           left ? MouseEvent.BUTTON1 : MouseEvent.BUTTON2));
            if (component.isFocusable() && component != screen.keyFocus) {
                if (screen.keyFocus != null)
                    eventQueue.postEvent(new FocusEvent(screen.keyFocus, FocusEvent.FOCUS_LOST, false, component));
                eventQueue.postEvent(new FocusEvent(component, FocusEvent.FOCUS_GAINED, false, screen.keyFocus));
                screen.keyFocus = component;
            }
        });
        return true;
    }

    public void move(Player player, Location to) {
        if (!panels.containsKey(player.getUniqueId()))
            return;

        Screen screen = panels.get(player.getUniqueId());

        Pair<Integer, Integer> hit = calHit(to.clone().add(0, player.getEyeHeight(), 0));
        if (hit == null) {
            if (screen.mouseOn != null) {
                eventQueue.postEvent(new MouseEvent(screen.mouseOn,
                                                    MouseEvent.MOUSE_EXITED,
                                                    System.currentTimeMillis(),
                                                    0,
                                                    -1,
                                                    -1,
                                                    0,
                                                    false));
                screen.mouseOn = null;
            }
            return;
        }

        SwingUtilities.invokeLater(() -> {
            Component component = screen.panel.getComponentAt(hit.first, hit.second);
            if (component != screen.mouseOn) {
                if (screen.mouseOn != null)
                    eventQueue.postEvent(new MouseEvent(screen.mouseOn,
                                                        MouseEvent.MOUSE_EXITED,
                                                        System.currentTimeMillis(),
                                                        0,
                                                        hit.first,
                                                        hit.second,
                                                        0,
                                                        false));
                eventQueue.postEvent(new MouseEvent(component,
                                                    MouseEvent.MOUSE_ENTERED,
                                                    System.currentTimeMillis(),
                                                    0,
                                                    hit.first,
                                                    hit.second,
                                                    0,
                                                    false));
                screen.mouseOn = component;
            }
            passMouse(screen.panel,
                      hit.first,
                      hit.second,
                      new MouseEvent(screen.panel,
                                     MouseEvent.MOUSE_MOVED,
                                     System.currentTimeMillis(),
                                     0,
                                     hit.first,
                                     hit.second,
                                     0,
                                     false));
        });
    }

    public boolean input(Player player, String text) {
        if (!panels.containsKey(player.getUniqueId()))
            return false;

        Screen screen = panels.get(player.getUniqueId());

        if (screen.mouseOn == null || screen.keyFocus == null)
            return false;

        eventQueue.postEvent(new FocusEvent(screen.keyFocus, FocusEvent.FOCUS_GAINED, true));
        for (char ch : text.toCharArray()) {
            eventQueue.postEvent(new KeyEvent(screen.panel,
                                              KeyEvent.KEY_PRESSED,
                                              System.currentTimeMillis(),
                                              0,
                                              KeyEvent.getExtendedKeyCodeForChar(ch),
                                              ch,
                                              KeyEvent.KEY_LOCATION_STANDARD));
            eventQueue.postEvent(new KeyEvent(screen.panel,
                                              KeyEvent.KEY_TYPED,
                                              System.currentTimeMillis(),
                                              0,
                                              KeyEvent.VK_UNDEFINED,
                                              ch,
                                              KeyEvent.KEY_LOCATION_UNKNOWN));
            eventQueue.postEvent(new KeyEvent(screen.panel,
                                              KeyEvent.KEY_RELEASED,
                                              System.currentTimeMillis(),
                                              0,
                                              KeyEvent.getExtendedKeyCodeForChar(ch),
                                              ch,
                                              KeyEvent.KEY_LOCATION_STANDARD));
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        move(event.getPlayer(), event.getTo());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        switch (event.getAction()) {
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
                move(event.getPlayer(), event.getPlayer().getLocation());
                event.setCancelled(click(event.getPlayer(), true));
                break;
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                move(event.getPlayer(), event.getPlayer().getLocation());
                event.setCancelled(click(event.getPlayer(), false));
                break;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ItemFrame) {
            move(event.getPlayer(), event.getPlayer().getLocation());
            if (click(event.getPlayer(), false)) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(frames.contains(event.getRightClicked().getUniqueId()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        if (event.getEntity() instanceof ItemFrame)
            event.setCancelled(frames.contains(event.getEntity().getUniqueId()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.ITEM_FRAME &&
            event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK)
            event.setCancelled(frames.contains(event.getEntity().getUniqueId()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntityType() == EntityType.ITEM_FRAME &&
            frames.contains(event.getEntity().getUniqueId()) &&
            event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            event.setCancelled(true);
            if (event.getDamager() instanceof Player) {
                Player p = (Player) event.getDamager();
                move(p, p.getLocation());
                click(p, true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(background.contains(event.getBlock().getLocation()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        event.setCancelled(input(event.getPlayer(), event.getMessage()));
    }

    class Screen {
        JPanel panel;
        BufferedImage image = createImage();
        Component mouseOn = null;
        Component keyFocus = null;

        Screen(JPanel panel) {
            this.panel = panel;
        }
    }
}
