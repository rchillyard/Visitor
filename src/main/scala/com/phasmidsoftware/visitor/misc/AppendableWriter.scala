package com.phasmidsoftware.visitor.misc

import java.io.*

/**
 * `AppendableWriter` is a case class that wraps a `Writer` instance, enabling string
 * appending functionality along with proper resource management.
 *
 * This class implements the `Appendable` trait specialized for `String`, providing an
 * efficient way to append strings to an underlying writer.
 *
 * The `closeable` parameter is an optional resource that will be closed when the
 * `close` method is invoked, ensuring that any additional resources associated with
 * the writer are properly released.
 *
 * @param writer    the underlying `Writer` instance to which strings are appended
 * @param closeable an optional `AutoCloseable` resource that will be closed along with the writer
 */
case class AppendableWriter(writer: Writer)(closeable: Option[AutoCloseable]) extends com.phasmidsoftware.visitor.core.Appendable[String] {

  /**
   * Appends the specified string to this `AppendableWriter` instance.
   * The string is written to the underlying `BufferedWriter`, and the current
   * instance is returned to enable method chaining.
   *
   * @param x the string to be appended to the `AppendableWriter`
   * @return the current `AppendableWriter` instance with the appended string
   */
  def append(x: String): com.phasmidsoftware.visitor.core.Appendable[String] = {
    writer.append(x)
    this
  }

  /**
   * Closes the underlying `BufferedWriter`.
   *
   * This method ensures that any resources associated with the writer are released
   * and any data still present in the buffer is flushed before closing.
   *
   * @return Unit.
   */
  def close(): Unit = {
    writer.close()
    closeable foreach (_.close())
  }
}

/**
 * Companion object for `AppendableWriter` providing factory methods for creating instances.
 *
 * The `AppendableWriter` allows for efficient and convenient appending of string data
 * to files or other writable resources via a `BufferedWriter`. The factory methods
 * in the companion object ensure flexibility by enabling creation from file names,
 * `File` instances, `FileWriter` objects, or directly from `BufferedWriter` instances.
 */
object AppendableWriter {

  /**
   * Creates an `AppendableWriter` instance for the given file name.
   *
   * The file name must not be `null` or empty and should start with a forward slash ("/")
   * and end with ".txt". If these conditions are not met, an `IllegalArgumentException`
   * will be thrown. This method internally constructs a `File` instance and delegates
   * creation to another `apply` method.
   *
   * @param name the name of the file (must be non-null, non-empty, start with "/", and end with ".txt")
   * @return an `AppendableWriter` instance associated with the specified file
   * @throws IllegalArgumentException if `name` is `null`, empty, does not start with "/", or does not end with ".txt"
   */
  def apply(name: String): AppendableWriter = {
    require(name != null)
    require(name.nonEmpty)
    require(name.startsWith("/"))
    require(name.endsWith(".txt"))
    apply(new File(name))
  }

  /**
   * Creates an `AppendableWriter` instance using the specified `File`.
   *
   * This method initializes a `FileWriter` in append-mode for the provided file
   * and then uses it to create an `AppendableWriter` instance.
   *
   * @param file the file to which the `AppendableWriter` will append data
   * @return an instance of `AppendableWriter` that writes to the specified file in append-mode
   */
  def apply(file: File): AppendableWriter =
    apply(new FileWriter(file, true))

  /**
   * Creates a new instance of `AppendableWriter` using the provided `FileWriter`.
   *
   * The `AppendableWriter` wraps the `FileWriter` with a `BufferedWriter`, enabling efficient
   * writing operations, and provides an interface for appending string content.
   *
   * @param fileWriter the `FileWriter` to be wrapped, enabling appending functionality to the file
   * @return a new `AppendableWriter` instance wrapping the provided `FileWriter`
   */
  def apply(fileWriter: FileWriter): AppendableWriter =
    new AppendableWriter(new BufferedWriter(fileWriter))(Some(fileWriter))

  /**
   * Creates a new instance of `AppendableWriter` with a default `StringWriter` as the underlying writer.
   *
   * This method provides a convenient way to construct an `AppendableWriter`
   * in-memory without specifying a file or an external writer.
   *
   * @return a new `AppendableWriter` instance wrapping a default `StringWriter`
   */
  def apply(): AppendableWriter =
    new AppendableWriter(new StringWriter())(None)
}
