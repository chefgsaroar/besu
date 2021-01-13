/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.consensus.qbft.messagewrappers;

import org.hyperledger.besu.consensus.common.bft.BftBlockHeaderFunctions;
import org.hyperledger.besu.consensus.common.bft.messagewrappers.BftMessage;
import org.hyperledger.besu.consensus.common.bft.payload.SignedData;
import org.hyperledger.besu.consensus.qbft.payload.PayloadDeserializers;
import org.hyperledger.besu.consensus.qbft.payload.PreparePayload;
import org.hyperledger.besu.consensus.qbft.payload.PreparedRoundMetadata;
import org.hyperledger.besu.consensus.qbft.payload.RoundChangePayload;
import org.hyperledger.besu.ethereum.core.Block;
import org.hyperledger.besu.ethereum.rlp.BytesValueRLPOutput;
import org.hyperledger.besu.ethereum.rlp.RLP;
import org.hyperledger.besu.ethereum.rlp.RLPInput;

import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;

public class RoundChange extends BftMessage<RoundChangePayload> {

  private final Optional<Block> proposedBlock;
  private final List<SignedData<PreparePayload>> prepares;

  public RoundChange(
      final SignedData<RoundChangePayload> payload,
      final Optional<Block> proposedBlock,
      final List<SignedData<PreparePayload>> prepares) {
    super(payload);
    this.proposedBlock = proposedBlock;
    this.prepares = prepares;
  }

  public Optional<Block> getProposedBlock() {
    return proposedBlock;
  }

  public List<SignedData<PreparePayload>> getPrepares() {
    return prepares;
  }

  public Optional<PreparedRoundMetadata> getPreparedRoundMetadata() {
    return getPayload().getPreparedRoundMetadata();
  }

  public Optional<Integer> getPreparedRound() {
    return getPayload()
        .getPreparedRoundMetadata()
        .map(preparedRoundMetadata -> preparedRoundMetadata.getPreparedRound());
  }

  @Override
  public Bytes encode() {
    final BytesValueRLPOutput rlpOut = new BytesValueRLPOutput();
    rlpOut.startList();
    getSignedPayload().writeTo(rlpOut);
    proposedBlock.ifPresentOrElse(pb -> pb.writeTo(rlpOut), rlpOut::writeNull);
    rlpOut.writeList(prepares, SignedData::writeTo);
    rlpOut.endList();
    return rlpOut.encoded();
  }

  public static RoundChange decode(final Bytes data) {

    final RLPInput rlpIn = RLP.input(data);
    rlpIn.enterList();
    final SignedData<RoundChangePayload> payload =
        PayloadDeserializers.readSignedRoundChangePayloadFrom(rlpIn);

    final Optional<Block> block;
    if (rlpIn.nextIsNull()) {
      rlpIn.skipNext();
      block = Optional.empty();
    } else {
      block = Optional.of(Block.readFrom(rlpIn, BftBlockHeaderFunctions.forCommittedSeal()));
    }

    final List<SignedData<PreparePayload>> prepares =
        rlpIn.readList(PayloadDeserializers::readSignedPreparePayloadFrom);
    rlpIn.leaveList();

    return new RoundChange(payload, block, prepares);
  }
}