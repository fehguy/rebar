package org.eatbacon.query;

import java.util.Arrays;
import java.util.List;

public class Index {
    private final String[] values;

    public Index(String ... values) {
        this.values = values;
    }

    public Index(List<Object> values) {
        this.values = new String[values.size()];
        for(int i = 0; i < values.size(); i++) {
            this.values[i] = values.get(i).toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Index index = (Index) o;

        return Arrays.equals(values, index.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    public String[] getValues() {
        return values;
    }
}
