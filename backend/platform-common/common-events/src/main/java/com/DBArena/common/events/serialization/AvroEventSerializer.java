package com.DBArena.common.events.serialization;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Plain Apache Avro binary (de)serialization for one generated
 * {@link SpecificRecordBase} type. No schema registry involved - schema
 * id framing is added by whichever milestone stands up Confluent Schema
 * Registry against a live Kafka cluster; this class only needs the
 * generated class itself, so it works the same in a unit test as it will
 * against a real broker.
 */
public final class AvroEventSerializer<T extends SpecificRecordBase> implements EventSerializer<T> {

    private final SpecificDatumWriter<T> writer;
    private final SpecificDatumReader<T> reader;
    private final Class<T> type;

    public AvroEventSerializer(Class<T> type) {
        this.type = type;
        this.writer = new SpecificDatumWriter<>(type);
        this.reader = new SpecificDatumReader<>(type);
    }

    @Override
    public byte[] serialize(T value) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(value, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to Avro-encode " + type.getSimpleName(), e);
        }
    }

    @Override
    public T deserialize(byte[] bytes) {
        try {
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(bytes, null);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to Avro-decode " + type.getSimpleName(), e);
        }
    }
}
