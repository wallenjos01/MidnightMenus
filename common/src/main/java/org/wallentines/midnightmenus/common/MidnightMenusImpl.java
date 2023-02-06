package org.wallentines.midnightmenus.common;

import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.midnightcore.api.text.LangProvider;
import org.wallentines.midnightcore.common.util.FileUtil;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.midnightcore.api.FileConfig;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightmenus.api.MidnightMenusAPI;
import org.wallentines.midnightmenus.api.menu.MenuRequirement;
import org.wallentines.midnightmenus.api.menu.MidnightMenu;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

public class MidnightMenusImpl extends MidnightMenusAPI {

    public static final Registry<MidnightMenu> MENU_REGISTRY = new Registry<>("midnightmenus");
    public static final Registry<MenuRequirement> REQUIREMENT_REGISTRY = new Registry<>("midnightmenus");

    private final File contentFolder;
    private final FileConfig configFile;

    private final LangProvider langProvider;

    public MidnightMenusImpl(Path configFolder, ConfigSection defaultLang) {

        //ConfigRegistry.INSTANCE.registerSerializer(MenuAction.class, MenuAction.SERIALIZER);

        File dataFolder = FileUtil.tryCreateDirectory(configFolder);
        if(dataFolder == null) {
            throw new IllegalStateException("Unable to create data directory " + configFolder);
        }

        contentFolder = configFolder.resolve("content").toFile();
        configFile = FileConfig.findOrCreate("config", configFolder.toFile());

        FileUtil.tryCreateDirectory(configFolder.resolve("lang"));
        langProvider = new LangProvider(configFolder.resolve("lang"), defaultLang);

        loadConfig();
    }


    private void loadConfig() {

        if(!contentFolder.exists() && !contentFolder.mkdirs()) {

            throw new IllegalStateException("Unable to create content folder for MidnightMenus!");
        }

        REQUIREMENT_REGISTRY.clear();
        MENU_REGISTRY.clear();

        forEachFile(contentFolder, f -> {

            if (!f.isDirectory()) return;

            String namespace = f.getName();

            File menusFolder = new File(f, "menus");
            File requirementsFolder = new File(f, "requirements");

            forEachFile(requirementsFolder, rf -> {

                try {

                    FileWrapper<ConfigObject> conf = FileConfig.REGISTRY.fromFile(ConfigContext.INSTANCE, rf);
                    String path = rf.getName().substring(0, rf.getName().lastIndexOf("."));

                    REQUIREMENT_REGISTRY.register(new Identifier(namespace, path), MenuRequirement.SERIALIZER.deserialize(ConfigContext.INSTANCE, conf.getRoot().asSection()).getOrThrow());

                } catch (Exception ex) {

                    LOGGER.warn("An exception occurred while parsing a requirement!");
                    ex.printStackTrace();
                }
            });

            forEachFile(menusFolder, mf -> {

                try {

                    FileWrapper<ConfigObject> conf = FileConfig.REGISTRY.fromFile(ConfigContext.INSTANCE, mf);
                    String path = mf.getName().substring(0, mf.getName().lastIndexOf("."));

                    MENU_REGISTRY.register(new Identifier(namespace, path), MidnightMenu.SERIALIZER.deserialize(ConfigContext.INSTANCE, conf.getRoot().asSection()).getOrThrow());

                } catch (Exception ex) {

                    MidnightMenusAPI.getLogger().warn("An exception occurred while parsing a menu in " + mf.getName() + "!");
                    ex.printStackTrace();
                }
            });
        });

        LOGGER.info("Registered " + REQUIREMENT_REGISTRY.getSize() + " requirements.");
        LOGGER.info("Registered " + MENU_REGISTRY.getSize() + " menus.");
    }


    private static void forEachFile(File folder, Consumer<File> func) {

        if(folder == null || !folder.exists() || !folder.isDirectory()) return;

        File[] fs = folder.listFiles();
        if(fs == null) return;

        for(File f : fs) {
            func.accept(f);
        }
    }

    public ConfigSection getConfig() { return configFile.getRoot(); }

    public LangProvider getLangProvider() {
        return langProvider;
    }

    public void reload() {

        loadConfig();
    }

    @Override
    public Registry<MidnightMenu> getMenuRegistry() {
        return MENU_REGISTRY;
    }

    @Override
    public Registry<MenuRequirement> getRequirementRegistry() {
        return REQUIREMENT_REGISTRY;
    }

}
