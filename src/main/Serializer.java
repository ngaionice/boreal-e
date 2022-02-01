import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import org.openqa.selenium.devtools.v85.io.IO;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class Serializer {

    private final Gson serializer;
    private JsonWriter writer;

    public Serializer(JsonWriter writer) {
        this.writer = writer;
        this.serializer = getSerializer();
    }

    public void setWriter(JsonWriter writer) {
        this.writer = writer;
    }

    public void serializeCourse(List<EvalPage.Header> header, List<Map<String, String>> content) throws IOException {
        writer.beginObject();

        writer.name("headers").beginArray();
        for (EvalPage.Header value : header) {
            writer.jsonValue(serializer.toJson(value));
        }
        writer.endArray();

        writer.name("content").beginArray();
        for (Map<String, String> value : content) {
            writer.jsonValue(serializer.toJson(value));
        }
        writer.endArray();

        writer.endObject();
        writer.close();
    }

    public void serializeTimetableData(String dataInJson) throws IOException {
        // need to parse to JsonElement first because API returns prettified data. minifying it first results in dramatic space savings
        JsonElement data = JsonParser.parseString(dataInJson);
        writer.jsonValue(serializer.toJson(data));
        writer.close();
    }

    public static Gson getSerializer() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(EvalPage.Header.class, new HeaderSerializer());
        return builder.create();
    }

    public static class HeaderSerializer implements JsonSerializer<EvalPage.Header> {

        @Override
        public JsonElement serialize(EvalPage.Header header, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject obj = new JsonObject();
            obj.add("title", new JsonPrimitive(header.title));
            obj.add("description", header.description == null ? null : new JsonPrimitive(header.description));
            return obj;
        }
    }
}
