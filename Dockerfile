# Use an official GraalVM image as the base image
FROM oracle/graalvm-ce:20.3.0-java17 as graalvm

# Set the working directory
WORKDIR /app

# Copy the compiled jar file to the image
COPY target/*.jar app.jar

# Set the default command to run the jar file
CMD ["java", "-jar", "app.jar"]