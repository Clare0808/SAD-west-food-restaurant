package com.westfood.backend;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class MongoStore {
    private final MongoTemplate mongo;

    public MongoStore(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public List<Map<String, Object>> all(String collection) {
        return mongo.findAll(Document.class, collection).stream().map(this::convert).toList();
    }

    public Map<String, Object> find(String collection, String field, Object value) {
        Document result = mongo.findOne(new Query(Criteria.where(field).is(value)), Document.class, collection);
        return result == null ? null : convert(result);
    }

    public Map<String, Object> findId(String collection, String id) {
        return find(collection, "_id", new ObjectId(id));
    }

    public Map<String, Object> save(String collection, Map<String, Object> value) {
        Document document = new Document(value);
        document.remove("_id");
        if ("buys".equals(collection) && document.get("list") instanceof List<?> items) {
            List<Document> entries = new ArrayList<>();
            for (Object item : items) {
                if (item instanceof Map<?, ?> map) {
                    Document entry = new Document("_id", new ObjectId());
                    for (String field : List.of("name", "amount", "price", "total"))
                        if (map.containsKey(field)) entry.put(field, map.get(field));
                    entries.add(entry);
                }
            }
            document.put("list", entries);
        }
        mongo.insert(document, collection);
        return convert(document);
    }

    public boolean existsEmail(String email) {
        return mongo.exists(new Query(Criteria.where("email").is(email)), Document.class, "logins");
    }

    public void delete(String collection, List<?> ids) {
        for (Object id : ids) {
            mongo.remove(new Query(Criteria.where("_id").is(new ObjectId(Objects.toString(id)))), collection);
        }
    }

    public Map<String, Object> update(String collection, String id, String field, Object value) {
        org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update();
        update.set(field, value);
        mongo.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(id))), update, collection);
        return findId(collection, id);
    }

    private Map<String, Object> convert(Document document) {
        Map<String, Object> result = new LinkedHashMap<>();
        document.forEach((key, value) -> result.put(key, normalize(value)));
        return result;
    }

    private Object normalize(Object value) {
        if (value instanceof ObjectId id) return id.toHexString();
        if (value instanceof Document doc) return convert(doc);
        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>();
            list.forEach(item -> result.add(normalize(item)));
            return result;
        }
        return value;
    }
}