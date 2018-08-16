package codemonstur.graalhelper;

import codemonstur.graalhelper.model.GraalField;
import codemonstur.graalhelper.model.GraalPojo;
import com.google.gson.Gson;
import graalhelper.annotations.IncludeInGraalConfig;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.objectweb.asm.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME;

@Mojo(name="generate-graal-configuration", defaultPhase=PACKAGE, threadSafe=true, requiresDependencyResolution=RUNTIME)
public final class GenerateGraalConfiguration extends AbstractMojo {

    @Parameter(defaultValue="${project}", readonly=true, required=true)
    private MavenProject project;

    @Parameter(defaultValue="graal/pojos.json" )
    private String output;

    @Override
    public void execute() throws MojoFailureException {
        final Path classes = Paths.get(project.getBuild().getOutputDirectory());
        try {
            final List<GraalPojo> config = Files
                .walk(classes)
                .parallel()
                .filter(GenerateGraalConfiguration::isClassFile)
                .map(GenerateGraalConfiguration::toGraalPojo)
                .filter(Objects::nonNull)
                .filter(pojo -> pojo.hasAnnotation)
                .collect(toList());

            writeGraalConfigToFile(config, toGraalConfigFile(project, output));
        } catch (IOException e) {
            throw new MojoFailureException("Failed to read classes directory " + classes);
        }
    }

    private static boolean isClassFile(final Path file) {
        return file.toString().endsWith(".class");
    }

    private static final String GRAAL_POJO_CLASSNAME = "L"+ IncludeInGraalConfig.class.getName().replaceAll("\\.", "/")+";";

    private static GraalPojo toGraalPojo(final Path classFile) {
        try (final InputStream in = new FileInputStream(classFile.toFile())) {
            final ClassReader reader = new ClassReader(in);

            final GraalPojo pojo = new GraalPojo(reader.getClassName().replaceAll("/", "."));
            reader.accept(new ClassVisitor(Opcodes.ASM5) {
                public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
                    if (GRAAL_POJO_CLASSNAME.equals(desc)) pojo.hasAnnotation = true;
                    return super.visitAnnotation(desc,visible);
                }
                public FieldVisitor visitField(final int access, final String name, final String descriptor, final String signature
                        , final Object value) {
                    pojo.fields.add(new GraalField(name));
                    return super.visitField(access, name, descriptor, signature, value);
                }
            }, 0);
            return pojo;
        } catch (IOException e) {
            return null;
        }
    }

    private static File toGraalConfigFile(final MavenProject project, final String output) {
        final File resultFile = new File(project.getBuild().getDirectory(), output);
        resultFile.getParentFile().mkdirs();
        return resultFile;
    }

    private static void writeGraalConfigToFile(final List<GraalPojo> config, final File file) throws MojoFailureException {
        final Gson gson = new Gson();

        try (final PrintWriter out = new PrintWriter(file)) {
            out.print(gson.toJson(config));
        } catch (FileNotFoundException e) {
            throw new MojoFailureException("Failed to write configuration to "+ file, e);
        }
    }

}
