# API examples

Use `Content-Type: application/json`. These examples assume the backend runs on port 8080.

## Create a decision
```http
POST /api/decisions
Content-Type: application/json

{"title":"Choose a laptop","description":"Compare three laptops for college"}
```

## Add a criterion
```http
POST /api/decisions/1/criteria
Content-Type: application/json

{"name":"Battery life","weight":4}
```

## Add an alternative
```http
POST /api/decisions/1/alternatives
Content-Type: application/json

{"name":"Laptop A","description":"Lightweight and portable"}
```

## Save a score
```http
PUT /api/decisions/1/scores
Content-Type: application/json

{"alternativeId":1,"criterionId":1,"score":8}
```

## Get ranking
```http
GET /api/decisions/1/results
```

Example response:
```json
[
  {"alternativeId":2,"alternativeName":"Laptop B","weightedScore":8.45,"rank":1},
  {"alternativeId":1,"alternativeName":"Laptop A","weightedScore":7.8,"rank":2}
]
```
