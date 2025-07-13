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
trait Appendable[X] {

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
 * A wrapper around `BufferedWriter` that provides an implementation of the `Appendable`
 * trait for appending `String` content to the underlying writer.
 *
 * `AppendableWriter` enables efficient string appending while maintaining compatibility
 * with the `Appendable` trait to support method chaining and scalability in writing operations.
 *
 * @param bufferedWriter the underlying `BufferedWriter` used for appending string data
 */
case class AppendableWriter(bufferedWriter: BufferedWriter) extends Appendable[String] {

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
   * This method initializes a `FileWriter` in append mode for the provided file
   * and then uses it to create an `AppendableWriter` instance.
   *
   * @param file the file to which the `AppendableWriter` will append data
   * @return an instance of `AppendableWriter` that writes to the specified file in append mode
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
    apply(new BufferedWriter(fileWriter))

  /**
   * Creates an `AppendableWriter` instance using the specified `BufferedWriter`.
   *
   * This method wraps the provided `BufferedWriter` to create an `AppendableWriter`
   * instance, enabling appending operations in compliance with the `Appendable`
   * interface. The returned `AppendableWriter` can be used for appending string
   * data efficiently while leveraging the buffering functionality of `BufferedWriter`.
   *
   * @param bufferedWriter the `BufferedWriter` to wrap, providing the underlying
   *                       writing mechanism for the `AppendableWriter`
   * @return an instance of `AppendableWriter` that wraps the given `BufferedWriter`
   */
  def apply(bufferedWriter: BufferedWriter): AppendableWriter =
    new AppendableWriter(bufferedWriter)
}