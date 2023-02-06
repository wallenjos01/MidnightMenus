package org.wallentines.midnightmenus.api;

import org.wallentines.midnightcore.api.text.LangProvider;
import org.wallentines.mdcfg.ConfigSection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightmenus.api.menu.MenuRequirement;
import org.wallentines.midnightmenus.api.menu.MidnightMenu;

public abstract class MidnightMenusAPI {

    protected static final Logger LOGGER = LogManager.getLogger("MidnightMenus");
    private static MidnightMenusAPI INSTANCE;

    protected MidnightMenusAPI() {
        INSTANCE = this;
    }

    public static MidnightMenusAPI getInstance() {
        return INSTANCE;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public abstract ConfigSection getConfig();

    public abstract LangProvider getLangProvider();

    public abstract void reload();

    public abstract Registry<MidnightMenu> getMenuRegistry();

    public abstract Registry<MenuRequirement> getRequirementRegistry();
}
