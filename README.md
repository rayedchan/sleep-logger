# Backend Engineering Interview: Take-home Assignment

Hello!

This is the repository that contains everything you need to complete the take home assignment, part of the backend interview process for Noom.

## The Assignment

You need to develop an API for sleep logger that will later be integrated into the Noom web interface. The functional requirements API needs to support are:

 1. Create the sleep log for the last night
    1. Sleep data contains
        1. The date of the sleep (today)
        1. The time in bed interval
        1. Total time in bed
        1. How the user felt in the morning: one of [BAD, OK, GOOD]

 1. Fetch information about the last night's sleep
 1. Get the last 30-day averages
    1. Data that needs to be included in the response
        1. The range for which averages are shown
        1. Average total time in bed
        1. The average time the user gets to bed and gets out of bed
        1. Frequencies of how the user felt in the morning
    1. The user can switch back to the single sleep log view (goes to requirement #1)

The assignment is to:

 1. Create tables required to support the functionality above (PostgreSQL)
    1. The Spring project includes Flyway, which you should use to manage your DB migrations.
 1. Build required functionality in the REST API service (Kotlin/Java + Spring)
    1. Ignore authentication and authorization, but make the REST API aware of the concept of a user.
 1. Write unit tests for the repository and any business logic.
 1. Write a simple script or create Postman collection that can be used to test the API

## Instructions

 1. Create a git repository from the files provided here.
 1. All code changes should be merged in as PRs to the repository. Separate your commits into meaningful pieces, and don't commit artifacts like build files etc.
 1. Write code and PR descriptions as if you were writing production-level code.
 1. The template in this repository provides a basic environment. Everything needed to start and test your code is available and functional. We expect you to use PostgreSQL and Java/Kotlin + Spring. On top of that, if you want to add new frameworks, you are free to do so.
 1. Keep in mind the goal of the interview is to assess your software development and coding skills. There's no need to spend time tweaking the configuration of the server, the DB, or the build system. The defaults in use here are good enough for this exercise.
 1. Once complete, zip the repository and upload it to the take-home link provided to you by Noom's talent team.

## How to Run

Dockerfiles are set up for your convenience for running the whole project. You will need docker and ports 5432 (Postgres) and 8080 (API).

To run everything, simply execute `docker-compose up`. To build and run, execute `docker-compose up --build`.
