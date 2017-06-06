package org.terracotta.helper.common;

import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

public class HelperEntityCodec implements MessageCodec<HelperEntityMessage, HelperEntityResponse> {

  @Override
  public byte[] encodeMessage(final HelperEntityMessage helperEntityMessage) throws MessageCodecException {
    return helperEntityMessage.encode();
  }

  @Override
  public HelperEntityMessage decodeMessage(final byte[] bytes) throws MessageCodecException {
    return HelperEntityMessage.decode(bytes);
  }

  @Override
  public byte[] encodeResponse(final HelperEntityResponse helperEntityResponse) throws MessageCodecException {
    return helperEntityResponse.encode();
  }

  @Override
  public HelperEntityResponse decodeResponse(final byte[] bytes) throws MessageCodecException {
    return HelperEntityResponse.decode(bytes);
  }
}
