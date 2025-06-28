package de.safti.saftiSk.skript.elements.types.datastruct;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Statement;
import org.bukkit.event.Event;

import java.util.ArrayDeque;
import java.util.Deque;

public class NodeDirection {
    private final Deque<StringExpression> parts = new ArrayDeque<>();

//    public static NodeDirection parse(String unparsed) throws IOException {
//
//        Deque<StringExpression> parts = new ArrayDeque<>();
//        StringReader reader = new StringReader(unparsed);
//        StringBuilder builder = new StringBuilder();
//
//        boolean collectingLiteral = true;
//
//        while (true) {
//            int n = reader.read();
//            char c = (char) n;
//            if(n == -1) {
//                break;
//            }
//
//            if(c == '%') {
//                if(!builder.isEmpty()) {
//                    // empty the builder
//                    StringExpression expression = parseStringExpression(builder.toString(), collectingLiteral);
//                    parts.add(expression);
//                    builder = new StringBuilder();
//                }
//
//                collectingLiteral = !collectingLiteral;
//                continue;
//            }
//
//            builder.append(c);
//        }
//
//    }

    public static StringExpression parseStringExpression(String str, boolean collectingLiteral) {
        if(collectingLiteral) {
            return new Literal(str);
        } else {
            return ComputedExpressionWrapper.parse(str);
        }
    }




    public record Literal(String str) implements StringExpression {

        @Override
        public String get(Event event) {
            return str;
        }

        @Override
        public String get() throws UnsupportedOperationException {
            return str;
        }

        @Override
        public boolean isLiteral() {
            return true;
        }
    }

    public record ComputedExpressionWrapper(Expression<?> expression) implements StringExpression {

        public static ComputedExpressionWrapper parse(String unparsedExpression) {
            Statement statement = Statement.parse(unparsedExpression, "Cannot understand Expression: " + unparsedExpression);
            if(!(statement instanceof Expression<?> parsedExpression)) {
                Skript.error("Cannot understand this Expression: " + unparsedExpression);
                return null;
            }

            return new ComputedExpressionWrapper(parsedExpression);
        }

        @Override
        public String get(Event event) {
            Object ret = expression.getSingle(event);
            if(ret == null) return  "";
            return ret.toString();
        }

        @Override
        public String get() throws UnsupportedOperationException {
            throw new UnsupportedOperationException("Cannot compute expression without event context!");
        }

        @Override
        public boolean isLiteral() {
            return false;
        }
    }

    public interface StringExpression {

        String get(Event event);

        String get() throws UnsupportedOperationException;

        boolean isLiteral();



    }


}
