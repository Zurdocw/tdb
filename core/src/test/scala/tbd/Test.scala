/**
 * Copyright (C) 2013 Carnegie Mellon University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tbd.test

import org.scalatest._

import tbd.{Adjustable, Changeable, Dest, Mutator, ListNode, TBD}
import tbd.mod.{Matrix, Mod}

class ArrayMapTest extends Adjustable {
  def run(dest: Dest[Any], tbd: TBD): Changeable[Any] = {
    val array = tbd.input.getArray[Mod[String]]()
    val mappedArray = tbd.map(array, (_: String) + " mapped")
    tbd.write(dest, mappedArray)
  }
}

class ListMapTest extends Adjustable {
  def run(dest: Dest[Any], tbd: TBD): Changeable[Any] = {
    val list = tbd.input.getList[String]()
    val mappedList = tbd.parMap(list, (_: String) + " mapped")
    tbd.write(dest, mappedList)
  }
}

class ListReduceTest extends Adjustable {
  def run(dest: Dest[Any], tbd: TBD): Changeable[Any] = {
    val list = tbd.input.getList[Int]()
    val mappedList = tbd.map(list, (_: Int) + 1)
    val reduced = tbd.reduce(mappedList, (_: Int) + (_: Int))
    tbd.write(dest, reduced)
  }
}

class MatrixMultTest extends Adjustable {
  def run(dest: Dest[Any], tbd: TBD): Changeable[Any] = {
    val one = tbd.input.get[Matrix](1)
    val two = tbd.input.get[Matrix](2)

    tbd.write(dest, one.mult(tbd, two)).asInstanceOf[Changeable[Any]]
  }
}

/*class MemoTest extends Adjustable {
  // Note: real client applications should NOT have mutable state like this.
  // We are just using it to ensure that the memoized function doesn't get
  // reexecuted as appropriate.
  var count = 0

  def run(dest: Dest, tbd: TBD): Changeable[Any] = {
    val one = tbd.input.get[Mod[Int]](1)
    val two = tbd.input.get[Mod[Int]](2)
    val memo = tbd.memo[Int, Int]()

    val memoMod = tbd.mod(dest => {
      memo(List(two))(() => {
	      tbd.read(two, valueTwo => {
	        count += 1
	        tbd.write(dest, valueTwo + 1)
	      })
      })
    })

    tbd.read(memoMod, (value1: Int) => {
      tbd.read(one, (value2: Int) => tbd.write(dest, value1 + value2))
    }).asInstanceOf[Changeable[Any]]
  }
}*/

class TestSpec extends FlatSpec with Matchers {
  "ArrayMapTest" should "return a correctly mapped array" in {
    val mutator = new Mutator()
    mutator.put(1, "one")
    mutator.put(2, "two")
    val output = mutator.run[Array[Mod[String]]](new ArrayMapTest())
    output.read().deep.mkString(", ") should be ("two mapped, one mapped")

    mutator.update(1, "three")
    mutator.propagate[Array[Mod[String]]]()
    val arr = output.read().asInstanceOf[Array[Mod[String]]]
    output.read().deep.mkString(", ") should be ("two mapped, three mapped")

    mutator.shutdown()
  }

  "ListMapTest" should "return a correctly mapped list" in {
    val mutator = new Mutator()
    mutator.put(1, "one")
    mutator.put(2, "two")
    val output = mutator.run(new ListMapTest())
    output.read().toString should be ("(one mapped, two mapped)")

    mutator.update(1, "three")
    mutator.propagate()
    output.read().toString should be ("(three mapped, two mapped)")

    mutator.put(3, "four")
    mutator.propagate()
    output.read().toString should be ("(three mapped, two mapped, four mapped)")

    mutator.update(2, "five")
    mutator.propagate()
    output.read().toString should be ("(three mapped, five mapped, four mapped)")

    mutator.put(4, "six")
    mutator.propagate()
    output.read().toString should be ("(three mapped, five mapped, four mapped, six mapped)")

    mutator.shutdown()
  }

  "ListReduceTest" should "return the reduced value" in {
    val mutator = new Mutator()
    mutator.put("one", 1)
    mutator.put("two", 2)
    val output = mutator.run(new ListReduceTest())
    output.read().toString should be ("5")

    mutator.put("three", 3)
    mutator.propagate()
    output.read().toString should be ("9")

    mutator.update("one", 4)
    mutator.propagate()
    output.read().toString should be ("12")

    mutator.update("three", 2)
    mutator.propagate()
    output.read().toString should be ("11")

    mutator.put("four", -1)
    mutator.propagate()
    output.read().toString should be ("11")

    mutator.update("four", 10)
    mutator.propagate()
    output.read().toString should be ("22")
  }

  /*"MatrixMult" should "do stuff" in {
    val mutator = new Mutator()
    mutator.putMatrix(1, Array(Array(1, 3)))
    mutator.putMatrix(2, Array(Array(5), Array(6)))
    val output = mutator.run(new MatrixMultTest())
    mutator.shutdown()
  }*/

  /*"MemoTest" should "do stuff" in {
    val mutator = new Mutator()
    mutator.put(1, 1)
    val twoMod = mutator.put(2, 10)
    val test = new MemoTest()
    val output = mutator.run[Int](test)
    output.read() should be (12)
    test.count should be (1)

    // Rerun without changes to ensure correct memoization.
    mutator.propagate[Int]().read() should be (12)
    test.count should be (1)

    // Change the mod used by the memoized function and rerun.
    twoMod.update(12)
    mutator.propagate[Int]().read() should be (14)
    test.count should be (2)

    mutator.shutdown()
  }*/
}
