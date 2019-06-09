package tests

import java.time.{Year, ZoneId}

import com.malliina.network.NetworkDevice
import org.scalatest.FunSuite

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class SimpleTests extends FunSuite {
  test("1+1 is 2") {
    assert(1 + 1 === 2)
  }

  test("ranges") {
    assert((1 to 1).size === 1)
  }

  test("Duration.toString") {
    val output = 6.seconds.toString()
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

  test("Future exceptions") {
    val failingFuture = Future.successful(1).map(_ => throw new Exception)
    val recovered = failingFuture.recover {
      case _: Exception => 2
    }
    val two = Await.result(recovered, 2.seconds)
    assert(two === 2)
  }

  test("Future.recoverAll") {
    import com.malliina.concurrent.FutureOps
    val f = Future(throw new IllegalArgumentException)
    val f2 = f.recoverAll(t => 5)
    val results = Await.result(f2, 1.second)
    assert(results === 5)
  }

  ignore("year now") {
    println(Year.now(ZoneId.of("Europe/Helsinki")).getValue)
  }
}
