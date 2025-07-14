package com.phasmidsoftware.visitor

import java.io.{BufferedWriter, File, FileWriter}

/**
 * A generic trait representing a collection or structure that supports appending elements of type `X`.
 *
 * `Appendable` provides an abstraction for objects that can be extended with additional elements,
 * returning a new instance that includes the appended element while keeping existing contents unchanged.
 *
 * This can be useful in cases where immutability of the original object is desired while adding new elements
 * to produce a new, enriched instance.
 *
 * @tparam X the type of elements that can be appended to the `Appendable`
 */
trait Appendable[X] extends AutoCloseable {

  /**
   * Appends the specified element to this `Appendable` object, returning a new instance
   * of the `Appendable` with the element included.
   *
   * @param x the element to be appended
   * @return a new `Appendable[X]` instance containing the existing elements and the newly appended element
   */
  def append(x: X): Appendable[X]
}

/**
 * A class for appending string data to a `BufferedWriter`, enabling efficient
 * writing operations with the capability to append text in a chained manner.
 *
 * The `AppendableWriter` is a wrapper around a `BufferedWriter` and an additional
 * `AutoCloseable` instance. It supports appending string content and ensures proper
 * management of the underlying resources through the `close` method.
 *
 * @param bufferedWriter the `BufferedWriter` instance used for writing data
 * @param closeable      an additional `AutoCloseable` resource associated with this writer
 */
case class AppendableWriter(bufferedWriter: BufferedWriter)(closeable: AutoCloseable) extends Appendable[String] {

  /**
   * Appends the specified string to this `AppendableWriter` instance.
   * The string is written to the underlying `BufferedWriter`, and the current
   * instance is returned to enable method chaining.
   *
   * @param x the string to be appended to the `AppendableWriter`
   * @return the current `AppendableWriter` instance with the appended string
   */
  def append(x: String): Appendable[String] = {
    bufferedWriter.append(x)
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
    bufferedWriter.close()
    closeable.close()
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
    new AppendableWriter(new BufferedWriter(fileWriter))(fileWriter)
}

/**
 * The `HasAppendables` trait defines an abstraction for entities that manage a collection of `Appendable` instances.
 *
 * This trait provides methods to access and interact with the associated `Appendable` and `Journal` elements,
 * allowing filtering and additional operations on these appendable entities.
 *
 * @tparam X the type of elements managed by the appendables
 */
trait HasAppendables[X] {

  /**
   * Retrieves the collection of `Appendable[X]` instances associated with this `AbstractVisitor`.
   *
   * The method provides access to all the appendable entities that the visitor interacts with.
   * This can be useful for iterating over, modifying, or closing the appendables as a group.
   *
   * @return an `Iterable` containing the appendable elements of type `Appendable[X]` associated with this visitor
   */
  def appendables: Iterable[Appendable[X]]

  /**
   * Retrieves an iterable collection of all `Journal[X]` instances from this `Visitor`.
   *
   * This method filters the iterable collection of `Appendable[X]` instances, returning only those
   * that are of type `Journal[X]`. It performs a type check on each `Appendable[X]` and selectively
   * includes those that match the `Journal[X]` type.
   *
   * @return an `Iterable` containing all `Journal[X]` instances managed by this `Visitor`
   */
  def journals: Iterable[Journal[X]] =
    for {
      appendable <- appendables
      xjo: Option[Journal[X]] = appendable match {
        case x: Journal[X] => Some(x);
        case _ => None
      }
      journal <- xjo
    } yield journal

  /**
   * Retrieves an iterable collection of all `Journal[X]` instances from this `Visitor`.
   *
   * This method filters the iterable collection of `Appendable[X]` instances, returning only those
   * that are of type `Journal[X]`. It performs a type check on each `Appendable[X]` and selectively
   * includes those that match the `Journal[X]` type.
   *
   * @return an `Iterable` containing all `Journal[X]` instances managed by this `Visitor`
   */
  def mapJournals: Iterable[Journal[X]] =
    journals filter {
      case x: MapJournal[_, _] => true;
      case _ => false
    }
}