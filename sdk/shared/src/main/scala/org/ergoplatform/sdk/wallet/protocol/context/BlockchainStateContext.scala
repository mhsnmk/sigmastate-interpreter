package org.ergoplatform.sdk.wallet.protocol.context

import special.collection.Coll

/** Blockchain context used in tx signing.
  *
  * @param sigmaLastHeaders    fixed number (10 in Ergo) of last block headers
  * @param previousStateDigest UTXO set digest from a last header (of sigmaLastHeaders)
  * @param sigmaPreHeader      returns pre-header (header without certain fields) of the current block
  */
case class BlockchainStateContext(
  sigmaLastHeaders: Coll[special.sigma.Header],
  previousStateDigest: Coll[Byte],
  sigmaPreHeader: special.sigma.PreHeader
)
