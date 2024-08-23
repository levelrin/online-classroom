window.onload = function() {
    if (hasCookie("username")) {
        const username = cookie("username");
        const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const host = window.location.hostname;
        const port = window.location.port ? `:${window.location.port}` : '';
        const ws = new WebSocket(`${wsProtocol}//${host}${port}/connect?username=${encodeURIComponent(username)}`);
        ws.addEventListener('message', event => {
            const message = JSON.parse(event.data);
            if (message.about === "classroom-info") {
                refreshClassroom();
            }
        });
        ws.addEventListener('close', event => {
            alert("Connect closed.")
            window.location.pathname = "/index.html";
        });
        ws.addEventListener('error', event => {
            alert("Connection lost due to an error. " + event);
            window.location.pathname = "/index.html";
        });
    } else {
        alert("Please login first.");
        window.location.pathname = "/index.html";
    }
}

function submitAnswer() {
    const input = document.getElementById("answer");
    const answer = input.value;

    // Prevent empty answer.
    if (answer === "") {
        return;
    }

    let body;
    if (input.value.startsWith("/host")) {
        body = {
            about: "host",
            username: answer.split("/host ")[1]
        }
    } else {
        body = {
            about: "answer",
            answer: answer
        }
    }
    const ajax = new XMLHttpRequest();
    ajax.open(
        "POST",
        "/answer",
        true
    );
    ajax.setRequestHeader("Content-Type", "text/plain");
    ajax.onload = function() {
        if (ajax.status === 200) {
            input.value = "";
        } else {
            console.error("Request failed. Status code: " + ajax.status);
        }
    }
    ajax.onerror = function() {
        console.error("Request failed due to a network error.");
    }
    ajax.send(JSON.stringify(body));
}

/**
 * Get the cookie value.
 * @param name Key.
 * @returns {string}
 */
function cookie(name) {
    let cookieArr = document.cookie.split(";");
    for(let i = 0; i < cookieArr.length; i++) {
        let cookiePair = cookieArr[i].split("=");
        if(name === cookiePair[0].trim()) {
            return decodeURIComponent(cookiePair[1]);
        }
    }
    alert("Could not find the cookie name: " + name);
}

/**
 * Check if the cookie exists.
 * @param name Key.
 * @return {boolean}
 */
function hasCookie(name) {
    let cookieArr = document.cookie.split(";");
    for(let i = 0; i < cookieArr.length; i++) {
        let cookiePair = cookieArr[i].split("=");
        if(name === cookiePair[0].trim()) {
            return true;
        }
    }
    return false;
}

function refreshClassroom() {
    const ajax = new XMLHttpRequest();
    ajax.open(
        "GET",
        "/classroom-info",
        true
    );
    ajax.responseType = "json";
    ajax.onload = function() {
        if (ajax.status === 200) {
            const responseBody = ajax.response;

            // Display the reset button for the host.
            const myUsername = cookie("username");
            const resetButton = document.getElementById("reset-button");
            resetButton.hidden = myUsername !== responseBody.host;

            // Refresh host's answer
            const isAnswerTime = responseBody["show-host-answer"];
            const divHostAnswer = document.getElementById("host-answer");
            const divAnswerInput = document.getElementById("answer-input-div");
            const correctAnswer = responseBody["host-answer"];
            if (isAnswerTime) {
                const prefix = document.createElement("strong");
                const answer = document.createElement("span");
                prefix.textContent = "Correct Answer: ";
                answer.textContent = correctAnswer;
                divHostAnswer.append(prefix);
                divHostAnswer.append(answer);
                divAnswerInput.hidden = true;
                divHostAnswer.hidden = false;
            } else {
                divHostAnswer.innerHTML = "";
                divAnswerInput.hidden = false;
                divHostAnswer.hidden = true;
            }

            // Refresh user list
            const userList = document.getElementById("user-list");
            userList.innerHTML = "";
            for (const user of responseBody.users) {
                const amIHost = responseBody.host === user.username;
                const li = document.createElement("li");
                const name = document.createElement("span");
                name.textContent = user.username;

                // When the user is a host
                if (amIHost) {
                    const badge = document.createElement("span");
                    badge.className = "badge bg-secondary";
                    badge.textContent = "Host";
                    li.className = "list-group-item d-flex justify-content-between align-items-center";
                    li.append(name);
                    li.append(badge);
                    userList.append(li);
                    continue;
                }

                // When the user is a student
                li.append(name);
                if (isAnswerTime) {
                    // Reveal the answer
                    const answer = user.answer;
                    const spanAnswer = document.createElement("span");
                    spanAnswer.textContent = ": " + answer;
                    li.append(spanAnswer);

                    // Correct answer
                    if (answer === correctAnswer) {
                        li.className = "list-group-item list-group-item-success";
                    } else {
                        // Wrong answer
                        li.className = "list-group-item list-group-item-danger";
                    }
                } else {
                    // Students still trying
                    if (user.answered) {
                        li.className = "list-group-item list-group-item-primary";
                    } else {
                        li.className = "list-group-item";
                    }
                }
                userList.append(li);
            }
        } else {
            console.error("Request failed. Status code: " + ajax.status);
        }
    }
    ajax.onerror = function() {
        console.error("Request failed due to a network error.");
    }
    ajax.send();
}

function resetQuiz() {
    const ajax = new XMLHttpRequest();
    ajax.open(
        "POST",
        "/reset",
        true
    );
    ajax.onload = function() {
        if (ajax.status !== 200) {
            console.error("Request failed. Status code: " + ajax.status);
        }
    }
    ajax.onerror = function() {
        console.error("Request failed due to a network error.");
    }
    ajax.send();
}
