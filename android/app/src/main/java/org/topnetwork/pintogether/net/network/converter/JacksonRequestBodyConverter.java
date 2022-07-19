package org.topnetwork.pintogether.net.network.converter;

import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Converter;

/**
 * JacksonRequestBodyConverter
 * @param <T>
 */
final class JacksonRequestBodyConverter<T> implements Converter<T, RequestBody> {
  private static final MediaType MEDIA_TYPE = MediaType.get("application/json; charset=UTF-8");

  private final ObjectWriter adapter;

  JacksonRequestBodyConverter(ObjectWriter adapter) {
    this.adapter = adapter;
  }

  @Override public RequestBody convert(T value) throws IOException {
    byte[] bytes = adapter.writeValueAsBytes(value);
    return RequestBody.create(MEDIA_TYPE, bytes);
  }
}