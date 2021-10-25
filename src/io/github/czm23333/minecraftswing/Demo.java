package io.github.czm23333.minecraftswing;

import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Objects;

public class Demo extends JavaPlugin {
    public static Demo instance;

    @Override
    public void onEnable() {
        instance = this;

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        Objects.requireNonNull(Bukkit.getPluginCommand("test")).setExecutor((commandSender, command, s, strings) -> {
            if (command.getName().equalsIgnoreCase("test") && commandSender instanceof Player) {
                JPanel panel = new JPanel();
                panel.setSize(300, 300);
                panel.setLayout(null);
                JEditorPane editor = new JEditorPane();
                editor.setSize(100, 100);
                editor.setLocation(100, 0);
                JButton button = new JButton("my button");
                button.setSize(100, 100);
                button.setLocation(0, 0);
                button.addMouseListener(new MouseListener() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        commandSender.sendMessage(editor.getText());
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {

                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {

                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        commandSender.sendMessage("entered");
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        commandSender.sendMessage("exited");
                    }
                });
                panel.add(button);
                panel.add(editor);

                Displayer.create(instance,
                                 300,
                                 300,
                                 ((Player) commandSender).getLocation().getBlock().getLocation(),
                                 BlockFace.EAST).setPanel((Player) commandSender, panel);
            }
            return true;
        });
    }

    @Override
    public void onDisable() {

    }
}
