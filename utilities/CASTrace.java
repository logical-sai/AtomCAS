package com.AtomCAS.utilities;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;
public final class CASTrace {
    private static boolean enabled = false;
    private static LogNode root;
    private static final Deque<LogNode> stack = new ArrayDeque<>();
    private CASTrace() {throw new UnsupportedOperationException("You haven't learned your lesson, have you? If not, why do you still code?");}
    public static void enable() {
        enabled = true;
        root = null;
        stack.clear();
    }

    public static void disable() {
        enabled = false;
        root = null;
        stack.clear();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static LogNode root() {
        return root;
    }

    public static <T> T call(String title, Object input, Supplier<T> body) {
        if (!enabled)
            return body.get();

        LogNode node = new LogNode(title);
        node.setInput(input);

        if (stack.isEmpty()) {
            root = node;
        } else {
            stack.peek().addChild(node);
        }

        stack.push(node);

        try {
            T result = body.get();
            node.setOutput(result);
            return result;
        } catch (RuntimeException e) {
            node.setError(e);
            throw e;
        } finally {
            stack.pop();
        }
    }

    public static void step(String title, String note) {
        if (!enabled) return;

        LogNode node = new LogNode(title);
        node.setNote(note);

        if (stack.isEmpty())
            root = node;
        else
            stack.peek().addChild(node);
    }

    public static void value(String title, Object input, Object output) {
        if (!enabled) return;

        LogNode node = new LogNode(title);
        node.setInput(input);
        node.setOutput(output);

        if (stack.isEmpty())
            root = node;
        else
            stack.peek().addChild(node);
    }
}
