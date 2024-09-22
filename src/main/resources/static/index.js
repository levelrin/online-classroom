function validateLogin() {
    const feedbackTag = document.getElementById("login-feedback");
    const username = document.getElementById("username").value;
    let valid = true;
    if (username === "") {
        feedbackTag.innerText = "Please enter your name.";
        valid = false;
    } else if (username.includes(" ")) {
        feedbackTag.innerText = "Please don't use spaces.";
        valid = false;
    } else {
        feedbackTag.innerText = "";
    }
    return valid;
}
