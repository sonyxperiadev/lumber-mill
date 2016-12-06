package lumbermill.internal;

import lumbermill.api.Event;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Optional;


public abstract class BooleanExpression {

    protected final StringTemplate expressionPattern;

    public BooleanExpression(String pattern) {
        this.expressionPattern = StringTemplate.compile(pattern);
    }

    public static BooleanExpression fromString(String expression) {
        return new BooleanJavaScriptExpression(expression);
    }

    public abstract BooleanExpression init(String script);

    public abstract boolean eval(Event event);


    static class BooleanJavaScriptExpression extends BooleanExpression {

        private static final ScriptEngine se =
                new ScriptEngineManager().getEngineByName("JavaScript");
        static {
            try {
                se.eval("Array.prototype.contains = function(obj) {\n" +
                        "    var i = this.length;\n" +
                        "    while (i--) {\n" +
                        "        if (this[i] === obj) {\n" +
                        "            return true;\n" +
                        "        }\n" +
                        "    }\n" +
                        "    return false;\n" +
                        "}\n");

                se.eval("String.prototype.contains = function(string) {" +
                        "    return this.indexOf(string) != -1;\n" +
                        "}");


            } catch (ScriptException e) {
                throw new IllegalStateException(e);
            }
        }

        BooleanJavaScriptExpression(String expression) {
            super(expression);
        }

        @Override
        public BooleanExpression init(String script) {
            try {
                se.eval(script);
            } catch (ScriptException e) {
                throw new IllegalStateException(e);
            }
            return this;
        }


        public boolean eval(Event event) {
            Optional<String> formattedExpression = expressionPattern.format(event);
            if (formattedExpression.isPresent()) {
                return doEval(formattedExpression.get());
            }
            return false;
        }

        private boolean doEval(String expression) {
            try {
                return (Boolean)se.eval(expression);
            } catch (ScriptException e) {
                throw new IllegalStateException("BooleanExpression is invalid: " + expression, e);
            }
        }

    }

    @Override
    public String toString() {
        return "BooleanExpression{" +
                "expressionPattern=" + expressionPattern +
                '}';
    }
}
