package codemonstur.graalhelper.model;

import java.util.ArrayList;
import java.util.List;

public final class GraalPojo {
    public transient boolean hasAnnotation;
    public final String name;
    public final boolean allDeclaredConstructors;
    public final boolean allPublicConstructors;
    public final boolean allDeclaredMethods;
    public final boolean allPublicMethods;
    public final List<GraalField> fields;

    public GraalPojo(final String name) {
        this.name = name;
        this.allDeclaredConstructors = true;
        this.allPublicConstructors = true;
        this.allDeclaredMethods = true;
        this.allPublicMethods = true;
        this.fields = new ArrayList<>();
    }
}

