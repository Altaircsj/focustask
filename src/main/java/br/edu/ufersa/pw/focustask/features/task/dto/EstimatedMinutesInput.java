package br.edu.ufersa.pw.focustask.features.task.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

/** An absent property leaves the record component null. A present JSON null carries a clear instruction. */
@JsonDeserialize(using = EstimatedMinutesInput.Reader.class)
public record EstimatedMinutesInput(@JsonValue Integer value) {
    public static final class Reader extends ValueDeserializer<EstimatedMinutesInput> {
        @Override
        public EstimatedMinutesInput deserialize(JsonParser parser, DeserializationContext context) {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT) || parser.getIntValue() <= 0) {
                return context.reportInputMismatch(EstimatedMinutesInput.class,
                        "estimatedMinutes must be a positive integer or null");
            }
            return new EstimatedMinutesInput(parser.getIntValue());
        }

        @Override public EstimatedMinutesInput getAbsentValue(DeserializationContext context) { return null; }
        @Override public EstimatedMinutesInput getNullValue(DeserializationContext context) { return new EstimatedMinutesInput(null); }
    }
}
