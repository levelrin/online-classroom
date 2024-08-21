# API

It's about REST APIs.

## Get classroom information

### Endpoint

GET /classroom-info

### Response Body

Example:
```json
{
  "host":"test01",
  "show-host-answer":true,
  "show-student-answers":true,
  "host-answer":"1",
  "users":[
    {
      "username":"test01",
      "answered":true,
      "answer":"1"
    },
    {
      "username":"test02",
      "answered":true,
      "answer":"1"
    }
  ]
}
```

`.host` is an empty String when there is no host yet.

`show-host-answer` is false by default.

`show-student-answers` is used when the answer is revealed by the host.

`host-answer` is an empty String while students are still trying.

`users[*].answer` is an empty String if the host's answer is not revealed yet.

### Response

200 OK

## Submit an answer

### Endpoint

POST /answer

### Request Body

Example:
```json
{
  "about":"answer",
  "answer":"1"
}
```

### Response

200 OK

## Set host

### Endpoint

POST /answer

### Request Body

Example:
```json
{
  "about":"host",
  "username":"test01"
}
```

### Response

200 OK

## Reset

Only the host can use this.

### Endpoint

POST /reset

### Response

200 OK
