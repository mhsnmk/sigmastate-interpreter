package sigmastate.utils

import org.ergoplatform.ErgoBox.TokenId
import scorex.util.{ModifierId, idToBytes}
import scorex.utils.{Ints, Longs, Shorts}
import sigmastate.eval.{Digest32Coll, SigmaDsl}
import special.collection.Coll

object Extensions {
  implicit class ByteOpsForSigma(val b: Byte) extends AnyVal {
    /** Returns a big-endian representation of this Byte in a collection of bytes.
      * For example, the Byte value {@code 0x12} would yield the
      * byte array {@code {0x12}}.
      */
    def toBytes: Coll[Byte] = SigmaDsl.Colls.fromItems(b)

    /** Returns a big-endian representation of this numeric in a collection of Booleans.
      * Each boolean corresponds to one bit.
      */
    def toBits: Coll[Boolean] = ???
  }

  implicit class ShortOpsForSigma(val x: Short) extends AnyVal {
    /** Returns a big-endian representation of this Short in a collection of bytes.
      * For example, the Short value {@code 0x1213} would yield the
      * byte array {@code {0x12, 0x13}}.
      */
    def toBytes: Coll[Byte] = SigmaDsl.Colls.fromArray(Shorts.toByteArray(x))

    /** Returns a big-endian representation of this numeric in a collection of Booleans.
      * Each boolean corresponds to one bit.
      */
    def toBits: Coll[Boolean] = ???
  }

  implicit class IntOpsForSigma(val x: Int) extends AnyVal {
    /** Returns a big-endian representation of this Int in a collection of bytes.
      * For example, the Int value {@code 0x12131415} would yield the
      * byte array {@code {0x12, 0x13, 0x14, 0x15}}.
      */
    def toBytes: Coll[Byte] = SigmaDsl.Colls.fromArray(Ints.toByteArray(x))

    /** Returns a big-endian representation of this numeric in a collection of Booleans.
      * Each boolean corresponds to one bit.
      */
    def toBits: Coll[Boolean] = ???
  }

  implicit class LongOpsForSigma(val x: Long) extends AnyVal {
    /** Returns a big-endian representation of this Long in a collection of bytes.
      * For example, the Long value {@code 0x1213141516171819} would yield the
      * byte array {@code {0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19}}.
      */
    def toBytes: Coll[Byte] = SigmaDsl.Colls.fromArray(Longs.toByteArray(x))

    /** Returns a big-endian representation of this numeric in a collection of Booleans.
      * Each boolean corresponds to one bit.
      *
      * @since 2.0
      */
    def toBits: Coll[Boolean] = ???
  }

  /** Provides extension methods for `ModifierId` instances.
    *
    * @param id the `ModifierId` instance
    */
  implicit class ModifierIdOps(val id: ModifierId) extends AnyVal {
    /** @return a `Coll[Byte]` representation of the `ModifierId` (decodes using Base16). */
    def toColl: Coll[Byte] = SigmaDsl.Colls.fromArray(idToBytes(id))

    /** Converts this modifier id to to token id. */
    def toTokenId: TokenId = Digest32Coll @@ toColl
  }
}
