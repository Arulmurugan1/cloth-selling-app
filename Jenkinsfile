pipeline {
  agent any

  tools {
    jdk 'jdk21'
    maven 'maven-3.9'
    nodejs 'node24'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Backend Build') {
      steps {
        bat 'mvn -B -U clean verify'
      }
    }

    stage('Frontend Build') {
      steps {
        dir('frontend') {
          bat 'npm ci'
          bat 'npm run build'
        }
      }
    }

    stage('Code Quality (Optional SonarQube)') {
      when {
        expression { return env.SONAR_HOST_URL?.trim() }
      }
      steps {
        bat 'mvn -B sonar:sonar'
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: '**/target/*.jar, frontend/dist/**', fingerprint: true
      junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
    }
  }
}
