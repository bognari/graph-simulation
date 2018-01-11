package Adapters;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MyRelationship extends MyEntry implements Relationship {
    private List<RelationshipType> types = new ArrayList<>();
    private MyNode start;
    private MyNode end;

    private int minLength = 1;
    private int maxLength = 1;

    private Direction direction;

    public MyRelationship() {
    }

    public MyRelationship(MyRelationship other) {
        types = other.types;
        minLength = other.minLength;
        maxLength = other.maxLength;
        direction = other.direction;
        properties = other.properties;
    }

    public int getMinLength() {
        return minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void addType(final RelationshipType type) {
        types.add(type);
    }

    public void setStart(final Node start) {
        this.start = (MyNode) start;
    }

    public void setEnd(final Node end) {
        this.end = (MyNode) end;
    }

    public void setMinLength(final int minLength) {
        this.minLength = minLength;
    }

    public void setMaxLength(final int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MyNode getStartNode() {
        return start;
    }

    @Override
    public MyNode getEndNode() {
        return end;
    }

    @Override
    public MyNode getOtherNode(final Node node) {
        if (node == start) {
            return end;
        }
        if (node == end) {
            return start;
        }
        return null;
    }

    @Override
    public Node[] getNodes() {
        return new Node[]{start, end};
    }

    @Override
    public RelationshipType getType() {
        throw new UnsupportedOperationException();
    }

    public Iterable<RelationshipType> getTypes() {
        return types;
    }

    @Override
    public boolean isType(final RelationshipType relationshipType) {
        return types.isEmpty() && relationshipType == null || types.contains(relationshipType);
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(final Direction direction) {
        this.direction = direction;
    }

    @Override
    public String toString() {
        switch (direction) {
            case INCOMING:
                return String.format("%s <-%s- %s", start, typesString(), end);
            case OUTGOING:
                return String.format("%s -%s-> %s", start, typesString(), end);
            case BOTH:
                return String.format("%s -%s- %s", start, typesString(), end);
            default:
                return super.toString();
        }
    }

    private String typesString() {
        if (this.types.isEmpty()) {
            return "";
        }

        List<String> types = new LinkedList<>();
        for (RelationshipType relationshipType : this.types) {
            types.add(relationshipType.toString());
        }

        return "[" + String.join(", ", types) + "]";
    }
}
