package com.nova.ledger.config;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class SqlColorConverter extends ClassicConverter {

    private static final String RESET = "\u001b[0m";
    private static final String GREEN = "\u001b[32m";
    private static final String YELLOW = "\u001b[33m";
    private static final String CYAN = "\u001b[36m";

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        String loggerName = event.getLoggerName();

        if (loggerName.contains("hibernate.SQL")) {
            return GREEN + message + RESET;
        }
        if (loggerName.contains("hibernate.orm.jdbc.bind")) {
            return YELLOW + message + RESET;
        }
        return message;
    }
}
