package sigmastate.eval

import sigmastate.SMethod
import sigmastate.Values.SValue
import sigmastate.lang.Terms
import sigmastate.serialization.OpCodes
import sigmastate.serialization.OpCodes.OpCode
import sigmastate.serialization.ValueSerializer.getSerializer
import scalan.util.Extensions.ByteOps
import debox.{Buffer => DBuffer, Map => DMap}
import sigmastate.interpreter.{CostItem, PerBlockCostItem, SeqCostItem, SimpleCostItem}
import spire.sp

import scala.reflect.ClassTag

abstract class StatItem[@sp (Long, Double) V] {
  /** How many data points has been collected */
  def count: Int

  /** Sum of all data points */
  def sum: V

  /** Returns arithmetic average value. */
  def avg: V
}

class StatCollection[@sp(Int) K, @sp(Long, Double) V]
  (implicit n: spire.math.Numeric[V], ctK: ClassTag[K], ctV: ClassTag[V]) {

  // NOTE: this class is mutable so better to keep it private
  class StatItemImpl extends StatItem[V] {
    val dataPoints: DBuffer[V] = DBuffer.ofSize[V](256)

    def addPoint(point: V) = dataPoints += point

    /** How many data points has been collected */
    def count: Int = dataPoints.length

    /** Sum of all data points */
    def sum: V = dataPoints.sum(spire.math.Numeric[V])

    /** Returns arithmetic average value. */
    def avg: V = n.div(sum, n.fromInt(count))
  }

  /** Timings of op codes. For performance debox.Map is used, which keeps keys unboxed. */
  private val opStat = DMap[K, StatItemImpl]()

  /** Update time measurement stats for a given operation. */
  final def addPoint(key: K, point: V) = {
    val item = opStat.getOrElse(key, null)
    if (item != null) {
      item.addPoint(point)
    } else {
      val item = new StatItemImpl
      item.addPoint(point)
      opStat(key) = item
    }
  }

  final def mapToArray[@sp C: ClassTag](f: (K, StatItem[V]) => C): Array[C] = {
    opStat.mapToArray(f)
  }
}

/** A simple profiler to measure average execution times of ErgoTree operations. */
class Profiler {

  // NOTE: this class is mutable so better to keep it private
  private class OpStat(
    /** The node on the evaluation stack. */
    val node: SValue,
    /** The time then this node evaluation was started. */
    val outerStart: Long,
    /** The accumulated time of evaluating all the sub-nodes */
    var innerTime: Long,
    /** The time then this nodes evaluation finished */
    val outerEnd: Long
  )

  /** If every recursive evaluation of every Value is marked with
    * [[onBeforeNode()]]/[[onAfterNode()]], then this stack corresponds to the stack of
    * recursive invocations of the evaluator. */
  private var opStack: List[OpStat] = Nil

  /** Called from evaluator (like [[sigmastate.interpreter.ErgoTreeEvaluator]])
    * immediately before the evaluator start recursive evaluation of the given node.
    */
  def onBeforeNode(node: SValue) = {
    val t = System.nanoTime()
    opStack = new OpStat(node, t, 0, t) :: opStack
  }

  /** Called from evaluator (like [[sigmastate.interpreter.ErgoTreeEvaluator]])
    * immediately after the evaluator finishes recursive evaluation of the given node.
    */
  def onAfterNode(node: SValue) = {
    val t = System.nanoTime()

    val op = opStack.head   // always non empty at this point
    opStack = opStack.tail  // pop current op
    assert(op.node.opCode == node.opCode, s"Inconsistent stack at ${op :: opStack}")

    val opFullTime = t - op.outerStart  // full time spent in this op

    // add this time to parent's innerTime (if any parent)
    if (opStack.nonEmpty) {
      val parent = opStack.head
      parent.innerTime += opFullTime
    } else {
      // we are on top level, do nothing
    }

    val opSelfTime = opFullTime - op.innerTime

    // update timing stats
    node match {
      case mc: Terms.MethodCall if mc.method.costFunc.isEmpty =>
        // NOTE: the remaining MethodCalls are profiled via addCostItem
        val m = mc.method
        addMcTime(m.objType.typeId, m.methodId, opSelfTime)
      case _ =>
        addOpTime(node.opCode, opSelfTime)
    }
  }

  // NOTE: this class is mutable so better to keep it private
  private class StatItem {
      val times: DBuffer[Long] = DBuffer.ofSize(256)

      def addTime(time: Long) = times += time

      /** How many times the operation has been executed */
      def count: Int = times.length

      /** Sum of all execution times */
      def sum: Long = times.sum(spire.math.Numeric[Long])

      /** Returns average time in nanoseconds. */
      def avgTimeNano: Long = sum / count

      /** Returns average time in microseconds. */
      def avgTimeMicroseconds: Long = {
        val avgTime = sum / count
        avgTime / 1000
      }
  }


  /** Timings of op codes. For performance debox implementation of Map is used. */
  private val opStat = new StatCollection[Int, Long]()

  /** Update time measurement stats for a given operation. */
  @inline private final def addOpTime(op: OpCode, time: Long) = {
    opStat.addPoint(OpCode.raw(op), time)
  }

  /** Timings of method calls */
  private val mcStat = new StatCollection[Int, Long]()

  /** Update time measurement stats for a given method. */
  @inline private final def addMcTime(typeId: Byte, methodId: Byte, time: Long) = {
    val key = typeId << 8 | methodId
    mcStat.addPoint(key, time)
  }

  /** Timings of cost items */
  private val costItemsStat = new StatCollection[CostItem, Long]()

  def addCostItem(costItem: CostItem, time: Long) = {
    costItemsStat.addPoint(costItem, time)
  }

  /** Estimation errors for each script */
  private val estimationStat = new StatCollection[String, Double]()

  def addEstimation(script: String, cost: Int, actualTimeNano: Long) = {
    val actualTimeMicro = actualTimeNano.toDouble / 1000
    val delta = Math.abs(cost.toDouble - actualTimeMicro)
    val error = delta / actualTimeMicro
    estimationStat.addPoint(script, error)
  }

  /** Prints the operation timing table using collected information.
    */
  def opStatTableString(): String = {
    val opCodeLines = opStat.mapToArray { case (key, stat) =>
      val time = stat.avg
      val opCode = OpCode @@ key.toByte
      val ser = getSerializer(opCode)
      val opName = ser.opDesc.typeName
      (opName, (opCode.toUByte - OpCodes.LastConstantCode).toString, time, stat.count.toString)
    }.toList.sortBy(_._3)(Ordering[Long].reverse)

    val mcLines = mcStat.mapToArray { case (key, stat) =>
      val methodId = (key & 0xFF).toByte
      val typeId = (key >> 8).toByte
      val time = stat.avg
      val m = SMethod.fromIds(typeId, methodId)
      val typeName = m.objType.typeName
      (s"$typeName.${m.name}", typeId, methodId, time, stat.count.toString)
    }.toList.sortBy(r => (r._2,r._3))(Ordering[(Byte,Byte)].reverse)

    val ciLines = costItemsStat.mapToArray { case (ci, stat) =>
      val time = ci match {
        case _: SimpleCostItem => stat.avg
        case SeqCostItem(_, _, nItems) => stat.avg / nItems
        case PerBlockCostItem(_, _, nBlocks) => stat.avg / nBlocks
      }
      (ci.toString, time, stat.count.toString)
    }.toList.sortBy(_._2)(Ordering[Long].reverse)

    val estLines = estimationStat.mapToArray { case (script, stat) =>
      val time = stat.avg
      (script, time, stat.count.toString)
    }.toList.sortBy(_._2)(Ordering[Double].reverse)


    val rows = opCodeLines
        .map { case (opName, opCode, time, count) =>
          val key = s"$opName.opCode".padTo(30, ' ')
          s"$key -> $time,  // count = $count "
        }
        .mkString("\n")

    val mcRows = (mcLines)
        .map { case (opName, typeId, methodId, time, count) =>
          val key = s"($typeId.toByte, $methodId.toByte)".padTo(25, ' ')
          s"$key -> $time,  // count = $count, $opName "
        }
        .mkString("\n")

    val ciRows = (ciLines)
        .map { case (opName, time, count) =>
          val key = s"$opName".padTo(30, ' ')
          s"$key -> $time,  // count = $count "
        }
        .mkString("\n")

    val estRows = (estLines)
        .map { case (opName, time, count) =>
          val key = s"$opName".padTo(30, ' ')
          s"$key -> $time,  // count = $count "
        }
        .mkString("\n")

    s"""
      |-----------
      |$rows
      |-----------
      |$mcRows
      |-----------
      |$ciRows
      |-----------
      |$estRows
      |-----------
     """.stripMargin
  }

}

