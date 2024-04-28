package net.wesjd.anvilgui.version;

import org.bukkit.Bukkit;

/**
 * Matches the server's NMS version to its {@link VersionWrapper}
 *
 * @author Wesley Smith
 * @since 1.2.1
 */
public class VersionMatcher {

    /**
     * Matches the server version to it's {@link VersionWrapper}
     *
     * @return The {@link VersionWrapper} for this server
     * @throws IllegalStateException If the version wrapper failed to be instantiated or is unable to be found
     */
    public VersionWrapper match() {
        final String packageName = Bukkit.getServer()
                .getClass()
                .getPackage()
                .getName();

        String wrapperName = "Paper";
        if (packageName.contains(".v")) {
            wrapperName = packageName
                    .split("\\.")[3]
                    .substring(1);
        }

        try {
            return (VersionWrapper) Class.forName(getClass().getPackage().getName() + ".Wrapper" + wrapperName)
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    "AnvilGUI does not support server version \"" + Bukkit.getServer().getBukkitVersion() + "\"", exception);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Failed to instantiate version wrapper for version " + Bukkit.getServer().getBukkitVersion(), exception);
        }
    }
}
