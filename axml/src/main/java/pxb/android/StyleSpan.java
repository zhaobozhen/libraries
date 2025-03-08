package pxb.android;

public class StyleSpan {
    final public String name;
    final public int start;
    final public int end;

    public StyleSpan(String name, int start, int end) {
        this.name = name;
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        StyleSpan styleSpan = (StyleSpan) o;

        if (end != styleSpan.end)
            return false;
        if (start != styleSpan.start)
            return false;
        if (!name.equals(styleSpan.name))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + start;
        result = 31 * result + end;
        return result;
    }
}
