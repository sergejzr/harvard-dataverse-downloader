package com.example.dataverse.downloader.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class AppLogger {
    private static volatile boolean initialized;

    private AppLogger() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        synchronized (AppLogger.class) {
            if (initialized) {
                return;
            }

            Logger root = Logger.getLogger("");
            root.setLevel(Level.INFO);

            for (var handler : root.getHandlers()) {
                root.removeHandler(handler);
            }

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            consoleHandler.setFormatter(new SimpleLineFormatter());
            root.addHandler(consoleHandler);

            try {
                Path logDir = Paths.get(System.getProperty("user.home"), ".dataverse-downloader", "logs");
                Files.createDirectories(logDir);

                FileHandler fileHandler = new FileHandler(
                        logDir.resolve("app-%g.log").toString(),
                        2_000_000,
                        5,
                        true);
                fileHandler.setLevel(Level.ALL);
                fileHandler.setFormatter(new SimpleLineFormatter());
                root.addHandler(fileHandler);
            } catch (IOException e) {
                root.log(Level.WARNING, "Could not initialize file logging", e);
            }

            initialized = true;
        }
    }

    public static Logger getLogger(Class<?> type) {
        init();
        return Logger.getLogger(type.getName());
    }

    private static final class SimpleLineFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String thrown = "";
            if (record.getThrown() != null) {
                thrown = System.lineSeparator() + stackTrace(record.getThrown());
            }

            return String.format(
                    "%1$tF %1$tT [%2$s] %3$s - %4$s%5$s%n",
                    record.getMillis(),
                    record.getLevel().getName(),
                    record.getLoggerName(),
                    formatMessage(record),
                    thrown);
        }

        private String stackTrace(Throwable t) {
            StringBuilder sb = new StringBuilder();
            sb.append(t).append(System.lineSeparator());
            for (StackTraceElement ste : t.getStackTrace()) {
                sb.append("    at ").append(ste).append(System.lineSeparator());
            }
            Throwable cause = t.getCause();
            if (cause != null && cause != t) {
                sb.append("Caused by: ").append(stackTrace(cause));
            }
            return sb.toString();
        }
    }
}