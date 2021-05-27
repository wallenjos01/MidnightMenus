package me.m1dnightninja.midnightmenus.api;

import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigProvider;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.config.FileConfig;
import me.m1dnightninja.midnightcore.api.module.lang.ILangModule;
import me.m1dnightninja.midnightcore.api.module.lang.ILangProvider;
import me.m1dnightninja.midnightcore.api.registry.MIdentifier;
import me.m1dnightninja.midnightcore.api.registry.MRegistry;
import me.m1dnightninja.midnightmenus.api.menu.MidnightMenu;
import me.m1dnightninja.midnightmenus.api.menu.MenuRequirement;

import java.io.File;
import java.util.function.Consumer;

public class MidnightMenusAPI {

    public static final MRegistry<MidnightMenu> MENU_REGISTRY = new MRegistry<>();
    public static final MRegistry<MenuRequirement> REQUIREMENT_REGISTRY = new MRegistry<>();

    private final File contentFolder;
    private final FileConfig configFile;

    private final ILangProvider langProvider;

    private static MidnightMenusAPI instance;

    public MidnightMenusAPI(File configFolder, ConfigSection defaultLang) {

        instance = this;

        ConfigProvider prov = MidnightCoreAPI.getInstance().getDefaultConfigProvider();

        contentFolder = new File(configFolder, "content");
        configFile = new FileConfig(new File(configFolder, "config" + prov.getFileExtension()), prov);

        langProvider = MidnightCoreAPI.getInstance().getModule(ILangModule.class).createLangProvider(new File(configFolder, "lang"), prov, defaultLang);

        loadConfig();
    }


    private void loadConfig() {

        ConfigProvider prov = MidnightCoreAPI.getInstance().getDefaultConfigProvider();

        if(!contentFolder.exists() && !(contentFolder.mkdirs() && contentFolder.setWritable(true) && contentFolder.setReadable(true))) {

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

                String path = rf.getName().substring(0, rf.getName().length() - prov.getFileExtension().length());
                ConfigSection sec = prov.loadFromFile(rf);

                REQUIREMENT_REGISTRY.register(MIdentifier.create(namespace, path), MenuRequirement.SERIALIZER.deserialize(sec));
            });

            forEachFile(menusFolder, mf -> {

                String path = mf.getName().substring(0, mf.getName().length() - prov.getFileExtension().length());
                ConfigSection sec = prov.loadFromFile(mf);

                MENU_REGISTRY.register(MIdentifier.create(namespace, path), MidnightMenu.parse(sec));
            });
        });

        System.out.println("Registered " + REQUIREMENT_REGISTRY.getSize() + " requirements.");
        System.out.println("Registered " + MENU_REGISTRY.getSize() + " menus.");
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

    public ILangProvider getLangProvider() {
        return langProvider;
    }

    public void reload() {

        loadConfig();
    }

}
