package com.AtomCAS.utilities;

import java.util.ArrayList;
import java.util.List;

public class LogNode {
    private String title;
    private String input;
    private String output;
    private String note;
    private Throwable error;
    private final List<LogNode> children = new ArrayList<>();

    public LogNode(String title) {
        this.title = title;
    }
    @Override
    public String toString() {
        return displayText();
    }
    public void setInput(Object input) {
        this.input = safe(input);
    }

    public void setOutput(Object output) {
        this.output = safe(output);
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public void addChild(LogNode child) {
        children.add(child);
    }

    public List<LogNode> children() {
        return children;
    }

    public String displayText() {
        StringBuilder sb = new StringBuilder(title);

        if (input != null)
            sb.append("(").append(input).append(")");

        if (output != null)
            sb.append(" -> ").append(output);

        if (error != null)
            sb.append(" ERROR: ").append(error.getMessage());

        return sb.toString();
    }

    public String detailText() {
        StringBuilder sb = new StringBuilder();

        sb.append("Step: ").append(title).append("\n");

        if (input != null)
            sb.append("Input: ").append(input).append("\n");

        if (output != null)
            sb.append("Output: ").append(output).append("\n");

        if (note != null)
            sb.append("Note: ").append(note).append("\n");

        if (error != null)
            sb.append("Error: ").append(error).append("\n");

        return sb.toString();
    }

    private static String safe(Object obj) {
        try {
            return String.valueOf(obj);
        } catch (Throwable t) {
            return "<toString failed>";
        }
    }
}
