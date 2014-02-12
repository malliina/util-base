package tests

import org.scalatest.FunSuite
import concurrent.duration._
import com.mle.network.NetworkDevice

/**
 *
 * @author mle
 */
class SimpleTests extends FunSuite {
  test("1+1 is 2") {
    assert(1 + 1 === 2)
  }
  test("ranges") {
    assert((1 to 1).size === 1)
  }
  test("Duration.toString") {
    val output = (6 seconds).toString()
    assert(output === "6 seconds")
  }
  test("NetworkDevice.adjacentIPs") {
    val actual = NetworkDevice.adjacentIPs("10.0.0.3", radius = 4)
    val expected = Seq("10.0.0.1", "10.0.0.2", "10.0.0.4", "10.0.0.5", "10.0.0.6", "10.0.0.7")
    assert(actual === expected)

    assert(NetworkDevice.adjacentIPs("10.0.0.1", radius = 0) === Nil)
    assert(Nil.splitAt(0) ===(Nil, Nil))
  }
  test("diff") {
    val s1 = Seq(1, 2, 3, 4, 5)
    val s2 = Seq(3, 4)
    assert((s1 diff s2) === Seq(1, 2, 5))
  }
}
