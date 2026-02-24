package com.phasmidsoftware.visitor.util

import com.phasmidsoftware.visitor.core.Visitor
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

/**
 * The `Logging` object provides functionality for flexible and controlled logging of messages and data.
 * It allows toggling logging state, configuring custom logging functions, and logging collections or values
 * with specified prefixes.
 *
 * The class also defines the implicit `Loggable` class, which provides syntactic sugar for logging via the `!!` operator.
 */
object Logging {

  /**
   * Sets the logging state to either enabled or disabled.
   *
   * @param b a boolean value indicating whether logging should be enabled (`true`) or disabled (`false`)
   * @return Unit
   */
  def setLogging(b: Boolean): Unit = logging = b

  /**
   * Determines whether logging is currently enabled.
   *
   * @return true if logging is enabled, false otherwise
   */
  def isLogging: Boolean = logging

  /**
   * Sets the log function to be used for logging messages.
   *
   * @param f a function that takes a String message as input and processes it (e.g., logs it)
   * @return Unit this method does not return a value
   */
  def setLogFunction(f: String => Unit): Unit = logFunction = f

  /**
   * Provides logging capabilities for various types of data.
   * The `Loggable` class augments a `String` with logging functionality, allowing logs to be
   * generated for values or collections annotated with a specified logging prefix.
   *
   * Use `!!` to log and return the original values of supported types.
   * Logging can only occur if it is explicitly enabled within a suitable logging context.
   *
   * @param w the logging prefix to be used when generating log entries
   */
  implicit class Loggable(w: String) {

    /**
     * Logs the provided value with the specified logging prefix and returns the original value.
     * Logging occurs only if the logging context is active.
     *
     * @param x the value to be logged; the type is generic and can be any type X
     * @return the original value passed as input
     */
    def !![X](x: X): X =
      logIt(w)(x)

    /**
     * Logs an `Option` value if logging is enabled and returns the original value.
     *
     * @param xo an `Option` containing the value to be logged; if `xo` is `Some(value)`,
     *           the value is logged, otherwise no action is performed.
     * @return the original `Option` value passed as input.
     */
    def !![X](xo: Option[X]): Option[X] =
      logIt(w)(xo)

    /**
     * Logs a `Try` value using the logging prefix `w` and returns the original value.
     * Logging occurs only if enabled within the logging context.
     *
     * @param xy the `Try` instance containing the value to be logged
     * @return the same `Try` instance passed as input
     */
    def !![X](xy: Try[X]): Try[X] =
      logIt(w)(xy)

    /**
     * Logs and returns the provided sequence of values.
     * The sequence is logged with the prefix associated with the implicit `Loggable` context.
     * Logging occurs only if logging is enabled in the current context.
     *
     * @param xy the sequence of values to be logged and returned
     * @return the original sequence of values after logging
     */
    def !![X](xy: Seq[X]): Seq[X] =
      logIt(w)(xy)
  }

  private val logger: Logger = LoggerFactory.getLogger(classOf[Visitor[?, ?, ?]])

  /**
   * Logs a message along with the provided value if logging is enabled and returns the value.
   *
   * @param w a String representing the prefix or message to be included in the log
   * @param x the value to be logged; it is matched and formatted appropriately
   * @return the original value passed as input
   */
  private def logIt[X](w: String)(x: X): X = {
    if (isLogging) {
      x match {
        case s: String =>
          logFunction(s"""$w: "$s"""".stripMargin)
        case _ =>
          logFunction(s"$w: $x")
      }
    }
    x
  }

  private var logFunction: String => Unit = s => logger.info(s)

  private var logging: Boolean = false
}
