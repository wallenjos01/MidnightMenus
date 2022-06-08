package org.wallentines.midnightmenus.common;

import org.wallentines.midnightcore.api.MidnightCoreAPI;
import org.wallentines.midnightcore.api.module.lang.LangModule;
import org.wallentines.midnightcore.api.module.lang.LangProvider;
import org.wallentines.midnightcore.common.util.FileUtil;
import org.wallentines.midnightlib.config.ConfigRegistry;
import org.wallentines.midnightlib.config.ConfigSection;
import org.wallentines.midnightlib.config.FileConfig;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightmenus.api.MidnightMenusAPI;
import org.wallentines.midnightmenus.api.menu.MenuAction;
import org.wallentines.midnightmenus.api.menu.MenuRequirement;
import org.wallentines.midnightmenus.api.menu.MidnightMenu;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

public class MidnightMenusImpl extends MidnightMenusAPI {

    public static final Registry<MidnightMenu> MENU_REGISTRY = new Registry<>();
    public static final Registry<MenuRequirement> REQUIREMENT_REGISTRY = new Registry<>();

    private final File contentFolder;
    private final FileConfig configFile;

    private final LangProvider langProvider;

    public MidnightMenusImpl(Path configFolder, ConfigSection defaultLang) {

        ConfigRegistry.INSTANCE.registerSerializer(MenuAction.class, MenuAction.SERIALIZER);

        File dataFolder = FileUtil.tryCreateDirectory(configFolder);
        if(dataFolder == null) {
            throw new IllegalStateException("Unable to create data directory " + configFolder);
        }

        contentFolder = configFolder.resolve("content").toFile();
        configFile = FileConfig.findOrCreate("config", configFolder.toFile());

        langProvider = MidnightCoreAPI.getInstance().getModuleManager().getModule(LangModule.class).createProvider(configFolder.resolve("lang"), defaultLang);

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

                    FileConfig conf = FileConfig.fromFile(rf);
                    String path = rf.getName().substring(0, rf.getName().length() - conf.getProvider().getFileExtension().length());

                    REQUIREMENT_REGISTRY.register(new Identifier(namespace, path), MenuRequirement.SERIALIZER.deserialize(conf.getRoot()));

                } catch (Exception ex) {

                    LOGGER.warn("An exception occurred while parsing a requirement!");
                    ex.printStackTrace();
                }
            });

            forEachFile(menusFolder, mf -> {

                try {

                    FileConfig conf = FileConfig.fromFile(mf);
                    String path = mf.getName().substring(0, mf.getName().length() - conf.getProvider().getFileExtension().length());

                    MENU_REGISTRY.register(new Identifier(namespace, path), MidnightMenu.parse(conf.getRoot()));

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
        if(fs == null || fs.length == 0) return;

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
