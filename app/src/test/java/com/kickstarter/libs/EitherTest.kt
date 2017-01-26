package com.kickstarter.libs

import org.junit.Assert.*
import org.junit.Test

class EitherTest {
  @Test fun testEither_CaseAnalysis() {
    val square: (Int) -> Int = { it * it }
    val length: (String) -> Int = String::length

    assertEquals(4, Either.Companion.left<Int, String>(2).either(ifLeft = square, ifRight = length))
    assertEquals(5, Either.Companion.right<Int, String>("hello").either(ifLeft = square, ifRight = length))
  }

  @Test fun testEither_IsLeft() {
    val intOrString = Either.Companion.left<Int, String>(1)
    assertTrue(intOrString.isLeft())
    assertFalse(intOrString.isRight())
  }

  @Test fun testEither_IsRight() {
    val intOrString = Either.Companion.right<Int, String>("hello")
    assertTrue(intOrString.isRight())
    assertFalse(intOrString.isLeft())
  }

  @Test fun testEither_Left() {
    val intOrString = Either.Companion.left<Int, String>(1)
    assertEquals(1, intOrString.left)
    assertEquals(null, intOrString.right)
  }

  @Test fun testEither_Right() {
    val intOrString = Either.Companion.right<Int, String>("hello")
    assertEquals("hello", intOrString.right)
    assertEquals(null, intOrString.left)
  }
}
