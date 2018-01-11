package Adapters;

import org.neo4j.graphdb.*;

import java.util.*;
import java.util.stream.Collectors;

public class MyNode extends MyEntry implements Node {

    private List<Label> labels = new ArrayList<>();
    private Set<Relationship> relationships = new HashSet<>();
    private Set<Relationship> relationshipsIncoming = new HashSet<>();
    private Set<Relationship> relationshipsOutgoing = new HashSet<>();
    //private List<Relationship> relationshipsBoth = new ArrayList<>();

    @Override
    public void delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    public boolean hasRelationship() {
        return !relationships.isEmpty();
    }

    private static boolean hasType(Relationship relationship, RelationshipType... relationshipTypes) {
        for (RelationshipType relationshipType : relationshipTypes) {
              if (relationship.isType(relationshipType)) {
                  return true;
              }
        }
        return false;
    }
    
    @Override
    public Iterable<Relationship> getRelationships(final RelationshipType... relationshipTypes) {
        return  relationships.stream().filter(r -> hasType(r, relationshipTypes)).collect(Collectors.toList());
    }

    @Override
    public Iterable<Relationship> getRelationships(final Direction direction, final RelationshipType... relationshipTypes) {
        return getByDirection(direction).stream().filter(r -> hasType(r, relationshipTypes)).collect(Collectors.toList());
    }

    @Override
    public boolean hasRelationship(final RelationshipType... relationshipTypes) {
        return relationships.stream().anyMatch(r -> hasType(r, relationshipTypes));
    }

    @Override
    public boolean hasRelationship(final Direction direction, final RelationshipType... relationshipTypes) {
        return getByDirection(direction).stream().anyMatch(r -> hasType(r, relationshipTypes));
    }

    @Override
    public Iterable<Relationship> getRelationships(final Direction direction) {
        return getByDirection(direction);
    }

    @Override
    public boolean hasRelationship(final Direction direction) {
        return !getByDirection(direction).isEmpty();
    }

    @Override
    public Iterable<Relationship> getRelationships(final RelationshipType relationshipType, final Direction direction) {
        return getByDirection(direction).stream().filter(r -> r.isType(relationshipType)).collect(Collectors.toList());
    }

    @Override
    public boolean hasRelationship(final RelationshipType relationshipType, final Direction direction) {
        return getByDirection(direction).stream().anyMatch(r -> r.isType(relationshipType));
    }

    @Override
    public Relationship getSingleRelationship(final RelationshipType relationshipType, final Direction direction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Relationship createRelationshipTo(final Node node, final RelationshipType relationshipType) {
        throw new UnsupportedOperationException();
    }

    public void insertIncomingRelationShip(Relationship relationship) {
        if (relationships.contains(relationship) || relationshipsIncoming.contains(relationship)) {
            throw new IllegalArgumentException();
        }
        relationships.add(relationship);
        relationshipsIncoming.add(relationship);
    }

    public void insertOutgoingRelationShip(Relationship relationship) {
        if (relationships.contains(relationship) || relationshipsOutgoing.contains(relationship)) {
            throw new IllegalArgumentException();
        }
        relationships.add(relationship);
        relationshipsOutgoing.add(relationship);
    }

    @Override
    public Iterable<RelationshipType> getRelationshipTypes() {
        return relationships.stream().map(Relationship::getType).collect(Collectors.toList());
    }

    @Override
    public int getDegree() {
        return relationships.size();
    }

    @Override
    public int getDegree(final RelationshipType relationshipType) {
        return (int) relationships.stream().filter(r -> r.isType(relationshipType)).count();
    }

    @Override
    public int getDegree(final Direction direction) {
        return getByDirection(direction).size();
    }

    @Override
    public int getDegree(final RelationshipType relationshipType, final Direction direction) {
        return (int) getByDirection(direction).stream().filter(r -> r.isType(relationshipType)).count();
    }

    @Override
    public void addLabel(final Label label) {
        labels.add(label);
    }

    @Override
    public void removeLabel(final Label label) {
       labels.remove(label);
    }

    @Override
    public boolean hasLabel(final Label label) {
        return labels.contains(label);
    }

    @Override
    public List<Label> getLabels() {
        return labels;
    }

    private Collection<Relationship> getByDirection(Direction direction) {
        switch (direction) {
            case INCOMING:
                return relationshipsIncoming;
            case OUTGOING:
                return relationshipsOutgoing;
            case BOTH:
                throw new UnsupportedOperationException();
                //return relationshipsBoth;
        }
        throw new IllegalArgumentException();
    }

    @Override
    public String toString() {
        if (labels.isEmpty()) {
            return "no labels";
        }
        List<String> list = new LinkedList<>();
        for (Label label : labels) {
            list.add(label.name());
        }
        return String.join(", ", list);
    }
}
