package sigmastate

import org.scalatest.matchers.should.Matchers
import sigmastate.Values.{SigmaPropValue, SigmaBoolean, ErgoTree}
import org.ergoplatform.ErgoTreePredef

trait TestsBase extends Matchers with VersionTesting {
  /** Set this to true to enable debug console output in tests */
  val printDebugInfo: Boolean = false

  /** Print debug message if printDebugInfo is true */
  def printDebug(msg: Any): Unit = if (printDebugInfo) println(msg)

  /** Current ErgoTree header flags assigned dynamically using [[CrossVersionProps]] and
    * ergoTreeVersionInTests.
    */
  def ergoTreeHeaderInTests: Byte = ErgoTree.headerWithVersion(ergoTreeVersionInTests)

  /** Obtains [[ErgoTree]] which corresponds to True proposition using current
    * ergoTreeHeaderInTests. */
  def TrueTree: ErgoTree = ErgoTreePredef.TrueProp(ergoTreeHeaderInTests)

  /** Obtains [[ErgoTree]] which corresponds to False proposition using current
    * ergoTreeHeaderInTests. */
  def FalseTree: ErgoTree = ErgoTreePredef.FalseProp(ergoTreeHeaderInTests)

  /** Transform proposition into [[ErgoTree]] using current ergoTreeHeaderInTests. */
  def mkTestErgoTree(prop: SigmaPropValue): ErgoTree =
    ErgoTree.fromProposition(ergoTreeHeaderInTests, prop)

  /** Transform sigma proposition into [[ErgoTree]] using current ergoTreeHeaderInTests. */
  def mkTestErgoTree(prop: SigmaBoolean): ErgoTree =
    ErgoTree.fromSigmaBoolean(ergoTreeHeaderInTests, prop)

  /** Max cost of script execution in tests. */
  val scriptCostLimitInTests: Int = 1000000
}
