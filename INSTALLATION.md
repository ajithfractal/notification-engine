# Installation Guide - Fractal Notification Engine

## Building the Dependency

### Build the JAR

```bash
mvn clean install
```

This will:
1. Compile the code
2. Run tests
3. Create a JAR file
4. Install to your local Maven repository (`~/.m2/repository/com/fractal/fractal-notify/1.0.0/`)

### Build Without Tests

```bash
mvn clean install -DskipTests
```

## Using in Other Projects

### Option 1: Local Maven Repository (Development)

After running `mvn clean install`, the dependency is available in your local Maven repository. Other projects on the same machine can use it:

```xml
<dependency>
    <groupId>com.fractal</groupId>
    <artifactId>fractal-notify</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Option 2: Install to Company Maven Repository

If you have a company Maven repository (Nexus, Artifactory, etc.):

#### Configure Distribution in pom.xml

```xml
<distributionManagement>
    <repository>
        <id>company-releases</id>
        <url>https://your-nexus-server/repository/maven-releases/</url>
    </repository>
    <snapshotRepository>
        <id>company-snapshots</id>
        <url>https://your-nexus-server/repository/maven-snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

#### Deploy

```bash
mvn clean deploy
```

### Option 3: Manual Installation to Local Repository

```bash
mvn install:install-file \
  -Dfile=target/fractal-notify-1.0.0.jar \
  -DgroupId=com.fractal \
  -DartifactId=fractal-notify \
  -Dversion=1.0.0 \
  -Dpackaging=jar
```

### Option 4: Use as System Dependency (Not Recommended)

```xml
<dependency>
    <groupId>com.fractal</groupId>
    <artifactId>fractal-notify</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/fractal-notify-1.0.0.jar</systemPath>
</dependency>
```

## Verifying Installation

### Check Local Repository

```bash
ls ~/.m2/repository/com/fractal/fractal-notify/1.0.0/
```

You should see:
- `fractal-notify-1.0.0.jar`
- `fractal-notify-1.0.0.pom`
- `fractal-notify-1.0.0-sources.jar` (if source plugin is configured)

### Test in Another Project

Create a simple test project and add the dependency:

```xml
<dependencies>
    <dependency>
        <groupId>com.fractal</groupId>
        <artifactId>fractal-notify</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

Then try to use it:

```java
@Autowired
private NotificationUtils notificationUtils;
```

If it works, the dependency is properly installed!

## Publishing to Maven Central (Optional)

If you want to publish to Maven Central:

1. **Sign up for Sonatype OSSRH**
2. **Configure GPG signing**
3. **Add distributionManagement to pom.xml**
4. **Run**: `mvn clean deploy`

See: https://central.sonatype.org/publish/publish-guide/

## Version Management

### Updating Version

Edit `pom.xml`:

```xml
<version>1.0.1</version>
```

Then rebuild and install/deploy.

### Semantic Versioning

- **MAJOR.MINOR.PATCH**
- **MAJOR**: Breaking changes
- **MINOR**: New features, backward compatible
- **PATCH**: Bug fixes, backward compatible

## Troubleshooting

### Dependency Not Found

1. **Check local repository**: `ls ~/.m2/repository/com/fractal/fractal-notify/`
2. **Rebuild**: `mvn clean install`
3. **Check version**: Ensure version matches in both projects
4. **Refresh IDE**: In IntelliJ/Eclipse, refresh Maven dependencies

### ClassNotFoundException

1. **Check packaging**: Ensure JAR includes all classes
2. **Check dependencies**: All transitive dependencies should be available
3. **Check classpath**: Verify the JAR is in the classpath

### Auto-Configuration Not Working

1. **Check spring.factories**: Should be in `META-INF/spring.factories`
2. **Check package structure**: Components should be in `com.fractal.notify`
3. **Check component scanning**: Application should scan `com.fractal.notify` package
