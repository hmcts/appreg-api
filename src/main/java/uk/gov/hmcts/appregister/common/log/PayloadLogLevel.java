package uk.gov.hmcts.appregister.common.log;

import org.slf4j.Logger;

/**
 * Supported logging levels for annotated payload logs.
 */
public enum PayloadLogLevel {
    INFO {
        @Override
        public void log(Logger logger, String message, String prefix, Object payload) {
            logger.info(message, prefix, payload);
        }
    },
    DEBUG {
        @Override
        public void log(Logger logger, String message, String prefix, Object payload) {
            logger.debug(message, prefix, payload);
        }
    };

    public abstract void log(Logger logger, String message, String prefix, Object payload);
}
