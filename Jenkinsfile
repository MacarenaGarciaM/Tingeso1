pipeline {
    agent any
    tools {
        maven 'maven'
    }

    stages {

        stage("Build JAR File") {
            steps {
                checkout scmGit(
                    branches: [[name: '*/main']],
                    extensions: [],
                    userRemoteConfigs: [[url: 'https://github.com/MacarenaGarciaM/Tingeso1']]
                )
                dir("demo") {
                    bat "mvn clean install"
                }
            }
        }

        stage('Build maven') {
            steps {
                checkout scmGit(
                    branches: [[name: '*/main']],
                    extensions: [],
                    userRemoteConfigs: [[url: 'https://github.com/MacarenaGarciaM/Tingeso1']]
                )
                dir('demo') {
                    bat 'mvn clean package'
                }
            }
        }

        stage('Unit Tests') {
            steps {
                dir('demo') {
                    bat 'mvn test'
                }
            }
        }

        stage("Build and Push Docker Image") {
            steps {
                dir("demo") {
                    script {
                        withCredentials([
                            usernamePassword(
                                credentialsId: 'docker-credentials',
                                usernameVariable: 'DOCKER_USER',
                                passwordVariable: 'DOCKER_PASS'
                            )
                        ]) {
                            bat 'echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin'
                            bat "docker build -t macagarcia/tingeso1:latest ."
                            bat "docker push macagarcia/tingeso1:latest"
                        }
                    }
                }
            }
        }

    }
}
