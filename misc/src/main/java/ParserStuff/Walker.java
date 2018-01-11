package ParserStuff;

import Adapters.MyNode;
import Adapters.MyRelationship;
import example.PatternBaseVisitor;
import example.PatternParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.neo4j.graphdb.*;

import java.util.*;

public class Walker extends PatternBaseVisitor<Object> {

    private Map<String, MyNode> nodesDirectory;
    private List<MyNode> nodes;
    private List<MyRelationship> relationships;
    private Map<String, Variable> variables;

    public Map<String, MyNode> getNodesDirectory() {
        return nodesDirectory;
    }

    public List<MyNode> getNodes() {
        return nodes;
    }

    public List<MyRelationship> getRelationships() {
        return relationships;
    }

    public Map<String, Variable> getVariables() {
        return variables;
    }

    public Walker() {
        nodesDirectory = new HashMap<>();
        nodes = new LinkedList<>();
        relationships = new LinkedList<>();
        variables = new HashMap<>();
    }

    /**
     * Visit a parse tree produced by {@link PatternParser#nodePattern}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    @Override
    public Node visitNodePattern(final PatternParser.NodePatternContext ctx) {
        String variable = null;
        List<String> labels = new LinkedList<>();
        Map<String, Object> properties = null;

        for (ParseTree child : ctx.children) {
            if (child instanceof PatternParser.VariableContext) {
                PatternParser.VariableContext variableContext = (PatternParser.VariableContext) child;
                variable = (String) visitVariable(variableContext);
            } else if (child instanceof PatternParser.NodeLabelsContext) {
                PatternParser.NodeLabelsContext nodeLabelsContext = (PatternParser.NodeLabelsContext) child;
                labels = visitNodeLabels(nodeLabelsContext);
            } else if (child instanceof PatternParser.PropertiesContext) {
                PatternParser.PropertiesContext propertiesContext = (PatternParser.PropertiesContext) child;
                properties = (Map<String, Object>) visitProperties(propertiesContext);
            }
        }

        MyNode node = null;

        if (variable != null) {
            node = nodesDirectory.get(variable);
        }

        if (node == null) {
            node = new MyNode();
            nodes.add(node);
        }

        for (String label : labels) {
            node.addLabel(Label.label(label));
        }

        if (variable != null) {
            nodesDirectory.put(variable, node);
        }

        node.setProperty(properties);

        return node;
    }


    @Override
    public Object visitSymbolicName(final PatternParser.SymbolicNameContext ctx) {
        return ctx.getText();
    }

    @Override
    public List<String> visitNodeLabels(final PatternParser.NodeLabelsContext ctx) {
        List<String> list = new LinkedList<>();

        for (ParseTree child : ctx.children) {
            if (child instanceof PatternParser.NodeLabelContext) {
                PatternParser.NodeLabelContext nodeLabelContext = (PatternParser.NodeLabelContext) child;
                list.add((String) visitNodeLabel(nodeLabelContext));
            }
        }
        return list;
    }

    @Override
    public Map<String, Object> visitMapLiteral(final PatternParser.MapLiteralContext ctx) {
        List<Object> list = new LinkedList<>();

        for (ParseTree child : ctx.children) {
            if (child instanceof PatternParser.PropertyKeyNameContext) {
                PatternParser.PropertyKeyNameContext keyNameContext = (PatternParser.PropertyKeyNameContext) child;
                list.add(visitPropertyKeyName(keyNameContext));
            } else if (child instanceof PatternParser.ExpressionContext) {
                PatternParser.ExpressionContext expressionContext = (PatternParser.ExpressionContext) child;
                list.add(visitExpression(expressionContext));
            }
        }

        Map<String, Object> ret = new HashMap<>();

        for (int i = 0; i < list.size(); i += 2) {
            ret.put((String) list.get(i), list.get(i + 1));
        }

        return ret;
    }

    @Override
    public Object visitLiteral(final PatternParser.LiteralContext ctx) {
        for (ParseTree child : ctx.children) {
            if (child instanceof TerminalNodeImpl) {
                return child.getText();
            } else if (child instanceof PatternParser.BooleanLiteralContext) {
                return new Boolean(child.getText());
            } else if (child instanceof PatternParser.MapLiteralContext) {
                PatternParser.MapLiteralContext mapLiteralContext = (PatternParser.MapLiteralContext) child;
                return visitMapLiteral(mapLiteralContext);
            }
        }
        return super.visitLiteral(ctx);
    }

    @Override
    public Object visitIntegerLiteral(final PatternParser.IntegerLiteralContext ctx) {
        String value = ctx.getText();
        return Integer.valueOf(value, 10);
        //return super.visitIntegerLiteral(ctx);
    }

    @Override
    public Object visitParameter(final PatternParser.ParameterContext ctx) {
        String label = ctx.getText();
        Variable variable = variables.get(label);

        if (variable == null) {
            variable = new Variable();
            variables.put(label, variable);
        }

        return variable;
    }

    @Override
    public Object visitPatternElement(final PatternParser.PatternElementContext ctx) {
        List<Entity> list = new LinkedList<>();

        for (ParseTree child : ctx.children) {
            if (child instanceof PatternParser.NodePatternContext) {
                PatternParser.NodePatternContext nodePatternContext = (PatternParser.NodePatternContext) child;
                list.add(visitNodePattern(nodePatternContext));
            }
            if (child instanceof PatternParser.PatternElementChainContext) {
                PatternParser.PatternElementChainContext chainContext = (PatternParser.PatternElementChainContext) child;
                list.addAll(visitPatternElementChain(chainContext));
            }
        }

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof Relationship) {
                MyRelationship relationship = (MyRelationship) list.get(i);

                switch (relationship.getDirection()) {
                    case BOTH: {
                        MyNode start = (MyNode) list.get(i - 1);
                        MyNode end = (MyNode) list.get(i + 1);
                        MyRelationship first = new MyRelationship(relationship);
                        MyRelationship second = new MyRelationship(relationship);

                        first.setStart(start);
                        first.setEnd(end);

                        start.insertOutgoingRelationShip(first);
                        end.insertIncomingRelationShip(first);


                        second.setStart(end);
                        second.setEnd(start);

                        start.insertIncomingRelationShip(second);
                        end.insertOutgoingRelationShip(second);

                        relationships.add(first);
                        relationships.add(second);
                    }
                    break;
                    case OUTGOING: {
                        MyNode start = (MyNode) list.get(i - 1);
                        MyNode end = (MyNode) list.get(i + 1);

                        relationship.setStart(start);
                        relationship.setEnd(end);

                        start.insertOutgoingRelationShip(relationship);
                        end.insertIncomingRelationShip(relationship);

                        relationships.add(relationship);
                    }
                        break;

                    case INCOMING: {
                        MyNode start = (MyNode) list.get(i - 1);
                        MyNode end = (MyNode) list.get(i + 1);

                        relationship.setStart(end);
                        relationship.setEnd(start);

                        start.insertIncomingRelationShip(relationship);
                        end.insertOutgoingRelationShip(relationship);

                        relationships.add(relationship);
                    }
                        break;
                }
            }
        }

        return null;
    }

    @Override
    public List<Entity> visitPatternElementChain(final PatternParser.PatternElementChainContext ctx) {
        List<Entity> list = new LinkedList<>();

        for (ParseTree child : ctx.children) {
            if (child instanceof PatternParser.RelationshipPatternContext) {
                PatternParser.RelationshipPatternContext relationshipPatternContext = (PatternParser.RelationshipPatternContext) child;
                list.add(visitRelationshipPattern(relationshipPatternContext));
            } else if (child instanceof PatternParser.NodePatternContext) {
                PatternParser.NodePatternContext nodePatternContext = (PatternParser.NodePatternContext) child;
                list.add(visitNodePattern(nodePatternContext));
            }
        }

        return list;
    }

    @Override
    public MyRelationship visitRelationshipPattern(final PatternParser.RelationshipPatternContext ctx) {
        boolean rightArrow = false;
        boolean leftArrow = false;

        Map<String, Object> details = null;

        for (ParseTree child : ctx.children) {
            if (child instanceof PatternParser.LeftArrowHeadContext) {
                leftArrow = true;
            } else if (child instanceof PatternParser.RightArrowHeadContext) {
                rightArrow = true;
            } else if (child instanceof PatternParser.RelationshipDetailContext) {
                PatternParser.RelationshipDetailContext detailContext = (PatternParser.RelationshipDetailContext) child;

                details = (Map<String, Object>) visitRelationshipDetail(detailContext);
            }
        }

        MyRelationship relationship = new MyRelationship();

        if (rightArrow == leftArrow) {
            relationship.setDirection(Direction.BOTH);
        } else if (rightArrow) {
            relationship.setDirection(Direction.OUTGOING);
        } else if (leftArrow) {
            relationship.setDirection(Direction.INCOMING);
        }

        if (details != null) {
            List<String> types = (List<String>) details.get("TYPES");

            if (types != null) {
                for (String type : types) {
                    relationship.addType(RelationshipType.withName(type));
                }
            }

            Map<String, Object> properties = (Map<String, Object>) details.get("PROPERTIES");

            if (properties != null) {
                relationship.setProperty(properties);
            }
        }

        return relationship;
    }

    @Override
    public Object visitRelationshipDetail(final PatternParser.RelationshipDetailContext ctx) {
        Map<String, Object> values = new HashMap<>();

        for (ParseTree child : ctx.children) {
            if (child instanceof PatternParser.VariableContext) {
                PatternParser.VariableContext variableContext = (PatternParser.VariableContext) child;

                values.put("VARIABLE", visitVariable(variableContext));
            } else if (child instanceof PatternParser.RelationshipTypesContext) {
                PatternParser.RelationshipTypesContext typesContext = (PatternParser.RelationshipTypesContext) child;

                values.put("TYPES", visitRelationshipTypes(typesContext));
            } else if (child instanceof PatternParser.RangeLiteralContext) {
                PatternParser.RangeLiteralContext rangeLiteralContext = (PatternParser.RangeLiteralContext) child;

                values.put("RANGES", visitRangeLiteral(rangeLiteralContext));
            } else if (child instanceof PatternParser.PropertiesContext) {
                PatternParser.PropertiesContext propertiesContext = (PatternParser.PropertiesContext) child;

                values.put("PROPERTIES", visitProperties(propertiesContext));
            }
        }

        return values;
    }

    @Override
    public Object visitRelationshipTypes(final PatternParser.RelationshipTypesContext ctx) {
        List<String> types = new ArrayList<>();

        for (ParseTree child : ctx.children) {
            if (child instanceof PatternParser.RelTypeNameContext) {
                PatternParser.RelTypeNameContext typeNameContext = (PatternParser.RelTypeNameContext) child;

                types.add((String) visitRelTypeName(typeNameContext));
            }
        }

        return types;
    }

    @Override
    public Object visitRangeLiteral(final PatternParser.RangeLiteralContext ctx) {
        return super.visitRangeLiteral(ctx);
    }

    class Variable {
        Object content;
    }
}
