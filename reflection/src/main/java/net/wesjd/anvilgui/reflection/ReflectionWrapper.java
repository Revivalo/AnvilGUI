package net.wesjd.anvilgui.reflection;


import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutCloseWindow;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerAnvil;
import net.minecraft.world.inventory.ContainerPlayer;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.level.World;
import net.wesjd.anvilgui.version.VersionWrapper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public final class ReflectionWrapper implements VersionWrapper {
	private final Field fieldContainer;
	private final Field fieldPlayerContainer;

    private final Class<?> classCraftWorld;
    private final Method methodCraftWorldGetHandle;

    private final Class<?> classCraftPlayer;
    private final Method methodCraftPlayerGetHandle;

    private final Class<?> classCraftEventFactory;
    private final Method methodHandleInventoryCloseEvent;

    private final Field fieldEntityPlayerGetConnection;


	public ReflectionWrapper(String serverVersion) throws ClassNotFoundException, NoSuchMethodException {
		this.fieldContainer = Arrays.asList(EntityHuman.class.getDeclaredFields())
                .stream()
                .filter(field -> field.getType() == Container.class)
                .findFirst()
                .get();
		this.fieldPlayerContainer = Arrays.asList(EntityHuman.class.getDeclaredFields())
                .stream()
                .filter(field -> field.getType() == ContainerPlayer.class)
                .findFirst()
                .get();

        serverVersion = "v" + serverVersion;

        this.classCraftWorld = Class.forName("org.bukkit.craftbukkit." + serverVersion + ".CraftWorld");
        this.methodCraftWorldGetHandle = this.classCraftWorld.getDeclaredMethod("getHandle");

        this.classCraftPlayer = Class.forName("org.bukkit.craftbukkit." + serverVersion + ".entity.CraftPlayer");
        this.methodCraftPlayerGetHandle = this.classCraftPlayer.getDeclaredMethod("getHandle");

        this.classCraftEventFactory = Class.forName("org.bukkit.craftbukkit." + serverVersion + ".event.CraftEventFactory");
        this.methodHandleInventoryCloseEvent = this.classCraftEventFactory.getDeclaredMethod("handleInventoryCloseEvent", EntityHuman.class);

        this.fieldEntityPlayerGetConnection = Arrays.stream(EntityPlayer.class.getDeclaredFields())
                .filter(field -> field.getType() == PlayerConnection.class).findFirst().get();

		
	}
	
    private int getRealNextContainerId(Player player) {
        return toNMS(player).nextContainerCounter();
    }

    /**
     * Turns a {@link Player} into an NMS one
     *
     * @param player The player to be converted
     * @return the NMS EntityPlayer
     */
    private EntityPlayer toNMS(Player player) {
        try {
            return (EntityPlayer) this.methodCraftPlayerGetHandle.invoke(classCraftPlayer.cast(player));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getNextContainerId(Player player, Object container) {
        return ((AnvilContainer) container).getContainerId();
    }

    @Override
    public void handleInventoryCloseEvent(Player player) {
        try {
            methodHandleInventoryCloseEvent.invoke(null, toNMS(player));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendPacketOpenWindow(Player player, int containerId, String inventoryTitle) {
        try {
            PlayerConnection playerConnection = (PlayerConnection) fieldEntityPlayerGetConnection.get(toNMS(player));
            playerConnection.a(new PacketPlayOutOpenWindow(containerId, Containers.h, IChatBaseComponent.a(inventoryTitle)));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendPacketCloseWindow(Player player, int containerId) {
        try {
            PlayerConnection playerConnection = (PlayerConnection) fieldEntityPlayerGetConnection.get(toNMS(player));
            playerConnection.a(new PacketPlayOutCloseWindow(containerId));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setActiveContainerDefault(Player player) {
        //toNMS(player).bU = toNMS(player).bT;
        
        EntityPlayer entityPlayer = toNMS(player);
        
        try {
            ContainerPlayer containerPlayer = (ContainerPlayer) fieldPlayerContainer.get(entityPlayer);
            fieldContainer.set(entityPlayer, containerPlayer);	
        } catch(Exception e) {
        	e.printStackTrace();
        }
    }

    @Override
    public void setActiveContainer(Player player, Object container) {
        //toNMS(player).bU = (Container) container;

    	EntityPlayer entityPlayer = toNMS(player);
        try {
            fieldContainer.set(entityPlayer, (Container) container);	
        } catch(Exception e) {
        	e.printStackTrace();
        }
    }

    @Override
    public void setActiveContainerId(Object container, int containerId) {}

    @Override
    public void addActiveContainerSlotListener(Object container, Player player) {
        toNMS(player).a((Container) container);
    }

    @Override
    public Inventory toBukkitInventory(Object container) {
        return ((Container) container).getBukkitView().getTopInventory();
    }

    @Override
    public Object newContainerAnvil(Player player, String title) {
        try {
            return new AnvilContainer(player, getRealNextContainerId(player), title, this);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static class AnvilContainer extends ContainerAnvil {


    	public static PlayerInventory getPlayerInventory(Player player, ReflectionWrapper wrapper) {

    		Method getInventory = Arrays.asList(EntityHuman.class.getDeclaredMethods())
                    .stream()
                    .filter(method -> method.getReturnType() == PlayerInventory.class)
                    .findFirst()
                    .get();

			try {
                EntityPlayer entityPlayer = (EntityPlayer) wrapper.methodCraftPlayerGetHandle.invoke(wrapper.classCraftPlayer.cast(player));
				return (PlayerInventory) getInventory.invoke(entityPlayer);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            return null;
    	}
        public AnvilContainer(Player player, int containerId, String guiTitle, ReflectionWrapper reflectionWrapper) throws InvocationTargetException, IllegalAccessException {
            super(
                    containerId,
                    //((CraftPlayer) player).getHandle().fB(),
                    AnvilContainer.getPlayerInventory(player, reflectionWrapper),
                    ContainerAccess.a((World) reflectionWrapper.methodCraftWorldGetHandle
                            .invoke(reflectionWrapper.classCraftWorld.cast(player.getWorld())), new BlockPosition(0, 0, 0)));

            this.checkReachable = false;
            setTitle(IChatBaseComponent.a(guiTitle));
        }

//        @Override
//        public void l() {
//            super.l();
//            this.w.a(0);
//        }

        @Override
        public void b(EntityHuman player) {}

        @Override
        protected void a(EntityHuman player, IInventory container) {}

        public int getContainerId() {
            return this.j;
        }
    }
}
