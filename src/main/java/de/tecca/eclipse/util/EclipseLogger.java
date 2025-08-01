package de.tecca.eclipse.util;

import org.bukkit.plugin.Plugin;
import java.util.logging.Logger;
import java.util.logging.Level;

public class EclipseLogger {

    private final Logger logger;
    private final String prefix;

    public EclipseLogger(Plugin plugin) {
        this.logger = plugin.getLogger();
        this.prefix = "[" + plugin.getName() + "] ";
    }

    public EclipseLogger(Plugin plugin, String component) {
        this.logger = plugin.getLogger();
        this.prefix = "[" + plugin.getName() + "][" + component + "] ";
    }

    public void info(String message) {
        logger.info(prefix + message);
    }

    public void warning(String message) {
        logger.warning(prefix + message);
    }

    public void severe(String message) {
        logger.severe(prefix + message);
    }

    public void debug(String message) {
        logger.log(Level.FINE, prefix + "[DEBUG] " + message);
    }

    public void trace(String message) {
        logger.log(Level.FINER, prefix + "[TRACE] " + message);
    }

    public void log(Level level, String message) {
        logger.log(level, prefix + message);
    }

    public void log(Level level, String message, Throwable throwable) {
        logger.log(level, prefix + message, throwable);
    }

    public Logger getLogger() {
        return logger;
    }
}