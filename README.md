# online-classroom

## Use Cases

You have study sessions using collaboration tools such as Discord, Teams, Zoom, Google Meet, etc.

During the session, you (host) want to give some quizzes to participants and check answers in real-time.

This web app provides a platform for having interactive quizzes.

## Quick Start

Run this command:
```sh
./gradlew run
```

You will see the console log "Server is ready!"

Access the login page via `http://localhost:7070`.

After logging in, you will be able to navigate to the classroom.

The host can be decided by submitting `/host <username>`.

The `/host` command only works for the host, or there is no host yet.

The host gives a quiz and lets participants submit their answers.

The quiz will end and reveal the correct answer when the host submits their answer.

The host can click the reset button to have another quiz.
