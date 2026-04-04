# Base image with Java
FROM openjdk:17

# Working directory inside the container
WORKDIR /app

# Copy all source code
COPY . .

# Compile Java files
RUN javac *.java

# Run the main class
CMD ["java", "Main"]
