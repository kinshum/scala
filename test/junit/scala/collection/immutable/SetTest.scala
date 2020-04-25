package scala.collection.immutable

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import scala.tools.testing.AllocationTest

@RunWith(classOf[JUnit4])
class SetTest extends AllocationTest {
  @Test
  def test_SI8346_toSet_soundness(): Unit = {
    val any2stringadd = "Disabled string conversions so as not to get confused!"
    
    def any[A](set: Set[A]): Set[Any] = {
      val anyset = set.toSet[Any]
      assert((anyset + "fish") contains "fish")
      anyset
    }

    // Make sure default immutable Set does not rebuild itself on widening with toSet
    // Need to cover 0, 1, 2, 3, 4 elements as special cases
    var si = Set.empty[Int]
    assert(si eq si.toSet[Any])
    for (i <- 1 to 5) {
      val s1 = Set(Array.range(1, i+1): _*)
      val s2 = si + i
      val s1a = any(s1)
      val s2a = any(s2)
      assert(s1 eq s1a)
      assert(s2 eq s2a)
      si = s2
    }

    // Make sure BitSet correctly rebuilds itself on widening with toSet
    // Need to cover empty, values 0-63, values 0-127 as special cases
    val bitsets = Seq(BitSet.empty, BitSet(23), BitSet(23, 99), BitSet(23, 99, 141))
    bitsets.foreach{ b =>
      val ba = any(b)
      assert(b ne ba)
      assertEquals(b, ba)
    }

    // Make sure HashSet (and by extension, its implementing class HashTrieSet)
    // does not rebuild itself on widening by toSet
    val hashset = HashSet(1, 3, 5, 7)
    val hashseta = any(hashset)
    assert(hashset eq hashseta)

    // Make sure ListSet does not rebuild itself on widening by toSet
    // (Covers Node also, since it subclasses ListSet)
    val listset = ListSet(1, 3, 5, 7)
    val listseta = any(listset)
    assert(listset eq listseta)

    // Make sure SortedSets correctly rebuild themselves on widening with toSet
    // Covers TreeSet and keySet of SortedMap also
    val sortedsets = Seq(
      SortedSet.empty[Int], SortedSet(5), SortedSet(1,2,3,5,4),
      SortedMap(1 -> "cod", 2 -> "herring").keySet
    )
    sortedsets.foreach{ set => 
      val seta = any(set)
      assert(set ne seta)
      assertEquals(set, seta)
    }

    // Make sure ValueSets correctly rebuild themselves on widening with toSet
    object WeekDay extends Enumeration {
      type WeekDay = Value
      val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
    }
    val valuesa = any(WeekDay.values)
    assert(WeekDay.values ne valuesa)
    assertEquals(WeekDay.values, valuesa)

    // Make sure regular Map keySets do not rebuild themselves on widening with toSet
    val mapset = Map(1 -> "cod", 2 -> "herring").keySet
    val mapseta = any(mapset)
    assert(mapset eq mapseta)
  }

  def generate(n:Int): Set[String] = {
    (0 until n).map { i => s"key $i" }(scala.collection.breakOut)
  }
  def nonAllocatingEmptyPlusPlusN(n:Int): Unit = {
    val base = generate(n)
    assertSame(base, nonAllocating {
      Set.empty[String] ++ base
    })
  }
  def nonAllocatingEmptyUnionN(n:Int): Unit = {
    val base = generate(n)
    assertSame(base, nonAllocating {
      Set.empty[String] union base
    })
  }
  @Test
  def testGenerate(): Unit = {
    assertSame(Set.empty, generate(0))
    assertEquals(Set("key 0"), generate(1))
  }
  @Test
  def nonAllocatingEmptyUnionEmpty(): Unit = {
    nonAllocatingEmptyUnionN(0)
  }
  @Test
  def nonAllocatingEmptyPlusPlusEmpty(): Unit = {
    nonAllocatingEmptyPlusPlusN(0)
  }
  @Test
  def nonAllocatingEmptyUnionSmall(): Unit = {
    (1 to 4) foreach nonAllocatingEmptyUnionN
  }
  @Test
  def nonAllocatingEmptyPlusPlusSmall(): Unit = {
    (1 to 4) foreach nonAllocatingEmptyPlusPlusN
  }
  @Test
  def nonAllocatingEmptyUnionLarge(): Unit = {
    nonAllocatingEmptyUnionN (100)
  }
  @Test
  def nonAllocatingEmptyPlusPlusLarge(): Unit = {
    nonAllocatingEmptyPlusPlusN(100)
  }

  @Test def builderCompare1: Unit = {
    for (size <- 0 to 100;
         start <- (0 to 10)) {
      val tBuilder = TreeMap.newBuilder[String, String]
      val sBuilder = SortedMap.newBuilder[String, String]
      val control = HashMap.newBuilder[String, String]
      for (i <- start to start + size) {
        sBuilder += i.toString -> ""
        tBuilder += i.toString -> ""
        control += i.toString -> ""
      }
      val treeMap = tBuilder.result()
      val sortMap = sBuilder.result()
      val expected = control.result()

      assertEquals(expected.size, treeMap.size)
      assertEquals(expected.size, sortMap.size)

      assertEquals(expected, treeMap)
      assertEquals(expected, sortMap)

      assertEquals(expected, treeMap.iterator.toMap)
      assertEquals(expected, sortMap.iterator.toMap)

    }
  }
  @Test def builderCompare2: Unit = {
    for (size <- 0 to 100;
         start <- (0 to 10)) {
      val data = for (i <- start to start + size) yield {
        i -> ""
      }
      val treeMap =  TreeMap(data : _*)
      val sortMap =  SortedMap(data : _*)
      val expected = HashMap( data : _*)

      assertEquals(expected.size, treeMap.size)
      assertEquals(expected.size, sortMap.size)

      assertEquals(expected, treeMap)
      assertEquals(expected, sortMap)

      assertEquals(expected, treeMap.iterator.toMap)
      assertEquals(expected, sortMap.iterator.toMap)

    }
  }
}
