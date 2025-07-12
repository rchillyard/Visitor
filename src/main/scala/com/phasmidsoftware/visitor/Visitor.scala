package com.phasmidsoftware.visitor

trait Visitor[X] {

  def visit(msg: Message)(x: X): Visitor[X]

  def unit: Visitor[X] = this
}
