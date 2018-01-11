package Adapters;

import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class MyEntry implements Entity {
    protected List<Map<String, Object>> properties = new  ArrayList<>();

    @Override
    public long getId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GraphDatabaseService getGraphDatabase() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasProperty(final String s) {
        for (Map<String, Object> map : properties) {
            if (map.containsKey(s)) {
                return true;
            }
        }
        return false;
    }

    public void setProperty(Map<String, Object> map) {
        properties.add(map);
    }

    @Override
    public Object getProperty(final String s) {
        for (Map<String, Object> map : properties) {
            if (map.containsKey(s)) {
                return map.get(s);
            }
        }
        return null;
    }

    @Override
    public Object getProperty(final String s, final Object o) {
        for (Map<String, Object> map : properties) {
            if (map.containsKey(s)) {
                return map.get(s);
            }
        }
        return o;
    }

    @Override
    public void setProperty(final String s, final Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object removeProperty(final String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<String> getPropertyKeys() {
        List<String> keys = new ArrayList<>();
        for (Map<String, Object> map : properties) {
            keys.addAll(map.keySet());
        }

        return keys;
    }

    @Override
    public Map<String, Object> getProperties(final String... strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getAllProperties() {
        throw new UnsupportedOperationException();
    }
}
