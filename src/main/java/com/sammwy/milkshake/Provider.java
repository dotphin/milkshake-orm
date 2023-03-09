package com.sammwy.milkshake;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.sammwy.milkshake.find.FindFilter;
import com.sammwy.milkshake.find.FindOptions;
import com.sammwy.milkshake.operations.Operation;

import org.bson.Document;
import org.bson.conversions.Bson;

public class Provider {
    private String databaseUri;
    private int connections = 0;
    private boolean active = false;

    private MongoClient client;
    private MongoDatabase database;

    protected Provider(String databaseUri) {
        ConnectionString uri = new ConnectionString(databaseUri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(uri)
                .retryWrites(true)
                .retryReads(true)
                .build();

        this.client = MongoClients.create(settings);
        this.database = this.client.getDatabase(uri.getDatabase());
        this.databaseUri = databaseUri;
        this.active = true;
    }

    protected void addConnection() {
        this.connections++;
    }

    public void close() {
        this.connections--;

        if (this.connections == 0) {
            if (this.client != null) {
                this.client.close();
            }

            this.client = null;
            this.database = null;
            this.active = false;
            Milkshake.providers.remove(this.databaseUri);
        }
    }

    public boolean isActive() {
        return this.active;
    }

    public String create(String collection, Document props) {
        MongoCollection<Document> documents = database.getCollection(collection);
        InsertOneResult result = documents.insertOne(props);
        return result.getInsertedId().asObjectId().getValue().toHexString();
    }

    public Document findOne(String collection, FindFilter filter) {
        MongoCollection<Document> documents = database.getCollection(collection);
        Document doc = documents.find(filter.build()).first();
        return doc;
    }

    public Document findByID(String collection, String id) {
        return this.findOne(collection, new FindFilter().isIDEquals(id));
    }

    public List<Document> findMany(String collection, FindFilter filter, FindOptions options) {
        MongoCollection<Document> documents = database.getCollection(collection);
        FindIterable<Document> iterator = documents.find(filter.build());

        if (options != null) {
            options.apply(iterator);
        }

        List<Document> result = new ArrayList<>();
        iterator.forEach((document) -> {
            result.add(document);
        });
        return result;
    }

    public List<Document> findMany(String collection, FindFilter filter) {
        return this.findMany(collection, filter, null);
    }

    public boolean updateOne(String collection, FindFilter filter, Bson update) {
        MongoCollection<Document> documents = database.getCollection(collection);
        UpdateResult result = documents.updateOne(filter.build(), update);
        return result.getModifiedCount() > 0;
    }

    public boolean updateOne(String collection, FindFilter filter, Operation update) {
        return this.updateOne(collection, filter, update.build());
    }

    public boolean updateByID(String collection, String id, Bson update) {
        return this.updateOne(collection, new FindFilter().isIDEquals(id), update);
    }

    public boolean updateByID(String collection, String id, Operation update) {
        return this.updateByID(collection, id, update.build());
    }

    public long updateMany(String collection, FindFilter filter, Bson update) {
        MongoCollection<Document> documents = database.getCollection(collection);
        UpdateResult result = documents.updateMany(filter.build(), update);
        return result.getModifiedCount();
    }

    public long updateMany(String collection, FindFilter filter, Operation update) {
        return this.updateMany(collection, filter, update.build());
    }

    public boolean deleteOne(String collection, FindFilter filter) {
        MongoCollection<Document> documents = database.getCollection(collection);
        DeleteResult result = documents.deleteOne(filter.build());
        return result.getDeletedCount() > 0;
    }

    public boolean deleteByID(String collection, String id) {
        return this.deleteOne(collection, new FindFilter().isIDEquals(id));
    }

    public long deleteMany(String collection, FindFilter filter) {
        MongoCollection<Document> documents = database.getCollection(collection);
        DeleteResult result = documents.deleteMany(filter.build());
        return result.getDeletedCount();
    }
}
