package org.nexbook.tools.fixordergenerator.generator

import org.nexbook.tools.fixordergenerator.repository.PriceRepository
import org.scalatest.{FlatSpec, Matchers}

/**
 * Created by milczu on 11.12.15
 */
class OrderGeneratorTest extends FlatSpec with Matchers {

  "OrderGenerator" should "generate orders fast!" in {
    PriceGenerator.updatePrices(PriceRepository.defaultPrices)
    val orderGenerator = new OrderGenerator(new SymbolGenerator(List("GBP/USD", "EUR/USD", "USD/JPY")))

    val n = 100000
    val repeats = 4

    val startTime = System.nanoTime
    for (r <- 1 to repeats) {
      for (i <- 1 to n) {
        orderGenerator.generate()
      }
    }
    val endTime = System.nanoTime
    val execTime = (endTime - startTime) / repeats
    val avgNs = execTime.toDouble / n.toDouble

    println("Exec time for " + n + " orders: " + execTime + "ns - : " + (execTime / 1000000) + "ms. Avg ns: " + (avgNs / 1000000.00))
  }

}
