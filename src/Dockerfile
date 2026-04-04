# Base Java image
FROM openjdk:17-jdk

# Working directory inside container
WORKDIR /app

# Copy all Java source files into container
COPY . .

# Compile all Java files
RUN javac *.java

# Run the main class
CMD ["java", "Main"]
