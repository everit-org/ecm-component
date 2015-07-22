package org.everit.osgi.ecm.component.ri.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * Internal LogService implementation that forwards every logs to JDK logger.
 *
 */
public class JavaLogService implements LogService {

  private static final Logger LOGGER = Logger.getLogger("org.everit.osgi.ecm.component.ri");

  @Override
  public void log(final int level, final String message) {
    LOGGER.log(translateLevel(level), message);
  }

  @Override
  public void log(final int level, final String message, final Throwable exception) {
    LOGGER.log(translateLevel(level), message, exception);
  }

  @Override
  public void log(@SuppressWarnings("rawtypes") final ServiceReference sr, final int level,
      final String message) {
    LOGGER.log(translateLevel(level), sr.toString() + ": " + message);
  }

  @Override
  public void log(@SuppressWarnings("rawtypes") final ServiceReference sr, final int level,
      final String message, final Throwable exception) {
    LOGGER.log(translateLevel(level), sr.toString() + ": " + message, exception);
  }

  private Level translateLevel(final int level) {
    Level result;
    switch (level) {
      case LogService.LOG_DEBUG:
        result = Level.FINE;
        break;
      case LogService.LOG_ERROR:
        result = Level.SEVERE;
        break;
      case LogService.LOG_INFO:
        result = Level.INFO;
        break;
      case LogService.LOG_WARNING:
        result = Level.WARNING;
        break;
      default:
        result = Level.INFO;
        break;
    }
    return result;
  }

}
