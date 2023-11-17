package com.malliina.network

import java.net.{InetAddress, NetworkInterface}

import scala.collection.JavaConverters.enumerationAsScalaIteratorConverter

trait NetworkDevice {
  def hostAddresses: Seq[String] = addresses.map(_.getHostAddress)

  def addresses: Seq[InetAddress] =
    (for {
      interface <- NetworkInterface.getNetworkInterfaces.asScala
      address <- interface.getInetAddresses.asScala
      if !address.isAnyLocalAddress && !address.isLinkLocalAddress && !address.isLoopbackAddress
    } yield address).toSeq

  /** Given `sampleIP`, which is a numerical IP address, returns a list of addresses close to that
    * IP. A subnet of 255.255.255.0 is assumed.
    *
    * The IPs are returned in increasing order.
    *
    * Example: if `sampleIP` is 10.0.0.6 and `radius` is 2, this method returns the following IPs:
    * 10.0.0.4, 10.0.0.5, 10.0.0.7, 10.0.0.8.
    *
    * @param sampleIP
    *   a sample IP
    * @param radius
    *   the maximum distance from `sampleIP` of the returned addresses
    * @return
    *   addresses in the same subnet as `sampleIP`, excluding `sampleIP`
    */
  def adjacentIPs(sampleIP: String, radius: Int = 10): List[String] = {
    val (nw, lastOctet) = ipSplit(sampleIP)
    val range =
      (math.max(1, lastOctet - radius) to math.min(254, lastOctet + radius)).filter(_ != lastOctet)
    range.map(num => s"$nw.$num").toList
  }

  /** @param ip
    *   a numerical IP address
    * @return
    *   the network part of the IP and its last octet
    */
  def ipSplit(ip: String): (String, Int) = {
    val lastDotIndex = ip lastIndexOf '.'
    val nw = ip.substring(0, lastDotIndex)
    val lastOctet = ip.substring(lastDotIndex + 1, ip.length).toInt
    (nw, lastOctet)
  }

  def network(ip: String) = ipSplit(ip)._1
}

object NetworkDevice extends NetworkDevice
