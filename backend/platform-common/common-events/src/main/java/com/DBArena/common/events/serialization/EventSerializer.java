package com.DBArena.common.events.serialization;

/** Encodes/decodes one event payload type. One instance is bound to one {@code T}. */
public interface EventSerializer<T> {

    byte[] serialize(T value);

    T deserialize(byte[] bytes);
}
