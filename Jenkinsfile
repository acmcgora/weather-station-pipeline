pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build REST API') {
            steps {
                sh 'echo Building REST API...'  // replace with your actual build commands
            }
        }

        stage('Start MySQL & API') {
            steps {
                sh 'docker-compose up -d --build'  // if you have a docker-compose.yml
            }
        }

        stage('Test Integration') {
            steps {
                sh 'echo Running integration tests...'  // replace with test commands
            }
        }
    }

    post {
        success { echo 'Pipeline SUCCESS' }
        failure { echo 'Pipeline FAILURE' }
    }
}
