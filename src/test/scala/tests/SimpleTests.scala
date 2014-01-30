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
  test("Duration.toString") {
    val output = (6 seconds).toString()
    assert(output === "6 seconds")
  }
  test("NetworkDevice.adjacentIPs") {
    val actual = NetworkDevice.adjacentIPs("10.0.0.3", radius = 4)
    val expected = Seq("10.0.0.1", "10.0.0.2", "10.0.0.4", "10.0.0.5", "10.0.0.6", "10.0.0.7")
    assert(actual === expected)
  }
}
