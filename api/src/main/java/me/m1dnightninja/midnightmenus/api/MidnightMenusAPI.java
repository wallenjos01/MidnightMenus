package me.m1dnightninja.midnightmenus.api;

import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigProvider;
import me.m1dnightninja.midnightcore.api.config.ConfigRegistry;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.config.FileConfig;
import me.m1dnightninja.midnightcore.api.module.lang.ILangModule;
import me.m1dnightninja.midnightcore.api.module.lang.ILangProvider;
import me.m1dnightninja.midnightcore.api.registry.MIdentifier;
import me.m1dnightninja.midnightcore.api.registry.MRegistry;
import me.m1dnightninja.midnightmenus.api.menu.MenuAction;
import me.m1dnightninja.midnightmenus.api.menu.MidnightMenu;
import me.m1dnightninja.midnightmenus.api.menu.MenuRequirement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.function.Consumer;

public class MidnightMenusAPI {

    private static final Logger LOGGER = LogManager.getLogger("MidnightMenus");

    public static final MRegistry<MidnightMenu> MENU_REGISTRY = new MRegistry<>();
    public static final MRegistry<MenuRequirement> REQUIREMENT_REGISTRY = new MRegistry<>();

    private final File contentFolder;
    private final FileConfig configFile;

    private final ILangProvider langProvider;

    private static MidnightMenusAPI instance;

    public MidnightMenusAPI(File configFolder, ConfigSection defaultLang) {

        instance = this;

        ConfigProvider prov = ConfigRegistry.INSTANCE.getDefaultProvider();
        MidnightCoreAPI.getInstance().getConfigRegistry().registerSerializer(MenuAction.class, MenuAction.SERIALIZER);

        contentFolder = new File(configFolder, "content");
        configFile = new FileConfig(new File(configFolder, "config" + prov.getFileExtension()), prov);

        langProvider = MidnightCoreAPI.getInstance().getModule(ILangModule.class).createLangProvider(new File(configFolder, "lang"), defaultLang);

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

                    REQUIREMENT_REGISTRY.register(MIdentifier.create(namespace, path), MenuRequirement.SERIALIZER.deserialize(conf.getRoot()));

                } catch (Exception ex) {

                    MidnightMenusAPI.getLogger().warn("An exception occurred while parsing a requirement!");
                    ex.printStackTrace();
                }
            });

            forEachFile(menusFolder, mf -> {

                try {

                    FileConfig conf = FileConfig.fromFile(mf);
                    String path = mf.getName().substring(0, mf.getName().length() - conf.getProvider().getFileExtension().length());

                    MENU_REGISTRY.register(MIdentifier.create(namespace, path), MidnightMenu.parse(conf.getRoot()));

                } catch (Exception ex) {

                    MidnightMenusAPI.getLogger().warn("An exception occurred while parsing a menu!");
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


    public static MidnightMenusAPI getInstance() {
        return instance;
    }

    public static Logger getLogger() { return LOGGER; }

    public ConfigSection getConfig() { return configFile.getRoot(); }

    public ILangProvider getLangProvider() {
        return langProvider;
    }

    public void reload() {

        loadConfig();
    }

}
